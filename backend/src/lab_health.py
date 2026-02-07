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