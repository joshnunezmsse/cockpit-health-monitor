# Lab Monitor API

A lightweight, agentless REST API that monitors the system health of your Cockpit-managed homelab. 

It retrieves the "System Status" (Green/Red/Orange) from your servers using SSH and serves the data as a clean JSON endpoint for mobile apps or custom dashboards.

## 1. Purpose
This tool bridges the gap between Linux system tools and modern dashboards. 
* **Agentless:** No software needs to be installed on the target VMs.
* **Integration:** Reads your existing `/etc/cockpit/machines.d/` inventory.
* **Status Reporting:** Checks `systemctl is-system-running` on remote hosts.
* **JSON API:** Exposes a simple HTTP endpoint (default port `8080`) for consumption by other tools.

## 2. Development Setup
To run and test the monitor on your local machine without installing it as a service:

### Prerequisites
* Python 3
* SSH access to your target nodes (key-based auth configured)
* Standard Cockpit configuration files in `/etc/cockpit/machines.d/` (optional, falls back to `localhost`)

### Running Locally
1.  **Navigate to the source directory:**
    ```bash
    cd src
    ```

2.  **Create a Development Config (Optional):**
    The application looks for a `config.json` in the same directory.
    ```bash
    echo '{ "ssh_user": "labuser", "port": 8080 }' > config.json
    ```

3.  **Run the Server:**
    ```bash
    python3 monitor_api.py
    ```

4.  **Test the Endpoint:**
    Open a new terminal and curl the local instance:
    ```bash
    curl http://localhost:8080/mobile-monitor/
    ```

## 3. Building the Package
This project uses a build script to compile the Python source and configuration logic into a single, self-contained `install_monitor.sh` script.

**Run the builder:**
```bash
./build.sh
```

**Output:**

* Generates a file named `install_monitor.sh` in the root directory.
* This script contains the latest version of the Python code and the installation logic.

## 4. Installation & Configuration

### Installing

Transfer the `install_monitor.sh` file to the Cockpit Node (or whichever server will host the API) and run it.

**Note:** Run with `sudo`, but from your normal user account so the installer detects the correct user for SSH permissions.

```bash
chmod +x install_monitor.sh
sudo ./install_monitor.sh
```

**What the installer does:**

1. Creates `/opt/lab-monitor/`.
2. Installs the Python source files.
3. Generates a `config.json` defaulting to the current user.
4. Creates and enables a Systemd service (`lab-monitor`).

### Configuration

After installation, you can customize the runtime behavior by editing the config file:

**File:** `/opt/lab-monitor/config.json`

```json
{
  "ssh_user": "labuser",
  "port": 8080
}
```

* **`ssh_user`**: The Linux user that the service uses to SSH into other VMs. This user must have valid SSH keys in `/home/<user>/.ssh/` and access to the target machines.
* **`port`**: The HTTP port the API listens on.

**Applying Changes:**
If you edit the config file, simply restart the service:

```bash
sudo systemctl restart lab-monitor
```