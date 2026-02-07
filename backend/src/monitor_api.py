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