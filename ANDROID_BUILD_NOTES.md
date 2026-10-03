# Android build notes

The Android client is intentionally separated from the upstream web application so the fork can continue pulling upstream updates with minimal merge friction.

The native shell asks for the backend URL on first launch and stores it in Android SharedPreferences. Provider keys should remain in the server's `.env` / provider settings, not in the APK.

The client currently supports:
- JavaScript and DOM storage required by the web UI
- Full-screen immersive mode
- Web audio/microphone permission flow for GEV voice
- Geolocation permission flow
- Full-screen web video/custom views
- Same-host navigation inside the app
- External links handed to the normal Android browser
- A native overflow button to reload, change server, or open the server in a browser
- HTTPS-only release networking; private-LAN cleartext HTTP is enabled only in debug builds
