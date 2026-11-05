package id.wawan.motionwallpaper

import android.app.Activity
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Movie
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var statusText: TextView
    private lateinit var modeButton: Button
    private var selectedUri: Uri? = null
    private var selectedKind: String? = null
    private var fillScreen = true

    private val preferences by lazy {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedUri = preferences.getString(KEY_URI, null)?.let(Uri::parse)
        selectedKind = preferences.getString(KEY_KIND, null)
        fillScreen = preferences.getBoolean(KEY_FILL, true)
        buildScreen()
        refreshStatus()
    }

    private fun buildScreen() {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(0xFF0C1019.toInt())
            isFillViewport = true
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(30), dp(24), dp(32))
        }
        scroll.addView(content)

        val title = TextView(this).apply {
            text = "MOTION WALLPAPER"
            textSize = 25f
            letterSpacing = 0.10f
            setTextColor(0xFFF5F6FA.toInt())
        }
        content.addView(title, matchWrap())

        val subtitle = TextView(this).apply {
            text = "Pasang GIF atau video sebagai wallpaper bergerak di Home Screen."
            textSize = 14f
            setTextColor(0xFFB8C0D1.toInt())
            setPadding(0, dp(10), 0, dp(24))
        }
        content.addView(subtitle, matchWrap())

        statusText = TextView(this).apply {
            textSize = 14f
            setTextColor(0xFFE8ECF5.toInt())
            setBackgroundColor(0xFF1A2230.toInt())
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        content.addView(statusText, matchWrap())

        content.addView(actionButton("Pilih GIF") {
            openPicker("image/gif", REQUEST_GIF)
        }, buttonParams())

        content.addView(actionButton("Pilih video MP4") {
            openPicker("video/mp4", REQUEST_VIDEO)
        }, buttonParams())

        modeButton = actionButton("") {
            fillScreen = !fillScreen
            preferences.edit().putBoolean(KEY_FILL, fillScreen).apply()
            refreshModeButton()
        }
        content.addView(modeButton, buttonParams())
        refreshModeButton()

        content.addView(actionButton("Pratinjau dan pasang wallpaper") {
            launchWallpaperPreview()
        }, buttonParams())

        content.addView(actionButton("Hapus pilihan media") {
            clearSelection()
        }, buttonParams())

        val instructions = TextView(this).apply {
            text = "Cara pakai:\n1. Pilih GIF atau video dari penyimpanan.\n2. Pilih mode Isi layar atau Pas seluruh gambar.\n3. Tekan Pratinjau dan pasang wallpaper.\n4. Di layar sistem, tekan Set wallpaper lalu pilih Home screen.\n\nVideo diputar tanpa suara dan diulang otomatis. Animasi dijeda ketika wallpaper tidak terlihat untuk menghemat baterai."
            textSize = 13f
            setTextColor(0xFF9EA8BA.toInt())
            setPadding(0, dp(24), 0, 0)
        }
        content.addView(instructions, matchWrap())
        setContentView(scroll)
    }

    private fun actionButton(label: String, action: () -> Unit): Button = Button(this).apply {
        text = label
        isAllCaps = false
        textSize = 15f
        setOnClickListener { action() }
    }

    private fun buttonParams(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = dp(12)
        }

    private fun matchWrap(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun openPicker(mimeType: String, requestCode: Int) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = mimeType
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        try {
            startActivityForResult(intent, requestCode)
        } catch (error: Exception) {
            Toast.makeText(this, "Pemilih file tidak tersedia: ${error.message}", Toast.LENGTH_LONG).show()
        }
    }

    @Deprecated("Uses ACTION_OPEN_DOCUMENT for Android 8+")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        val uri = data?.data ?: return

        val kind = when (requestCode) {
            REQUEST_GIF -> "gif"
            REQUEST_VIDEO -> "video"
            else -> return
        }

        try {
            val grantedFlags = (data.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (grantedFlags != 0) {
                contentResolver.takePersistableUriPermission(uri, grantedFlags)
            }
        } catch (_: SecurityException) {
            // Some document providers issue a temporary read grant only.
        }

        if (kind == "gif" && !isAnimatedGif(uri)) {
            Toast.makeText(this, "File yang dipilih bukan GIF yang dapat dibaca.", Toast.LENGTH_LONG).show()
            return
        }
        if (kind == "video" && !isVideo(uri)) {
            Toast.makeText(this, "Pilih file video MP4 yang valid.", Toast.LENGTH_LONG).show()
            return
        }

        selectedUri = uri
        selectedKind = kind
        preferences.edit()
            .putString(KEY_URI, uri.toString())
            .putString(KEY_KIND, kind)
            .apply()
        refreshStatus()
        Toast.makeText(this, "Media berhasil dipilih.", Toast.LENGTH_SHORT).show()
    }

    private fun isAnimatedGif(uri: Uri): Boolean {
        return try {
            contentResolver.openInputStream(uri)?.use { input ->
                val header = ByteArray(6)
                val count = input.read(header)
                count == 6 && String(header, Charsets.US_ASCII).startsWith("GIF8")
            } ?: false
        } catch (_: Exception) {
            false
        }
    }

    private fun isVideo(uri: Uri): Boolean {
        val type = contentResolver.getType(uri).orEmpty()
        if (type.startsWith("video/", ignoreCase = true)) return true
        val name = displayName(uri).lowercase()
        return name.endsWith(".mp4") || name.endsWith(".m4v") || name.endsWith(".3gp")
    }

    private fun displayName(uri: Uri): String {
        return try {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0).orEmpty() else uri.lastPathSegment.orEmpty()
            } ?: uri.lastPathSegment.orEmpty()
        } catch (_: Exception) {
            uri.lastPathSegment.orEmpty()
        }
    }

    private fun refreshStatus() {
        val uri = selectedUri
        statusText.text = if (uri == null || selectedKind == null) {
            "Belum ada media dipilih.\nPilih GIF atau video MP4 untuk memulai."
        } else {
            val typeLabel = if (selectedKind == "gif") "GIF" else "VIDEO"
            "MEDIA TERPILIH · $typeLabel\n${displayName(uri)}"
        }
    }

    private fun refreshModeButton() {
        modeButton.text = if (fillScreen) "Mode: Isi layar (crop)" else "Mode: Pas seluruh gambar"
    }

    private fun clearSelection() {
        selectedUri?.let { uri ->
            try {
                contentResolver.releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {
                // No persisted grant to release.
            }
        }
        selectedUri = null
        selectedKind = null
        preferences.edit().remove(KEY_URI).remove(KEY_KIND).apply()
        refreshStatus()
    }

    private fun launchWallpaperPreview() {
        if (selectedUri == null || selectedKind == null) {
            Toast.makeText(this, "Pilih GIF atau video terlebih dahulu.", Toast.LENGTH_LONG).show()
            return
        }
        try {
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                putExtra(
                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    ComponentName(this@MainActivity, MotionWallpaperService::class.java)
                )
            }
            startActivity(intent)
        } catch (error: Exception) {
            Toast.makeText(this, "Pratinjau wallpaper gagal dibuka: ${error.message}", Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        const val PREFS_NAME = "motion_wallpaper_settings"
        const val KEY_URI = "selected_media_uri"
        const val KEY_KIND = "selected_media_kind"
        const val KEY_FILL = "fill_screen"
        private const val REQUEST_GIF = 4101
        private const val REQUEST_VIDEO = 4102
    }
}
