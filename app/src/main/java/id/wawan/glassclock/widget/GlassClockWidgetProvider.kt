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
        appWidgetIds.forEach {
            updateOne(context, appWidgetManager, it)
        }
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
            val component = ComponentName(
                context,
                GlassClockWidgetProvider::class.java
            )

            manager.getAppWidgetIds(component).forEach {
                updateOne(
                    context,
                    manager,
                    it,
                    manager.getAppWidgetOptions(it)
                )
            }
        }

        private fun updateOne(
            context: Context,
            manager: AppWidgetManager,
            widgetId: Int,
            options: Bundle = manager.getAppWidgetOptions(widgetId)
        ) {
            val widthDp = options.getInt(
                AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,
                220
            ).coerceAtLeast(120)

            val heightDp = options.getInt(
                AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,
                280
            ).coerceAtLeast(120)

            val time = SimpleDateFormat(
                "HH:mm",
                Locale.getDefault()
            ).format(Date())

            val bitmap = createClockBitmap(
                context,
                time,
                widthDp,
                heightDp
            )

            val views = RemoteViews(
                context.packageName,
                R.layout.widget_glass_clock
            )

            views.setImageViewBitmap(
                R.id.widget_time,
                bitmap
            )

            val launchIntent = Intent(
                context,
                MainActivity::class.java
            )

            val clickIntent = PendingIntent.getActivity(
                context,
                widgetId,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
            )

            views.setOnClickPendingIntent(
                R.id.widget_root,
                clickIntent
            )

            manager.updateAppWidget(widgetId, views)
        }

        private fun createClockBitmap(
            context: Context,
            time: String,
            widthDp: Int,
            heightDp: Int
        ): Bitmap {

            val density = context.resources.displayMetrics.density

            val rawWidth = (widthDp * density).roundToInt()
                .coerceAtLeast(120)

            val rawHeight = (heightDp * density).roundToInt()
                .coerceAtLeast(120)

            // Limit bitmap memory for Android RemoteViews.
            val maxPixels = 180000.0
            val rawArea = rawWidth.toDouble() * rawHeight
            val bitmapScale = min(
                1.0,
                sqrt(maxPixels / rawArea)
            ).toFloat()

            val width = (rawWidth * bitmapScale)
                .roundToInt().coerceAtLeast(1)

            val height = (rawHeight * bitmapScale)
                .roundToInt().coerceAtLeast(1)

            val bitmap = Bitmap.createBitmap(
                width,
                height,
                Bitmap.Config.ARGB_8888
            )

            val canvas = Canvas(bitmap)

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(248, 255, 255, 255)
                typeface = Typeface.create(
                    "sans-serif-thin",
                    Typeface.NORMAL
                )
                textAlign = Paint.Align.LEFT
                textSize = 1000f
                setShadowLayer(4f, 0f, 2f, 0x55000000)
            }

            val chars = listOf(
                time[0],
                time[1],
                time[3],
                time[4]
            )

            val widthsAt1000 = chars.map {
                paint.measureText(it.toString())
            }

            val digitGap1000 = 24f
            val colonGap1000 = 70f
            val colonSlot1000 = 160f

            val totalAt1000 =
                widthsAt1000.sum() +
                digitGap1000 * 2 +
                colonGap1000 * 2 +
                colonSlot1000

            val metrics1000 = paint.fontMetrics
            val glyphHeight1000 =
                metrics1000.descent - metrics1000.ascent

            val widthFit =
                width * 0.94f * 1000f / totalAt1000

            val heightFit =
                height * 0.84f * 1000f / glyphHeight1000

            paint.textSize = min(
                widthFit,
                heightFit
            ).coerceAtLeast(8f)

            val digitWidths = chars.map {
                paint.measureText(it.toString())
            }

            val digitGap = paint.textSize * 0.024f
            val colonGap = paint.textSize * 0.07f
            val colonSlot = paint.textSize * 0.16f

            val totalWidth =
                digitWidths.sum() +
                digitGap * 2 +
                colonGap * 2 +
                colonSlot

            val startX = (width - totalWidth) / 2f

            val metrics = paint.fontMetrics
            val glyphHeight = metrics.descent - metrics.ascent

            val centerY = height / 2f
            val baseline = centerY -
                (metrics.ascent + metrics.descent) / 2f

            // Stretch the digits vertically to fill the widget.
            val verticalScale = (
                height * 0.84f / glyphHeight
            ).coerceIn(1f, 6f)

            var x = startX

            canvas.save()
            canvas.translate(0f, centerY)
            canvas.scale(1f, verticalScale)
            canvas.translate(0f, -centerY)

            fun drawDigit(index: Int) {
                canvas.drawText(
                    chars[index].toString(),
                    x,
                    baseline,
                    paint
                )
                x += digitWidths[index]
            }

            drawDigit(0)
            x += digitGap

            drawDigit(1)
            x += colonGap

            val colonCenterX = x + colonSlot / 2f
            x += colonSlot + colonGap

            drawDigit(2)
            x += digitGap

            drawDigit(3)

            canvas.restore()

            // Draw two round colon dots, not stretched ovals.
            val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(248, 255, 255, 255)
                style = Paint.Style.FILL
                setShadowLayer(3f, 0f, 1f, 0x55000000)
            }

            val dotRadius = (
                paint.textSize * 0.038f
            ).coerceIn(2f, 14f)

            val dotOffset = glyphHeight *
                verticalScale * 0.16f

            canvas.drawCircle(
                colonCenterX,
                centerY - dotOffset,
                dotRadius,
                dotPaint
            )

            canvas.drawCircle(
                colonCenterX,
                centerY + dotOffset,
                dotRadius,
                dotPaint
            )

            return bitmap
        }
    }
}
