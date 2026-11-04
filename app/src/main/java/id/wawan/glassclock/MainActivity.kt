package id.wawan.glassclock

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import id.wawan.glassclock.widget.GlassClockWidgetProvider

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.add_widget_button).setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = AppWidgetManager.getInstance(this)
                val provider = ComponentName(this, GlassClockWidgetProvider::class.java)
                val requested = manager.requestPinAppWidget(provider, null, null)
                if (!requested) showManualInstructions()
            } else {
                showManualInstructions()
            }
        }
    }

    private fun showManualInstructions() {
        Toast.makeText(
            this,
            "Tekan lama layar Home → Widget → Glass Clock → Tambahkan",
            Toast.LENGTH_LONG
        ).show()
    }
}
