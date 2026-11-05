# Motion Wallpaper (Samsung A16)

This patch adds a separate Android application module to the existing GlassClockWidget Gradle project.

## Features
- Pick an animated GIF or MP4 from Android's document picker (no broad storage permission).
- Preview and apply using Android's native Live Wallpaper preview.
- Fit or fill/crop mode.
- Video playback is muted and loops; playback pauses while the wallpaper is not visible.
- GIF playback is redrawn while visible and paused while hidden.
- Minimum Android version: 8.0 (API 26).

## Apply the patch in Termux
From the existing project directory:

```sh
cd ~/projects/widget
unzip -o ~/storage/downloads/MotionWallpaper_Termux_Patch.zip -d ~/projects/widget
git add -A
git commit -m "Add GIF and video live wallpaper app"
git push origin main
```

## Build
Open GitHub Actions and wait for **Build Motion Wallpaper APK** to finish successfully. Download artifact **Motion-Wallpaper-debug-APK**, extract the ZIP, and install `livewallpaper-debug.apk`.

## Use
1. Open Motion Wallpaper.
2. Choose GIF or MP4.
3. Choose Fill screen or Fit whole image.
4. Tap Preview and set wallpaper.
5. In Android's wallpaper preview, choose Set wallpaper, then Home screen.

The app uses Android's Storage Access Framework, so it does not need `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`, or all-files access.
