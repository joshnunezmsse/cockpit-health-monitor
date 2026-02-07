# Cockpit Health Monitor

**Your Homelab's Pulse, in Your Pocket.**

**Cockpit Health Monitor** is a lightweight, agentless monitoring solution for homelabs managed by [Cockpit Project](https://cockpit-project.org/). It consists of a self-hosted Python service that piggybacks on your existing Cockpit infrastructure and a native Android app that lets you check your lab's health from anywhere.

No new agents to install. No complex Prometheus stacks. Just a clean "Green/Red" status for all your servers, instantly available on your phone.

---

## 🎯 Why this project?

Most monitoring solutions are overkill for a homelab. You don't always need 30 days of CPU graphs; you just need to know: *"Is everything running?"*

* **Agentless:** Uses your existing Cockpit SSH connections to check status. If Cockpit can see it, we can monitor it.
* **Zero-Config Discovery:** The backend announces itself via mDNS (Avahi/Bonjour). Open the Android app, and it finds your server instantly.
* **Deep Integration:** One server acting up? Tap the node in the Android app to deep-link directly into that specific machine's Cockpit terminal/dashboard.
* **VPN Friendly:** Smart hostname resolution ensures links work whether you are on Wi-Fi or tunneled in via WireGuard/PiVPN.

---

## 🏗️ Architecture

This project is a "Monorepo" containing two distinct parts:

1. **The Backend Service (`/backend`)**
* A Python-based REST API that runs on your main "Cockpit Bastion" node.
* Reads your inventory from `/etc/cockpit/machines.d/`.
* Checks the `systemctl is-system-running` status of every node via SSH.
* Exposes a lightweight JSON endpoint.


2. **The Android App (`/android`)**
* A native Kotlin application.
* Auto-discovers the backend service on your network.
* Displays a clean list of nodes and their health status (Healthy, Degraded, Offline).



---

## 🚀 Getting Started (Server Side)

You only need to install the service on **one** machine (your main Cockpit node).

### 1. Download & Install

Go to the **[Releases](https://www.google.com/search?q=https://github.com/%3Cyour-username%3E/cockpit-health-monitor/releases)** page and download the latest `install_monitor.sh` script.

```bash
# Upload the script to your server, then run:
chmod +x install_monitor.sh
sudo ./install_monitor.sh

```

**That's it.** The installer will:

* Set up the Python service in `/opt/lab-monitor/`.
* Create a systemd service (`lab-monitor`).
* Configure Avahi/mDNS so the Android app can find it.
* Default to port **8080**.

### 2. Configuration (Optional)

If you need to change the port or the SSH user after installation, edit the config file:

`nano /opt/lab-monitor/config.json`

```json
{
  "ssh_user": "your-username",
  "port": 8080
}

```

*After changing, restart the service: `sudo systemctl restart lab-monitor*`

---

## 📱 Getting Started (Android App)

1. Download the APK from the **[Releases](https://www.google.com/search?q=https://github.com/%3Cyour-username%3E/cockpit-health-monitor/releases)** page.
2. Connect to your home Wi-Fi (or VPN).
3. Open the app.
4. **Auto-Discovery:** It should automatically find your Cockpit node.
5. **Manual Fallback:** If you are on a restrictive VPN, you can manually add your server URL (e.g., `http://cockpit-node:8080`).

---

## 🛠️ Development

We welcome contributions! Whether you want to improve the Python backend logic or make the Android UI slicker.

### Backend Setup

```bash
cd backend/src
# Run the server locally for testing
python3 monitor_api.py

```

### Android Setup

* Open the `android/` directory in **Android Studio Koala** (or newer).
* Sync Gradle and run on your emulator or device.

See [CONTRIBUTING.md](https://www.google.com/search?q=CONTRIBUTING.md) for detailed guidelines on submitting Pull Requests.

---

## 📄 License

This project is licensed under the [MIT License](https://www.google.com/search?q=LICENSE) - see the https://www.google.com/search?q=LICENSE file for details.