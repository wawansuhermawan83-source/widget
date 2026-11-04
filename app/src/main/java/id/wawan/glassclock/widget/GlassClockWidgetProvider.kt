package id.wawan.glassclock.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
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
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val provider = ComponentName(context, GlassClockWidgetProvider::class.java)
            manager.getAppWidgetIds(provider).forEach { id ->
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
                options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 240)
            ).coerceAtLeast(100)

            val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            val bitmap = drawClockBitmap(context, time, widthDp, heightDp)
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

        /** Draw only HH:mm. Glyph height follows the widget's actual resizable bitmap height. */
        private fun drawClockBitmap(
            context: Context,
            time: String,
            widthDp: Int,
            heightDp: Int
        ): Bitmap {
            val density = context.resources.displayMetrics.density
            val rawWidth = (widthDp * density).roundToInt().coerceAtLeast(120)
            val rawHeight = (heightDp * density).roundToInt().coerceAtLeast(120)

            // Keep the RemoteViews bitmap modest in size to avoid TransactionTooLargeException.
            val area = rawWidth.toDouble() * rawHeight.toDouble()
            val reduction = min(1.0, sqrt(220_000.0 / area)).toFloat()
            val bitmapWidth = (rawWidth * reduction).roundToInt().coerceAtLeast(1)
            val bitmapHeight = (rawHeight * reduction).roundToInt().coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(246, 255, 255, 255)
                typeface = Typeface.create("sans-serif-thin", Typeface.NORMAL)
                textAlign = Paint.Align.LEFT
                textSize = 1000f
                setShadowLayer(3f, 0f, 1.5f, 0x44000000)
            }

            val digits = charArrayOf(time[0], time[1], time[3], time[4])
            val sampleBounds = android.graphics.Rect()
            paint.getTextBounds("8888", 0, 4, sampleBounds)
            val sampleGlyphHeight = sampleBounds.height().toFloat().coerceAtLeast(1f)

            // The digit's visible ink, not the font's extra line spacing, fills most of the height.
            paint.textSize = 1000f * (bitmapHeight * 0.88f) / sampleGlyphHeight
            val bounds = android.graphics.Rect()
            paint.getTextBounds("8888", 0, 4, bounds)
            val glyphHeight = bounds.height().toFloat().coerceAtLeast(1f)
            val baseline = bitmapHeight / 2f - (bounds.top + bounds.bottom) / 2f

            val digitWidths = digits.map { paint.measureText(it.toString()) }
            val digitGap = paint.textSize * 0.018f
            val colonGap = paint.textSize * 0.025f
            val colonSlot = paint.textSize * 0.24f
            val totalWidth = digitWidths.sum() + digitGap * 2f + colonGap * 2f + colonSlot
            val horizontalScale = min(1f, bitmapWidth * 0.96f / totalWidth)
            val startX = (bitmapWidth - totalWidth) / 2f

            var x = startX
            val colonCenter: Float
            canvas.save()
            canvas.translate(bitmapWidth / 2f, 0f)
            canvas.scale(horizontalScale, 1f)
            canvas.translate(-bitmapWidth / 2f, 0f)

            canvas.drawText(digits[0].toString(), x, baseline, paint)
            x += digitWidths[0] + digitGap
            canvas.drawText(digits[1].toString(), x, baseline, paint)
            x += digitWidths[1] + colonGap
            colonCenter = x + colonSlot / 2f
            x += colonSlot + colonGap
            canvas.drawText(digits[2].toString(), x, baseline, paint)
            x += digitWidths[2] + digitGap
            canvas.drawText(digits[3].toString(), x, baseline, paint)
            canvas.restore()

            // Draw circular colon dots after the horizontal squeeze so they remain round.
            val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(246, 255, 255, 255)
                style = Paint.Style.FILL
                setShadowLayer(2f, 0f, 1f, 0x44000000)
            }
            val displayedColonX = bitmapWidth / 2f +
                (colonCenter - bitmapWidth / 2f) * horizontalScale
            val dotRadius = (glyphHeight * 0.035f).coerceIn(2f, 18f)
            val dotOffset = glyphHeight * 0.19f
            val centerY = bitmapHeight / 2f
            canvas.drawCircle(displayedColonX, centerY - dotOffset, dotRadius, dotPaint)
            canvas.drawCircle(displayedColonX, centerY + dotOffset, dotRadius, dotPaint)

            return bitmap
        }
    }
}
