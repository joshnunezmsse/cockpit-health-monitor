#!/bin/bash
# Lab Monitor Installer
# Auto-generated build.

INSTALL_DIR="/opt/lab-monitor"
SERVICE_NAME="lab-monitor"
DEFAULT_PORT=8080

# Detect User
REAL_USER=${SUDO_USER:-$USER}
if [ "$REAL_USER" == "root" ]; then
    echo "⚠️  WARNING: Running as root. Config will default to 'root'."
fi

echo "Installing Lab Monitor..."
mkdir -p "$INSTALL_DIR"

cat << 'PYTHON_EOF' > "$INSTALL_DIR/lab_health.py"
import json
import glob
import subprocess
import os

class LabHealth:
    def __init__(self):
        self.config_dir = "/etc/cockpit/machines.d/"
        self.config = self._load_config()
        self.user = self.config.get("ssh_user", None)

    def _load_config(self):
        base_dir = os.path.dirname(os.path.abspath(__file__))
        config_path = os.path.join(base_dir, "config.json")
        try:
            with open(config_path, 'r') as f:
                return json.load(f)
        except Exception as e:
            print(f"Error loading config: {e}")
            return {}

    def get_inventory(self):
        ips = set()
        try:
            files = glob.glob(os.path.join(self.config_dir, "*.json"))
            for f in files:
                with open(f, 'r') as json_file:
                    data = json.load(json_file)
                    ips.update(data.keys())
        except Exception as e:
            print(f"Warning: inventory error: {e}")
        return list(ips)

    def _generate_url(self, target_ip, bastion_host):
        """Generates the Cockpit Deep Link."""
        if not bastion_host:
            return ""

        # Remove port from bastion host if present
        clean_host = bastion_host.split(':')[0]
        
        if target_ip == "127.0.0.1":
            # Direct link to the bastion's own dashboard
            return f"https://{clean_host}:9090/system"
        else:
            # Federation URL: Tunnels through the bastion to the specific host
            # return f"https://{clean_host}:9090/=@{target_ip}/system/index.html"
            return f"https://{clean_host}:9090/"

    def check_host(self, ip, bastion_host=None):
        target = f"{self.user}@{ip}" if self.user else ip
        dashboard_url = self._generate_url(ip, bastion_host)

        # CHANGED: We now ask for the Hostname AND the Status in one command
        cmd = [
            "ssh", "-q", 
            "-o", "BatchMode=yes",
            "-o", "ConnectTimeout=2",
            target, "hostname && systemctl is-system-running"
        ]

        # Default response (fallback to IP if hostname fetch fails)
        resp = {
            "ip": ip,
            "name": ip, 
            "dashboard_url": dashboard_url
        }

        try:
            result = subprocess.run(cmd, capture_output=True, text=True, timeout=5)
            # Output will be:
            #   my-server-name
            #   running
            output_lines = result.stdout.strip().split('\n')
            rc = result.returncode

            if rc == 255:
                resp.update({"status": "OFFLINE", "details": "SSH Connection Failed", "health": "red"})
                return resp

            # Parse the Multi-line Output
            real_hostname = output_lines[0].strip() if len(output_lines) > 0 else ip
            status_str = output_lines[-1].strip() if len(output_lines) > 0 else "unknown"

            # Set the real name
            resp["name"] = real_hostname

            if status_str == "running":
                resp.update({"status": "HEALTHY", "details": "All services OK", "health": "green"})
                return resp

            elif status_str == "degraded":
                fail_cmd = ["ssh", "-q", target, "systemctl --failed --no-legend --plain"]
                fail_res = subprocess.run(fail_cmd, capture_output=True, text=True, timeout=5)
                failed_unit = fail_res.stdout.split()[0] if fail_res.stdout else "Unknown"
                resp.update({"status": "DEGRADED", "details": f"Failed: {failed_unit}", "health": "red"})
                return resp

            elif status_str == "starting":
                resp.update({"status": "STARTING", "details": "Booting...", "health": "yellow"})
                return resp
            else:
                resp.update({"status": "WARNING", "details": f"State: {status_str}", "health": "yellow"})
                return resp

        except subprocess.TimeoutExpired:
            resp.update({"status": "OFFLINE", "details": "Timed Out", "health": "red"})
            return resp
        except Exception as e:
            resp.update({"status": "ERROR", "details": str(e), "health": "red"})
            return resp

    def get_all_status(self, bastion_host=None):
        results = []
        for host in self.get_inventory():
            if host: results.append(self.check_host(host, bastion_host))
        return results
PYTHON_EOF

cat << 'PYTHON_EOF' > "$INSTALL_DIR/monitor_api.py"
#!/usr/bin/env python3
import http.server
import json
import os
from lab_health import LabHealth

# Load Config for Port
base_dir = os.path.dirname(os.path.abspath(__file__))
config_path = os.path.join(base_dir, "config.json")
PORT = 8080 # Fallback
try:
    with open(config_path, 'r') as f:
        config = json.load(f)
        PORT = config.get("port", PORT)
except:
    pass

class LabMonitorHandler(http.server.BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path == '/mobile-monitor/':
            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.send_header('Access-Control-Allow-Origin', '*') 
            self.end_headers()
            
            # 1. Extract Host Header (e.g., "192.168.1.50:8080")
            host_header = self.headers.get('Host')

            # 2. Init LabHealth
            checker = LabHealth() 
            
            # 3. Pass the header so we can build deep links
            data = checker.get_all_status(bastion_host=host_header)
            
            self.wfile.write(json.dumps(data).encode('utf-8'))
        else:
            self.send_response(404)
            self.end_headers()
            self.wfile.write(b'{"error": "Endpoint not found"}')

if __name__ == "__main__":
    server = http.server.HTTPServer(('0.0.0.0', PORT), LabMonitorHandler)
    print(f"Starting Lab API on port {PORT}...")
    server.serve_forever()
PYTHON_EOF

# Config JSON
echo "Generating config.json..."
cat << JSON_EOF > "$INSTALL_DIR/config.json"
{
  "ssh_user": "$REAL_USER",
  "port": $DEFAULT_PORT
}
JSON_EOF
echo "✅ Configured for SSH User: $REAL_USER"

# Avahi (Custom Type)
if command -v avahi-daemon >/dev/null 2>&1; then
    echo "Configuring Avahi Discovery..."
    cat << AVAHI_EOF > /etc/avahi/services/cockpit-monitor.service
<?xml version="1.0" standalone='no'?>
<!DOCTYPE service-group SYSTEM "avahi-service.dtd">
<service-group>
  <name replace-wildcards="yes">%h (Lab Monitor)</name>
  <service>
    <type>_cockpitlabmonitor._tcp</type>
    <port>$DEFAULT_PORT</port>
    <txt-record>path=/mobile-monitor/</txt-record>
  </service>
</service-group>
AVAHI_EOF
    echo "✅ Created /etc/avahi/services/cockpit-monitor.service"
fi

# Set-Port Tool
cat << 'SCRIPT_EOF' > "$INSTALL_DIR/set-port.sh"
#!/bin/bash
NEW_PORT=$1
if [ -z "$NEW_PORT" ]; then echo "Usage: sudo ./set-port.sh <PORT>"; exit 1; fi
if [ "$EUID" -ne 0 ]; then echo "Run as root"; exit 1; fi

DIR="/opt/lab-monitor"
sed -i "s/\"port\": [0-9]*/\"port\": $NEW_PORT/" "$DIR/config.json"
if [ -f /etc/avahi/services/cockpit-monitor.service ]; then
    sed -i "s|<port>[0-9]*</port>|<port>$NEW_PORT</port>|" /etc/avahi/services/cockpit-monitor.service
fi

systemctl restart lab-monitor
if command -v avahi-daemon >/dev/null 2>&1; then systemctl restart avahi-daemon; fi
echo "✅ Port changed to $NEW_PORT"
SCRIPT_EOF
chmod +x "$INSTALL_DIR/set-port.sh"

# Permissions & Service
chown -R "$REAL_USER:$REAL_USER" "$INSTALL_DIR"
chmod +x "$INSTALL_DIR/monitor_api.py"

cat << SERVICE_EOF > /etc/systemd/system/$SERVICE_NAME.service
[Unit]
Description=Lab Monitor REST API
After=network.target

[Service]
Type=simple
User=$REAL_USER
WorkingDirectory=$INSTALL_DIR
ExecStart=/usr/bin/python3 $INSTALL_DIR/monitor_api.py
Restart=always
Environment=PYTHONUNBUFFERED=1

[Install]
WantedBy=multi-user.target
SERVICE_EOF

# Start
systemctl daemon-reload
systemctl enable $SERVICE_NAME
systemctl restart $SERVICE_NAME
if command -v avahi-daemon >/dev/null 2>&1; then systemctl restart avahi-daemon; fi

echo "------------------------------------------------"
echo "🎉 Installation Complete!"
echo "   Discovery:  Enabled (_cockpitlabmonitor._tcp)"
echo "   Test URL:   http://localhost:$DEFAULT_PORT/mobile-monitor/"
echo "------------------------------------------------"
