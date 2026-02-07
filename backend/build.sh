#!/bin/bash

VERSION="1.4.0"
OUTPUT_FILE="install_monitor.sh"
SRC_DIR="src"

echo "🔨 Building Installer $VERSION..."

# 1. Header & User Detection
cat << 'EOF' > $OUTPUT_FILE
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
EOF

# 2. Inject Code Files (Read from src/)
# We add 'echo ""' after the cat command to ensure the heredoc 
# delimiter appears on its own line safely.

echo "" >> $OUTPUT_FILE
echo "cat << 'PYTHON_EOF' > \"\$INSTALL_DIR/lab_health.py\"" >> $OUTPUT_FILE
cat "$SRC_DIR/lab_health.py" >> $OUTPUT_FILE
echo "" >> $OUTPUT_FILE  # <--- SAFETY NEWLINE
echo "PYTHON_EOF" >> $OUTPUT_FILE

echo "" >> $OUTPUT_FILE
echo "cat << 'PYTHON_EOF' > \"\$INSTALL_DIR/monitor_api.py\"" >> $OUTPUT_FILE
cat "$SRC_DIR/monitor_api.py" >> $OUTPUT_FILE
echo "" >> $OUTPUT_FILE  # <--- SAFETY NEWLINE
echo "PYTHON_EOF" >> $OUTPUT_FILE

# 3. GENERATE CONFIG & AVAHI & TOOLS
cat << 'EOF' >> $OUTPUT_FILE

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
EOF

chmod +x $OUTPUT_FILE
echo "✅ Build Complete: ./$OUTPUT_FILE"