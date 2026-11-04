package id.wawan.glassclock.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.appwidget.AppWidgetManager
import android.content.ComponentName

class ClockTickReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val manager = AppWidgetManager.getInstance(context)
        val provider = ComponentName(context, GlassClockWidgetProvider::class.java)
        val widgetIds = manager.getAppWidgetIds(provider)
        if (widgetIds.isEmpty()) return

        GlassClockWidgetProvider.updateAll(context)

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> ClockTicker.schedule(context)
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_LOCALE_CHANGED -> ClockTicker.schedule(context)
        }
    }
}
