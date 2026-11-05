# Notif Tone Manager

No-root Android notification settings manager for Android 8+.

The app lists installed applications and opens each application's Android notification settings.

Important: Android does not allow an ordinary third-party app to silently rewrite another application's notification-channel sound. The user must make the sound change in the system notification settings. This project therefore avoids root/accessibility hacks and uses the official settings screen.

Build with GitHub Actions:
- JDK 17
- Gradle 8.9
- Android Gradle Plugin 8.7.3
- compileSdk 35
