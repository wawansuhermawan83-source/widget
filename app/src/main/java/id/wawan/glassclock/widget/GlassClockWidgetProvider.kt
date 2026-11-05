package id.wawan.glassclock.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.LinearGradient
import android.os.Bundle
import android.widget.RemoteViews
import id.wawan.glassclock.MainActivity
import id.wawan.glassclock.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Glass Clock renderer. Digits are custom vector paths rather than a system font,
 * so resizing the widget changes the actual numeral proportions consistently.
 */
class GlassClockWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { updateOne(context, appWidgetManager, it) }
        ClockTicker.schedule(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        updateOne(context, appWidgetManager, appWidgetId, newOptions)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        ClockTicker.schedule(context)
    }

    override fun onDisabled(context: Context) {
        ClockTicker.cancel(context)
        super.onDisabled(context)
    }

    companion object {
        private const val MAX_BITMAP_PIXELS = 180_000.0

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, GlassClockWidgetProvider::class.java)
            manager.getAppWidgetIds(component).forEach { id ->
                updateOne(context, manager, id, manager.getAppWidgetOptions(id))
            }
        }

        private fun updateOne(
            context: Context,
            manager: AppWidgetManager,
            widgetId: Int,
            options: Bundle = manager.getAppWidgetOptions(widgetId)
        ) {
            val widthDp = options.getInt(
                AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,
                options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 220)
            ).coerceAtLeast(120)
            val heightDp = options.getInt(
                AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,
                options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 280)
            ).coerceAtLeast(120)

            // Force 24-hour numeric display: never locale-dependent AM/PM.
            val time = SimpleDateFormat("HH:mm", Locale.US).format(Date())
            val bitmap = createClockBitmap(context, time, widthDp, heightDp)

            val views = RemoteViews(context.packageName, R.layout.widget_glass_clock)
            views.setImageViewBitmap(R.id.widget_time, bitmap)

            val launchIntent = Intent(context, MainActivity::class.java)
            val clickIntent = PendingIntent.getActivity(
                context,
                widgetId,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, clickIntent)
            manager.updateAppWidget(widgetId, views)
        }

        private fun createClockBitmap(
            context: Context,
            time: String,
            widthDp: Int,
            heightDp: Int
        ): Bitmap {
            val density = context.resources.displayMetrics.density
            val rawWidth = (widthDp * density).roundToInt().coerceAtLeast(120)
            val rawHeight = (heightDp * density).roundToInt().coerceAtLeast(120)
            val area = rawWidth.toDouble() * rawHeight.toDouble()
            val bitmapScale = min(1.0, kotlin.math.sqrt(MAX_BITMAP_PIXELS / area)).toFloat()
            val width = (rawWidth * bitmapScale).roundToInt().coerceAtLeast(1)
            val height = (rawHeight * bitmapScale).roundToInt().coerceAtLeast(1)

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.TRANSPARENT)

            val digits = charArrayOf(time[0], time[1], time[3], time[4])

            // The reference uses tall, narrow numerals. This ratio leaves the
            // clock visually vertical while still fitting four digits + colon.
            val glyphHeight = min(height * 0.80f, width / 1.20f)
                .coerceAtLeast(30f)
            val glyphWidth = glyphHeight * 0.235f
            val digitGap = glyphHeight * 0.038f
            val colonWidth = glyphHeight * 0.075f
            val colonSideGap = glyphHeight * 0.035f
            val totalWidth = glyphWidth * 4f + digitGap * 2f +
                colonWidth + colonSideGap * 2f
            val left = (width - totalWidth) / 2f
            val top = (height - glyphHeight) / 2f

            var x = left
            drawGlassDigit(canvas, digits[0], x, top, glyphWidth, glyphHeight)
            x += glyphWidth + digitGap
            drawGlassDigit(canvas, digits[1], x, top, glyphWidth, glyphHeight)
            x += glyphWidth + colonSideGap

            val colonCenterX = x + colonWidth / 2f
            drawGlassColon(canvas, colonCenterX, top, glyphHeight)
            x += colonWidth + colonSideGap

            drawGlassDigit(canvas, digits[2], x, top, glyphWidth, glyphHeight)
            x += glyphWidth + digitGap
            drawGlassDigit(canvas, digits[3], x, top, glyphWidth, glyphHeight)

            return bitmap
        }

        private fun drawGlassDigit(
            canvas: Canvas,
            digit: Char,
            left: Float,
            top: Float,
            width: Float,
            height: Float
        ) {
            val source = digitPath(digit)
            val matrix = Matrix().apply {
                setScale(width / 100f, height / 180f)
            }
            val path = Path()
            source.transform(matrix, path)
            path.offset(left, top)

            val stroke = height * 0.058f

            // Soft shadow beneath the glass edge.
            val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = stroke * 1.30f
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                color = Color.argb(58, 55, 37, 48)
                maskFilter = BlurMaskFilter(stroke * 0.30f, BlurMaskFilter.Blur.NORMAL)
            }
            canvas.save()
            canvas.translate(0f, stroke * 0.20f)
            canvas.drawPath(path, shadow)
            canvas.restore()

            // Restrained outer bevel: avoid the neon-tube halo of the previous build.
            val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = stroke * 1.16f
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                color = Color.argb(34, 220, 224, 232)
                maskFilter = BlurMaskFilter(stroke * 0.24f, BlurMaskFilter.Blur.NORMAL)
            }
            canvas.drawPath(path, glow)

            // Broader, filled-looking glass body: the reference numerals are solid beveled forms,
            // not thin neon tubing. The stronger body weight and restrained glow reproduce that mass.
            val body = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = stroke
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                shader = LinearGradient(
                    left,
                    top,
                    left + width,
                    top + height,
                    intArrayOf(
                        Color.argb(178, 214, 218, 226),
                        Color.argb(150, 183, 188, 198),
                        Color.argb(185, 224, 227, 234),
                        Color.argb(162, 197, 202, 211)
                    ),
                    null,
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawPath(path, body)

            // No inner white rim: keep a single translucent light-gray glass body.
        }

        private fun drawGlassColon(canvas: Canvas, centerX: Float, top: Float, height: Float) {
            val radius = height * 0.034f
            val centerY1 = top + height * 0.385f
            val centerY2 = top + height * 0.625f
            val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(48, 45, 48, 56)
                maskFilter = BlurMaskFilter(radius * 0.7f, BlurMaskFilter.Blur.NORMAL)
            }
            val glass = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    centerX - radius,
                    centerY1 - radius,
                    centerX + radius,
                    centerY1 + radius,
                    intArrayOf(
                        Color.argb(178, 214, 218, 226),
                        Color.argb(148, 183, 188, 198),
                        Color.argb(174, 224, 227, 234)
                    ),
                    null,
                    Shader.TileMode.CLAMP
                )
            }
            listOf(centerY1, centerY2).forEach { cy ->
                canvas.drawCircle(centerX, cy + radius * 0.20f, radius * 1.18f, shadow)
                canvas.drawCircle(centerX, cy, radius, glass)
            }
        }

        /** Custom geometric outlines, drawn independently of installed fonts. */
        private fun digitPath(digit: Char): Path {
            val p = Path()
            when (digit) {
                '0' -> {
                    p.moveTo(50f, 8f)
                    p.cubicTo(73f, 8f, 85f, 22f, 85f, 45f)
                    p.lineTo(85f, 135f)
                    p.cubicTo(85f, 159f, 73f, 172f, 50f, 172f)
                    p.cubicTo(27f, 172f, 15f, 159f, 15f, 135f)
                    p.lineTo(15f, 45f)
                    p.cubicTo(15f, 22f, 27f, 8f, 50f, 8f)
                    p.close()
                }
                '1' -> {
                    p.moveTo(29f, 43f)
                    p.lineTo(52f, 12f)
                    p.lineTo(52f, 170f)
                }
                '2' -> {
                    p.moveTo(17f, 39f)
                    p.cubicTo(18f, 18f, 34f, 8f, 53f, 8f)
                    p.cubicTo(75f, 8f, 85f, 22f, 85f, 42f)
                    p.cubicTo(85f, 61f, 73f, 77f, 59f, 96f)
                    p.lineTo(16f, 162f)
                    p.lineTo(86f, 162f)
                }
                '3' -> {
                    p.moveTo(19f, 25f)
                    p.cubicTo(32f, 9f, 51f, 6f, 68f, 13f)
                    p.cubicTo(84f, 20f, 86f, 39f, 72f, 53f)
                    p.cubicTo(64f, 61f, 56f, 65f, 48f, 67f)
                    p.cubicTo(71f, 67f, 85f, 80f, 85f, 103f)
                    p.lineTo(85f, 139f)
                    p.cubicTo(84f, 161f, 70f, 173f, 50f, 173f)
                    p.cubicTo(33f, 173f, 21f, 166f, 15f, 153f)
                }
                '4' -> {
                    p.moveTo(68f, 172f)
                    p.lineTo(68f, 10f)
                    p.lineTo(13f, 113f)
                    p.lineTo(89f, 113f)
                }
                '5' -> {
                    p.moveTo(83f, 12f)
                    p.lineTo(25f, 12f)
                    p.lineTo(20f, 75f)
                    p.cubicTo(34f, 63f, 51f, 62f, 66f, 72f)
                    p.cubicTo(80f, 81f, 85f, 94f, 85f, 113f)
                    p.lineTo(85f, 139f)
                    p.cubicTo(84f, 161f, 70f, 173f, 50f, 173f)
                    p.cubicTo(33f, 173f, 21f, 166f, 15f, 153f)
                }
                '6' -> {
                    p.moveTo(77f, 17f)
                    p.cubicTo(57f, 7f, 38f, 17f, 26f, 38f)
                    p.cubicTo(17f, 54f, 15f, 77f, 15f, 119f)
                    p.cubicTo(15f, 155f, 28f, 173f, 51f, 173f)
                    p.cubicTo(74f, 173f, 85f, 157f, 85f, 133f)
                    p.cubicTo(85f, 107f, 72f, 91f, 51f, 91f)
                    p.cubicTo(34f, 91f, 22f, 101f, 16f, 116f)
                }
                '7' -> {
                    p.moveTo(13f, 12f)
                    p.lineTo(88f, 12f)
                    p.lineTo(36f, 172f)
                }
                '8' -> {
                    p.moveTo(50f, 8f)
                    p.cubicTo(72f, 8f, 83f, 21f, 83f, 42f)
                    p.cubicTo(83f, 61f, 69f, 74f, 50f, 83f)
                    p.cubicTo(31f, 74f, 17f, 61f, 17f, 42f)
                    p.cubicTo(17f, 21f, 28f, 8f, 50f, 8f)
                    p.close()
                    p.moveTo(50f, 83f)
                    p.cubicTo(73f, 91f, 85f, 106f, 85f, 137f)
                    p.cubicTo(85f, 160f, 73f, 173f, 50f, 173f)
                    p.cubicTo(27f, 173f, 15f, 160f, 15f, 137f)
                    p.cubicTo(15f, 106f, 27f, 91f, 50f, 83f)
                    p.close()
                }
                '9' -> {
                    p.moveTo(84f, 82f)
                    p.cubicTo(84f, 61f, 83f, 8f, 50f, 8f)
                    p.cubicTo(27f, 8f, 15f, 24f, 15f, 49f)
                    p.cubicTo(15f, 75f, 28f, 90f, 50f, 90f)
                    p.cubicTo(69f, 90f, 82f, 77f, 84f, 59f)
                    p.lineTo(84f, 137f)
                    p.cubicTo(84f, 160f, 70f, 173f, 48f, 173f)
                }
            }
            return p
        }
    }
}
