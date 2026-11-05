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
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.widget.RemoteViews
import id.wawan.glassclock.MainActivity
import id.wawan.glassclock.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** Transparent, resizable clock using the Montserrat variable font at ExtraLight weight. */
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

            // 24-hour HH:mm only. No date, weather, AM/PM, or extra labels.
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
            val rawArea = rawWidth.toDouble() * rawHeight.toDouble()
            val bitmapScale = min(1.0, sqrt(MAX_BITMAP_PIXELS / rawArea)).toFloat()
            val width = (rawWidth * bitmapScale).roundToInt().coerceAtLeast(1)
            val height = (rawHeight * bitmapScale).roundToInt().coerceAtLeast(1)

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.TRANSPARENT)

            val font = try {
                Typeface.createFromAsset(context.assets, "fonts/Montserrat[wght].ttf")
            } catch (_: Exception) {
                Typeface.create("sans-serif-light", Typeface.NORMAL)
            }

            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
                typeface = font
                textAlign = Paint.Align.LEFT
                style = Paint.Style.FILL
                color = Color.WHITE
                isDither = true
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    fontVariationSettings = "'wght' 200"
                }
            }

            // Fill most of the available height, keeping a narrow iOS-like numeral proportion.
            paint.textSize = 1000f
            var metrics = paint.fontMetrics
            val desiredGlyphHeight = height * 0.80f
            paint.textSize *= desiredGlyphHeight / (metrics.descent - metrics.ascent)
            metrics = paint.fontMetrics

            val digits = charArrayOf(time[0], time[1], time[3], time[4])
            val widths = digits.map { paint.measureText(it.toString()) }
            val digitGap = paint.textSize * 0.025f
            val colonSlot = paint.textSize * 0.15f
            val colonSideGap = paint.textSize * 0.055f
            val totalNaturalWidth = widths.sum() + digitGap * 2f + colonSlot + colonSideGap * 2f
            val scaleX = min(0.39f, width * 0.93f / totalNaturalWidth)
            val left = width / 2f - totalNaturalWidth / 2f
            val baseline = height / 2f - (metrics.ascent + metrics.descent) / 2f
            val glyphTop = baseline + metrics.ascent
            val glyphBottom = baseline + metrics.descent

            val shadowPaint = Paint(paint).apply {
                color = Color.argb(34, 44, 30, 42)
                maskFilter = BlurMaskFilter(
                    (height * 0.006f).coerceAtLeast(1f),
                    BlurMaskFilter.Blur.NORMAL
                )
            }
            val glassPaint = Paint(paint).apply {
                shader = LinearGradient(
                    0f, glyphTop, 0f, glyphBottom,
                    intArrayOf(
                        Color.argb(246, 255, 252, 253),
                        Color.argb(220, 244, 231, 239),
                        Color.argb(235, 255, 250, 252),
                        Color.argb(218, 226, 211, 222)
                    ),
                    floatArrayOf(0f, 0.42f, 0.68f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            val rimPaint = Paint(paint).apply {
                style = Paint.Style.STROKE
                strokeWidth = (height * 0.0018f).coerceAtLeast(0.7f)
                color = Color.argb(115, 255, 255, 255)
            }

            canvas.save()
            canvas.scale(scaleX, 1f, width / 2f, height / 2f)
            var x = left
            digits.forEachIndexed { index, digit ->
                val glyph = digit.toString()
                canvas.drawText(glyph, x, baseline + height * 0.002f, shadowPaint)
                canvas.drawText(glyph, x, baseline, glassPaint)
                canvas.drawText(glyph, x, baseline, rimPaint)
                x += widths[index]

                when (index) {
                    0, 2 -> x += digitGap
                    1 -> x += colonSideGap
                }

                if (index == 1) {
                    val colonX = x + colonSlot / 2f
                    val centerY = height / 2f
                    val radius = (height * 0.023f).coerceIn(2.2f, 15f)
                    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        shader = LinearGradient(
                            colonX - radius, centerY - radius * 2f,
                            colonX + radius, centerY + radius * 2f,
                            intArrayOf(
                                Color.argb(250, 255, 252, 253),
                                Color.argb(218, 239, 220, 234),
                                Color.argb(244, 255, 255, 255)
                            ),
                            null,
                            Shader.TileMode.CLAMP
                        )
                    }
                    canvas.drawCircle(colonX, centerY - height * 0.13f, radius, dotPaint)
                    canvas.drawCircle(colonX, centerY + height * 0.13f, radius, dotPaint)
                    x += colonSlot + colonSideGap
                }
            }
            canvas.restore()
            return bitmap
        }
    }
}
