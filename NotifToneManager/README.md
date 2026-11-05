# Notif Tone Manager 1.1

- Lists installed apps and opens Android per-app notification settings.
- Imports MP3 files into the shared `Notifications/` audio folder using MediaStore on Android 10+.
- On Android 8/9, uses the public Notifications folder with the legacy storage permission.
- The user selects the imported MP3 in each target application's notification category settings.

Android restriction: a regular app cannot silently set another app's notification channel sound. This app imports the MP3 and opens the official Settings screen for the user to apply it.
