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
import android.graphics.Typeface

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

        val width = (widthDp * density).roundToInt().coerceAtLeast(120)
        val height = (heightDp * density).roundToInt().coerceAtLeast(120)

        val bitmap = Bitmap.createBitmap(
            width,
            height,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.TRANSPARENT)

        val digits = time.toCharArray()

        /*
         * Font utama:
         * sans-serif-thin = sangat tipis seperti referensi.
         *
         * Angka dibuat tinggi dan sempit.
         * Kita tidak lagi memakai Path/glyph buatan sendiri.
         */
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create("sans-serif-thin", Typeface.NORMAL)
            color = Color.WHITE
            textSize = height * 0.80f
            textAlign = Paint.Align.LEFT
            style = Paint.Style.FILL
            isSubpixelText = true
            isDither = true
        }

        /*
         * Sedikit efek glow lembut.
         */
        val glowPaint = Paint(paint).apply {
            style = Paint.Style.FILL
            color = Color.argb(95, 255, 255, 255)
            maskFilter = BlurMaskFilter(
                height * 0.018f,
                BlurMaskFilter.Blur.NORMAL
            )
        }

        /*
         * Shadow sangat tipis supaya angka mempunyai
         * efek kaca/bercahaya, bukan font putih biasa.
         */
        val shadowPaint = Paint(paint).apply {
            color = Color.argb(45, 0, 0, 0)
            maskFilter = BlurMaskFilter(
                height * 0.010f,
                BlurMaskFilter.Blur.NORMAL
            )
        }

        val metrics = paint.fontMetrics

        /*
         * Tinggi visual angka.
         */
        val glyphHeight = metrics.descent - metrics.ascent

        /*
         * Baseline dibuat supaya angka berada di tengah.
         */
        val baseline = (height - glyphHeight) / 2f - metrics.ascent

        /*
         * Lebar karakter asli font kemudian dipadatkan
         * secara horizontal supaya bentuknya tinggi/narrow.
         */
        val textWidth = paint.measureText(time)

        val targetWidth = width * 0.78f
        val scaleX = (targetWidth / textWidth)
            .coerceIn(0.48f, 0.72f)

        val centerX = width / 2f

        canvas.save()

        canvas.scale(scaleX, 1f, centerX, height / 2f)

        /*
         * Shadow.
         */
        canvas.drawText(
            time,
            (width - textWidth) / 2f,
            baseline + height * 0.012f,
            shadowPaint
        )

        /*
         * Glow.
         */
        canvas.drawText(
            time,
            (width - textWidth) / 2f,
            baseline,
            glowPaint
        )

        /*
         * Core angka putih tipis.
         */
        paint.color = Color.argb(238, 255, 255, 255)

        canvas.drawText(
            time,
            (width - textWidth) / 2f,
            baseline,
            paint
        )

        canvas.restore()

        return bitmap
    }
