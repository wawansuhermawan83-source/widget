Glass Clock — Gray / Translucent refinement

This patch replaces only:
app/src/main/java/id/wawan/glassclock/widget/GlassClockWidgetProvider.kt

Changes:
- Removes the thin inner white rim line from every digit.
- Changes the glass body and colon dots to light neutral gray.
- Reduces body opacity so the wallpaper shows through slightly.
- Reduces the glow to avoid a neon effect.
- Keeps the custom digit paths, resizing renderer, and 24-hour HH:mm display.

Apply from Termux in the repository folder:
cd ~/projects/widget
unzip -o ~/storage/downloads/GlassClock_Gray_Translucent_NoCenterLine_Patch.zip -d ~/projects/widget

git add app/src/main/java/id/wawan/glassclock/widget/GlassClockWidgetProvider.kt
git commit -m "Remove inner rim and use translucent light gray clock"
git push origin main

Then wait for GitHub Actions and install the newest artifact. Remove the existing home-screen widget and add it again after installing.
