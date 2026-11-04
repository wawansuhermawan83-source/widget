package id.wawan.glassclock.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.widget.RemoteViews
import id.wawan.glassclock.MainActivity
import id.wawan.glassclock.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
            val component = ComponentName(context, GlassClockWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            ids.forEach { id ->
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
                220
            ).coerceAtLeast(150)

            val heightDp = options.getInt(
                AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,
                110
            ).coerceAtLeast(80)

            val timeSize = minOf(
                heightDp * 0.58f,
                widthDp * 0.43f
            ).coerceIn(54f, 132f)

            val views = RemoteViews(
                context.packageName,
                R.layout.widget_glass_clock
            )

            val time = SimpleDateFormat(
                "HH:mm",
                Locale.getDefault()
            ).format(Date())

            views.setTextViewText(R.id.widget_time, time)
            views.setTextViewTextSize(
                R.id.widget_time,
                TypedValue.COMPLEX_UNIT_SP,
                timeSize
            )

            val launchIntent = Intent(context, MainActivity::class.java)
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
    }
}
