# God's Eye View — Android client

This directory contains a small native Android WebView shell for a self-hosted God's Eye View instance.

## Architecture

The APK does **not** embed provider secrets. It connects to the normal God's Eye View Node/Vite server, so server-side API proxies and private keys stay on the server.

- Production: enter an HTTPS URL for your hosted God's Eye View server.
- Debug/LAN: private HTTP addresses such as `http://192.168.1.25:4173` are allowed by the debug manifest.
- External links leave the WebView and open in the user's normal browser.
- Microphone and location are granted only after Android runtime permission prompts.

## Local build

Requirements: JDK 17+, Android SDK 35, and Gradle 8.10.x.

From this directory:

```bash
gradle assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Security notes

Release builds disallow cleartext HTTP. Use HTTPS for any internet-facing server. The debug build permits cleartext only so a phone can connect to a trusted private-LAN development server.
