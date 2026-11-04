package id.wawan.glassclock.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock

/** Schedules a battery-conscious minute refresh for the home-screen clock. */
object ClockTicker {
    private const val ACTION_TICK = "id.wawan.glassclock.ACTION_MINUTE_TICK"
    private const val INTERVAL_MS = 60_000L

    fun schedule(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = tickPendingIntent(context)
        val firstTick = SystemClock.elapsedRealtime() + (INTERVAL_MS - (System.currentTimeMillis() % INTERVAL_MS))
        alarmManager.setInexactRepeating(
            AlarmManager.ELAPSED_REALTIME,
            firstTick,
            INTERVAL_MS,
            pendingIntent
        )
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(tickPendingIntent(context))
    }

    private fun tickPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, ClockTickReceiver::class.java).setAction(ACTION_TICK)
        return PendingIntent.getBroadcast(
            context,
            7401,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
