package id.wawan.notiftone;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int REQ_PICK_MP3 = 2101;
    private static final int REQ_WRITE_STORAGE = 2102;

    private LinearLayout appList;
    private EditText searchBox;
    private final List<AppItem> installedApps = new ArrayList<>();

    private static class AppItem {
        final String label;
        final String packageName;
        final android.graphics.drawable.Drawable icon;

        AppItem(String label, String packageName,
                android.graphics.drawable.Drawable icon) {
            this.label = label;
            this.packageName = packageName;
            this.icon = icon;
        }
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView makeText(String value, float sizeSp, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sizeSp);
        view.setTextColor(color);
        return view;
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        buildScreen();
        loadInstalledApps();
    }

    private void buildScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(8));
        root.setBackgroundColor(Color.rgb(10, 11, 16));

        TextView title = makeText("Notif Tone Manager", 25, Color.WHITE);
        title.setTypeface(null, 1);
        root.addView(title, new LinearLayout.LayoutParams(-1, dp(42)));

        TextView subtitle = makeText(
                "Impor MP3, lalu pilih nada untuk aplikasi Anda.",
                13, Color.rgb(165, 166, 180));
        root.addView(subtitle, new LinearLayout.LayoutParams(-1, dp(30)));

        Button importButton = new Button(this);
        importButton.setText("＋  Impor MP3 ke Notifications");
        importButton.setAllCaps(false);
        importButton.setTextColor(Color.WHITE);
        importButton.setBackground(roundBackground(
                Color.rgb(91, 72, 165), dp(16)));
        LinearLayout.LayoutParams importLp =
                new LinearLayout.LayoutParams(-1, dp(54));
        importLp.setMargins(0, dp(8), 0, dp(8));
        root.addView(importButton, importLp);
        importButton.setOnClickListener(v -> startMp3Import());

        TextView help = makeText(
                "Setelah impor: pilih aplikasi → kategori notifikasi → Sound → pilih MP3. " +
                "Android meminta pemilihan akhir dilakukan di Settings.",
                12, Color.rgb(180, 180, 192));
        help.setPadding(dp(12), dp(10), dp(12), dp(10));
        help.setBackground(roundBackground(Color.rgb(23, 24, 33), dp(14)));
        root.addView(help, new LinearLayout.LayoutParams(-1, -2));

        searchBox = new EditText(this);
        searchBox.setSingleLine(true);
        searchBox.setHint("Cari aplikasi…");
        searchBox.setHintTextColor(Color.rgb(125, 127, 140));
        searchBox.setTextColor(Color.WHITE);
        searchBox.setPadding(dp(14), 0, dp(14), 0);
        searchBox.setBackground(roundBackground(
                Color.rgb(25, 26, 35), dp(16)));
        LinearLayout.LayoutParams searchLp =
                new LinearLayout.LayoutParams(-1, dp(52));
        searchLp.setMargins(0, dp(12), 0, dp(10));
        root.addView(searchBox, searchLp);

        ScrollView scroll = new ScrollView(this);
        appList = new LinearLayout(this);
        appList.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(appList);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        searchBox.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start,
                                                    int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start,
                                                int before, int count) {
                renderApps(s.toString());
            }
            @Override public void afterTextChanged(android.text.Editable s) { }
        });

        setContentView(root);
    }

    private GradientDrawable roundBackground(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    private void loadInstalledApps() {
        PackageManager pm = getPackageManager();
        installedApps.clear();

        try {
            List<ApplicationInfo> apps =
                    pm.getInstalledApplications(PackageManager.GET_META_DATA);

            for (ApplicationInfo info : apps) {
                if (getPackageName().equals(info.packageName)) continue;

                String label = pm.getApplicationLabel(info).toString();
                installedApps.add(new AppItem(
                        label, info.packageName, pm.getApplicationIcon(info)));
            }

            installedApps.sort(Comparator.comparing(
                    a -> a.label.toLowerCase(Locale.ROOT)));
        } catch (Exception e) {
            Toast.makeText(this, "Tidak dapat membaca daftar aplikasi.",
                    Toast.LENGTH_LONG).show();
        }

        renderApps("");
    }

    private void renderApps(String filter) {
        appList.removeAllViews();
        String query = filter == null
                ? "" : filter.trim().toLowerCase(Locale.ROOT);

        for (AppItem item : installedApps) {
            if (!query.isEmpty()
                    && !item.label.toLowerCase(Locale.ROOT).contains(query)
                    && !item.packageName.toLowerCase(Locale.ROOT).contains(query)) {
                continue;
            }
            appList.addView(createAppRow(item));
        }
    }

    private View createAppRow(AppItem item) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(13), dp(9), dp(12), dp(9));
        row.setBackground(roundBackground(Color.rgb(20, 21, 29), dp(18)));

        ImageView icon = new ImageView(this);
        icon.setImageDrawable(item.icon);
        row.addView(icon, new LinearLayout.LayoutParams(dp(48), dp(48)));

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.setGravity(Gravity.CENTER_VERTICAL);
        labels.setPadding(dp(12), 0, dp(6), 0);

        TextView name = makeText(item.label, 16, Color.WHITE);
        TextView pkg = makeText(item.packageName, 10.5f,
                Color.rgb(137, 139, 153));
        labels.addView(name);
        labels.addView(pkg);
        row.addView(labels, new LinearLayout.LayoutParams(0, dp(58), 1));

        TextView arrow = makeText("›", 31, Color.rgb(190, 169, 255));
        arrow.setGravity(Gravity.CENTER);
        row.addView(arrow, new LinearLayout.LayoutParams(dp(30), dp(58)));

        row.setOnClickListener(v -> openAppNotificationSettings(item.packageName));

        LinearLayout.LayoutParams rowLp =
                new LinearLayout.LayoutParams(-1, dp(76));
        rowLp.setMargins(0, 0, 0, dp(8));
        row.setLayoutParams(rowLp);
        return row;
    }

    private void startMp3Import() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
                && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQ_WRITE_STORAGE);
            return;
        }
        openMp3Picker();
    }

    private void openMp3Picker() {
        Intent picker = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        picker.addCategory(Intent.CATEGORY_OPENABLE);
        picker.setType("audio/*");
        picker.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivityForResult(picker, REQ_PICK_MP3);
        } catch (Exception e) {
            Toast.makeText(this, "Pemilih file tidak tersedia.",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_WRITE_STORAGE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openMp3Picker();
            } else {
                Toast.makeText(this,
                        "Izin penyimpanan diperlukan pada Android lama.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_PICK_MP3
                && resultCode == RESULT_OK
                && data != null
                && data.getData() != null) {
            Uri selected = data.getData();
            try {
                String name = getDisplayName(selected);
                String lower = name.toLowerCase(Locale.ROOT);
                String mime = getContentResolver().getType(selected);

                if (!lower.endsWith(".mp3")
                        && (mime == null || !mime.toLowerCase(Locale.ROOT)
                        .contains("mpeg"))) {
                    Toast.makeText(this, "Pilih file MP3.",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                Uri saved = copyIntoNotifications(selected, name);
                if (saved != null) {
                    showImportSuccess(name);
                } else {
                    Toast.makeText(this, "MP3 gagal disimpan.",
                            Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, "Gagal mengimpor MP3: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private String getDisplayName(Uri uri) {
        String name = null;
        try (Cursor cursor = getContentResolver().query(
                uri, new String[]{OpenableColumns.DISPLAY_NAME},
                null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                name = cursor.getString(0);
            }
        } catch (Exception ignored) { }

        if (name == null || name.trim().isEmpty()) {
            name = "Notification_" + System.currentTimeMillis() + ".mp3";
        }
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private Uri copyIntoNotifications(Uri source, String displayName)
            throws Exception {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, displayName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, "audio/mpeg");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_NOTIFICATIONS + "/");
            values.put(MediaStore.MediaColumns.IS_PENDING, 1);

            Uri destination = getContentResolver().insert(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values);

            if (destination == null) {
                throw new IllegalStateException("MediaStore menolak file.");
            }

            boolean completed = false;
            try (InputStream input = getContentResolver().openInputStream(source);
                 OutputStream output = getContentResolver().openOutputStream(
                         destination, "w")) {
                if (input == null || output == null) {
                    throw new IllegalStateException("Tidak dapat membuka file.");
                }
                copyStream(input, output);
                output.flush();

                ContentValues publish = new ContentValues();
                publish.put(MediaStore.MediaColumns.IS_PENDING, 0);
                getContentResolver().update(destination, publish, null, null);
                completed = true;
                return destination;
            } finally {
                if (!completed) {
                    getContentResolver().delete(destination, null, null);
                }
            }
        }

        File directory = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_NOTIFICATIONS);
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IllegalStateException("Folder Notifications tidak dapat dibuat.");
        }

        File destination = new File(directory, displayName);
        try (InputStream input = getContentResolver().openInputStream(source);
             OutputStream output = new FileOutputStream(destination)) {
            if (input == null) {
                throw new IllegalStateException("File sumber tidak dapat dibuka.");
            }
            copyStream(input, output);
        }

        MediaScannerConnection.scanFile(
                this,
                new String[]{destination.getAbsolutePath()},
                new String[]{"audio/mpeg"},
                null);

        return Uri.fromFile(destination);
    }

    private void copyStream(InputStream input, OutputStream output)
            throws Exception {
        byte[] buffer = new byte[16 * 1024];
        int count;
        while ((count = input.read(buffer)) != -1) {
            output.write(buffer, 0, count);
        }
    }

    private void showImportSuccess(String fileName) {
        new AlertDialog.Builder(this)
                .setTitle("MP3 berhasil diimpor")
                .setMessage("\"" + fileName + "\" disimpan ke folder Notifications.\n\n" +
                        "Sekarang pilih aplikasi dari daftar. Di pengaturan Samsung, " +
                        "buka kategori notifikasi → Sound → pilih file MP3 tersebut.\n\n" +
                        "Jika kategori tidak terlihat, aktifkan " +
                        "\"Manage notification categories for each app\" di " +
                        "Settings → Notifications → Advanced settings.")
                .setPositiveButton("Pilih aplikasi", (dialog, which) -> {
                    if (searchBox != null) searchBox.requestFocus();
                })
                .setNeutralButton("Pengaturan notifikasi", (dialog, which) -> {
                    try {
                        startActivity(new Intent(Settings.ACTION_NOTIFICATION_SETTINGS));
                    } catch (Exception ignored) { }
                })
                .show();
    }

    private void openAppNotificationSettings(String packageName) {
        try {
            Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, packageName);
            startActivity(intent);
        } catch (Exception e) {
            try {
                Intent fallback = new Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                fallback.setData(Uri.parse("package:" + packageName));
                startActivity(fallback);
            } catch (Exception ignored) {
                Toast.makeText(this, "Pengaturan aplikasi tidak dapat dibuka.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }
}
