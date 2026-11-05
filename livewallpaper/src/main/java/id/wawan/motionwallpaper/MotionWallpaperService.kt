package id.wawan.motionwallpaper

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Movie
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.SurfaceHolder
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max
import kotlin.math.min

class MotionWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = MotionEngine()

    private inner class MotionEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private var movie: Movie? = null
        private var movieStartTime = 0L
        private var player: MediaPlayer? = null
        private var playerPrepared = false
        private val destroyed = AtomicBoolean(false)

        private val gifFrame = object : Runnable {
            override fun run() {
                if (!isVisible || destroyed.get()) return
                drawGifFrame()
                handler.postDelayed(this, FRAME_DELAY_MS)
            }
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            destroyed.set(false)
            loadSelectedMedia()
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            if (movie != null && isVisible) drawGifFrame()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            if (visible) {
                if (movie != null) {
                    movieStartTime = SystemClock.uptimeMillis()
                    handler.removeCallbacks(gifFrame)
                    handler.post(gifFrame)
                } else if (playerPrepared) {
                    startPlayerSafely()
                }
            } else {
                handler.removeCallbacks(gifFrame)
                pausePlayerSafely()
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            destroyed.set(true)
            handler.removeCallbacks(gifFrame)
            releasePlayer()
            movie = null
            super.onSurfaceDestroyed(holder)
        }

        override fun onDestroy() {
            destroyed.set(true)
            handler.removeCallbacks(gifFrame)
            releasePlayer()
            movie = null
            super.onDestroy()
        }

        private fun loadSelectedMedia() {
            val prefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE)
            val uriText = prefs.getString(MainActivity.KEY_URI, null)
            val kind = prefs.getString(MainActivity.KEY_KIND, null)
            if (uriText.isNullOrBlank() || kind.isNullOrBlank()) {
                drawFallback("Pilih media melalui Motion Wallpaper")
                return
            }

            val uri = android.net.Uri.parse(uriText)
            if (kind == "gif") {
                try {
                    val decoded = contentResolver.openInputStream(uri)?.use { Movie.decodeStream(it) }
                    if (decoded == null || decoded.width() <= 0 || decoded.height() <= 0) {
                        drawFallback("GIF tidak dapat dibuka")
                        return
                    }
                    movie = decoded
                    movieStartTime = SystemClock.uptimeMillis()
                    if (isVisible) handler.post(gifFrame)
                } catch (error: Exception) {
                    Log.e(TAG, "Unable to decode GIF", error)
                    drawFallback("GIF tidak dapat dibuka")
                }
            } else {
                prepareVideo(uri)
            }
        }

        private fun prepareVideo(uri: android.net.Uri) {
            releasePlayer()
            val mp = MediaPlayer()
            player = mp
            try {
                mp.setSurface(surfaceHolder.surface)
                mp.setDataSource(this@MotionWallpaperService, uri)
                mp.setVolume(0f, 0f)
                applyVideoScaleMode(mp)
                mp.setOnPreparedListener { prepared ->
                    if (player !== prepared || destroyed.get()) {
                        prepared.release()
                        return@setOnPreparedListener
                    }
                    playerPrepared = true
                    prepared.isLooping = true
                    if (isVisible) startPlayerSafely()
                }
                mp.setOnErrorListener { failed, what, extra ->
                    Log.e(TAG, "Video playback error: $what/$extra")
                    if (player === failed) releasePlayer()
                    true
                }
                mp.prepareAsync()
            } catch (error: Exception) {
                Log.e(TAG, "Unable to prepare video", error)
                releasePlayer()
                drawFallback("Video tidak dapat diputar")
            }
        }

        private fun applyVideoScaleMode(mp: MediaPlayer) {
            val fill = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE)
                .getBoolean(MainActivity.KEY_FILL, true)
            mp.setVideoScalingMode(
                if (fill) MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                else MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT
            )
        }

        private fun startPlayerSafely() {
            try {
                val mp = player
                if (playerPrepared && mp != null && !mp.isPlaying && surfaceHolder.surface.isValid) {
                    mp.start()
                }
            } catch (error: Exception) {
                Log.w(TAG, "Could not start wallpaper video", error)
            }
        }

        private fun pausePlayerSafely() {
            try {
                val mp = player
                if (playerPrepared && mp != null && mp.isPlaying) mp.pause()
            } catch (error: Exception) {
                Log.w(TAG, "Could not pause wallpaper video", error)
            }
        }

        private fun releasePlayer() {
            playerPrepared = false
            val old = player
            player = null
            try {
                old?.setOnPreparedListener(null)
                old?.setOnErrorListener(null)
                old?.release()
            } catch (_: Exception) {
                // Player may already have been released by the platform.
            }
        }

        private fun drawGifFrame() {
            val currentMovie = movie ?: return
            if (destroyed.get() || !surfaceHolder.surface.isValid) return

            var canvas: Canvas? = null
            try {
                canvas = surfaceHolder.lockCanvas()
                if (canvas != null) {
                    canvas.drawColor(Color.BLACK)
                    val duration = currentMovie.duration().coerceAtLeast(1000)
                    val elapsed = (SystemClock.uptimeMillis() - movieStartTime).coerceAtLeast(0L)
                    currentMovie.setTime((elapsed % duration).toInt())

                    val sourceWidth = currentMovie.width().coerceAtLeast(1).toFloat()
                    val sourceHeight = currentMovie.height().coerceAtLeast(1).toFloat()
                    val sx = canvas.width / sourceWidth
                    val sy = canvas.height / sourceHeight
                    val fill = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE)
                        .getBoolean(MainActivity.KEY_FILL, true)
                    val scale = if (fill) max(sx, sy) else min(sx, sy)
                    val drawWidth = sourceWidth * scale
                    val drawHeight = sourceHeight * scale

                    canvas.save()
                    canvas.translate(
                        (canvas.width - drawWidth) / 2f,
                        (canvas.height - drawHeight) / 2f
                    )
                    canvas.scale(scale, scale)
                    currentMovie.draw(canvas, 0f, 0f)
                    canvas.restore()
                }
            } catch (error: Exception) {
                Log.e(TAG, "GIF frame rendering failed", error)
            } finally {
                if (canvas != null) {
                    try {
                        surfaceHolder.unlockCanvasAndPost(canvas)
                    } catch (error: Exception) {
                        Log.w(TAG, "Unable to post wallpaper frame", error)
                    }
                }
            }
        }

        private fun drawFallback(message: String) {
            var canvas: Canvas? = null
            try {
                if (!surfaceHolder.surface.isValid) return
                canvas = surfaceHolder.lockCanvas()
                if (canvas != null) {
                    canvas.drawColor(Color.BLACK)
                    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.WHITE
                        textSize = 36f
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                    canvas.drawText(message, canvas.width / 2f, canvas.height / 2f, paint)
                }
            } catch (error: Exception) {
                Log.e(TAG, "Fallback wallpaper render failed", error)
            } finally {
                if (canvas != null) {
                    try { surfaceHolder.unlockCanvasAndPost(canvas) } catch (_: Exception) { }
                }
            }
        }
    }

    companion object {
        private const val TAG = "MotionWallpaper"
        private const val FRAME_DELAY_MS = 33L
    }
}
