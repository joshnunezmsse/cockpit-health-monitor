# Contributing to Cockpit Health Monitor

First off, thank you for considering contributing to Cockpit Health Monitor! It's people like you that make open source tools great.

Whether you're fixing a bug in the Python service, adding a feature to the Android app, or just improving documentation, we welcome your help.

## 📂 Project Structure

This is a **monorepo** containing two distinct parts. Please ensure you are working in the correct directory for your changes.

* **`backend/`**: The Python-based service that runs on the Cockpit node.
* **`android/`**: The native Kotlin Android application.

## 🛠️ Development Setup

### Backend (Python)
1.  Navigate to the source directory:
    ```bash
    cd backend/src
    ```
2.  Ensure you have Python 3 installed.
3.  **Running Locally:** You can run the server directly for testing without installing it as a system service:
    ```bash
    python3 monitor_api.py
    ```
    *Note: The server will look for a `config.json` in the same directory. You may need to create a dummy one for local testing.*
4.  **Building the Installer:** If you change the build logic, run the build script from the `backend/` root:
    ```bash
    ./build.sh
    ```

### Android (Kotlin)
1.  Open the `android/` directory in **Android Studio** (Koala Feature Drop or newer recommended).
2.  Let Gradle sync and download dependencies.
3.  Create a virtual device (AVD) or connect a physical Android device.
4.  Build and Run (`Shift + F10`).

## 🔄 The Workflow (Fork & Pull Request)

We follow the standard GitHub "Fork & Pull" workflow.

1.  **Fork the Project**
    * Click the "Fork" button at the top right of this repository.

2.  **Clone your Fork**
    ```bash
    git clone [https://github.com/YOUR-USERNAME/cockpit-health-monitor.git](https://github.com/YOUR-USERNAME/cockpit-health-monitor.git)
    cd cockpit-health-monitor
    ```

3.  **Create a Branch**
    * Create a branch with a descriptive name for your change.
    ```bash
    git checkout -b feature/amazing-new-feature
    # or
    git checkout -b fix/ssh-timeout-bug
    ```

4.  **Make your Changes**
    * **Backend:** If modifying Python code, try to follow PEP 8 style guidelines.
    * **Android:** Follow standard Kotlin coding conventions.

5.  **Test your Changes**
    * **Critical Step:** Please manually test your changes before submitting.
    * If you modified the **Backend**, verify that `install_monitor.sh` still generates correctly and runs on a Linux VM.
    * If you modified the **Android App**, verify it launches and connects on a real device or emulator.

6.  **Commit and Push**
    ```bash
    git add .
    git commit -m "Description of what you changed"
    git push origin feature/amazing-new-feature
    ```

7.  **Submit a Pull Request**
    * Go to the original repository on GitHub.
    * Click "New Pull Request" and select your branch.
    * **Description:** Please clearly describe what you changed and *how you tested it*. Screenshots are highly encouraged for UI changes!

## 🧪 Testing Requirements

Since this project interacts with real Linux servers and SSH connections, automated tests are difficult to mock perfectly. **We rely heavily on manual verification.**

* **For Backend PRs:** Please confirm you have deployed the generated `install_monitor.sh` to a real Linux node (a VM is fine) and verified that the service starts and reports health.
* **For Android PRs:** Please confirm the app connects to a backend and displays the node list correctly.

## 🐛 Reporting Bugs

Found a bug? Go to the [Issues](https://github.com/<your-username>/cockpit-health-monitor/issues) tab and create a new issue. Please include:
* Which component is failing (Android App or Backend Service).
* Steps to reproduce.
* Expected behavior vs. actual behavior.
* Any error logs (Logcat for Android, `journalctl -u lab-monitor` for Backend).

Thank you for helping us keep our homelabs healthy! 🚀