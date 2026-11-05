package id.wawan.netblocker;

import android.app.Activity;
import android.content.Intent;
import android.Manifest;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends Activity {
    private static final int REQUEST_VPN = 4101;
    private static final int REQUEST_NOTIFICATIONS = 4102;
    private static final String PREFS = "netblocker_prefs";
    private static final String KEY_BLOCKED = "blocked_packages";

    private final List<AppItem> allApps = new ArrayList<>();
    private LinearLayout appList;
    private TextView statusTitle;
    private TextView statusSummary;
    private TextView blockedCount;
    private Button firewallButton;
    private EditText searchBox;
    private String currentQuery = "";
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.setStatusBarColor(Color.rgb(16, 21, 31));
        window.setNavigationBarColor(Color.rgb(16, 21, 31));
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        buildUi();
        loadApps();
        renderApps();
        refreshStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(16, 21, 31));
        root.setPadding(dp(18), dp(26), dp(18), dp(12));
        if (Build.VERSION.SDK_INT >= 29) {
            root.setOnApplyWindowInsetsListener((view, insets) -> {
                view.setPadding(dp(18), dp(18) + insets.getSystemWindowInsetTop(), dp(18), dp(10) + insets.getSystemWindowInsetBottom());
                return insets;
            });
        }

        TextView brand = text("NETBLOCKER", 13, Color.rgb(105, 215, 196), Typeface.BOLD);
        brand.setLetterSpacing(0.16f);
        root.addView(brand, matchWrap());

        TextView title = text("Kendalikan akses internet", 25, Color.WHITE, Typeface.BOLD);
        LinearLayout.LayoutParams titleLp = matchWrap();
        titleLp.topMargin = dp(8);
        root.addView(title, titleLp);

        TextView subtitle = text("Pilih aplikasi yang tidak boleh terhubung ke jaringan.", 13, Color.rgb(166, 177, 194), Typeface.NORMAL);
        LinearLayout.LayoutParams subLp = matchWrap();
        subLp.topMargin = dp(5);
        root.addView(subtitle, subLp);

        LinearLayout statusCard = new LinearLayout(this);
        statusCard.setOrientation(LinearLayout.VERTICAL);
        statusCard.setPadding(dp(16), dp(14), dp(16), dp(14));
        statusCard.setBackground(roundRect(Color.rgb(28, 38, 52), dp(18), Color.rgb(49, 67, 83)));
        LinearLayout.LayoutParams cardLp = matchWrap();
        cardLp.topMargin = dp(20);
        root.addView(statusCard, cardLp);

        LinearLayout statusTop = new LinearLayout(this);
        statusTop.setGravity(Gravity.CENTER_VERTICAL);
        statusTitle = text("Firewall tidak aktif", 16, Color.WHITE, Typeface.BOLD);
        statusTop.addView(statusTitle, new LinearLayout.LayoutParams(0, -2, 1));
        blockedCount = text("0 DIPILIH", 10, Color.rgb(105, 215, 196), Typeface.BOLD);
        blockedCount.setPadding(dp(9), dp(6), dp(9), dp(6));
        blockedCount.setBackground(roundRect(Color.rgb(34, 69, 69), dp(30), Color.TRANSPARENT));
        statusTop.addView(blockedCount);
        statusCard.addView(statusTop);

        statusSummary = text("Aplikasi lain tetap menggunakan koneksi internet normal.", 12, Color.rgb(175, 189, 204), Typeface.NORMAL);
        LinearLayout.LayoutParams summaryLp = matchWrap();
        summaryLp.topMargin = dp(7);
        statusCard.addView(statusSummary, summaryLp);

        firewallButton = new Button(this);
        firewallButton.setText("AKTIFKAN FIREWALL");
        firewallButton.setTextColor(Color.rgb(10, 27, 30));
        firewallButton.setTextSize(14);
        firewallButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        firewallButton.setAllCaps(false);
        firewallButton.setPadding(dp(12), dp(8), dp(12), dp(8));
        firewallButton.setBackground(roundRect(Color.rgb(105, 215, 196), dp(14), Color.TRANSPARENT));
        LinearLayout.LayoutParams buttonLp = matchWrap();
        buttonLp.topMargin = dp(12);
        statusCard.addView(firewallButton, buttonLp);
        firewallButton.setOnClickListener(v -> onFirewallButtonPressed());

        TextView listTitle = text("Aplikasi terpasang", 17, Color.WHITE, Typeface.BOLD);
        LinearLayout.LayoutParams listTitleLp = matchWrap();
        listTitleLp.topMargin = dp(22);
        root.addView(listTitle, listTitleLp);

        searchBox = new EditText(this);
        searchBox.setSingleLine(true);
        searchBox.setTextSize(14);
        searchBox.setTextColor(Color.WHITE);
        searchBox.setHintTextColor(Color.rgb(125, 139, 158));
        searchBox.setHint("Cari nama aplikasi...");
        searchBox.setPadding(dp(14), 0, dp(14), 0);
        searchBox.setBackground(roundRect(Color.rgb(29, 38, 51), dp(13), Color.rgb(52, 65, 82)));
        LinearLayout.LayoutParams searchLp = new LinearLayout.LayoutParams(-1, dp(48));
        searchLp.topMargin = dp(10);
        root.addView(searchBox, searchLp);
        searchBox.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentQuery = s == null ? "" : s.toString();
                renderApps();
            }
            @Override public void afterTextChanged(Editable s) { }
        });

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        appList = new LinearLayout(this);
        appList.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(appList, new ScrollView.LayoutParams(-1, -2));
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(-1, 0, 1f);
        scrollLp.topMargin = dp(8);
        root.addView(scroll, scrollLp);

        TextView footnote = text("NetBlocker memakai VPN lokal di perangkat. Tidak ada server VPN eksternal dan aplikasi ini tidak meneruskan data ke internet.", 11, Color.rgb(126, 141, 160), Typeface.NORMAL);
        LinearLayout.LayoutParams footLp = matchWrap();
        footLp.topMargin = dp(6);
        root.addView(footnote, footLp);

        setContentView(root);
    }

    private void loadApps() {
        allApps.clear();
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> installed;
        if (Build.VERSION.SDK_INT >= 33) {
            installed = pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0));
        } else {
            //noinspection deprecation
            installed = pm.getInstalledApplications(0);
        }

        for (ApplicationInfo info : installed) {
            if (info.packageName == null || info.packageName.equals(getPackageName())) continue;
            if (!info.enabled) continue;
            Intent launchIntent = pm.getLaunchIntentForPackage(info.packageName);
            if (launchIntent == null) continue;
            String label;
            try {
                label = pm.getApplicationLabel(info).toString();
            } catch (Exception e) {
                label = info.packageName;
            }
            boolean system = (info.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
            allApps.add(new AppItem(info.packageName, label, info, system));
        }
        Collections.sort(allApps, Comparator.comparing(item -> item.label.toLowerCase(Locale.ROOT)));
    }

    private void renderApps() {
        if (appList == null) return;
        appList.removeAllViews();
        String query = currentQuery == null ? "" : currentQuery.trim().toLowerCase(Locale.ROOT);
        Set<String> blocked = getBlockedPackages();
        int shown = 0;
        for (AppItem item : allApps) {
            if (!query.isEmpty() && !item.label.toLowerCase(Locale.ROOT).contains(query)
                    && !item.packageName.toLowerCase(Locale.ROOT).contains(query)) continue;
            appList.addView(createAppRow(item, blocked.contains(item.packageName)));
            shown++;
        }
        if (shown == 0) {
            TextView empty = text(query.isEmpty() ? "Tidak ada aplikasi yang dapat ditampilkan." : "Aplikasi tidak ditemukan.", 13, Color.rgb(160, 173, 190), Typeface.NORMAL);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(28), 0, dp(28));
            appList.addView(empty, matchWrap());
        }
        refreshStatus();
    }

    private View createAppRow(AppItem item, boolean isBlocked) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(4), dp(9), dp(2), dp(9));

        ImageView icon = new ImageView(this);
        try {
            icon.setImageDrawable(item.info.loadIcon(getPackageManager()));
        } catch (Exception ignored) { }
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(43), dp(43));
        iconLp.rightMargin = dp(12);
        row.addView(icon, iconLp);

        LinearLayout details = new LinearLayout(this);
        details.setOrientation(LinearLayout.VERTICAL);
        details.setGravity(Gravity.CENTER_VERTICAL);
        TextView name = text(item.label, 14, Color.WHITE, Typeface.BOLD);
        name.setMaxLines(1);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        details.addView(name, matchWrap());
        TextView packageName = text(item.packageName + (item.isSystem ? "  ·  sistem" : ""), 10, Color.rgb(132, 148, 168), Typeface.NORMAL);
        packageName.setMaxLines(1);
        packageName.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams packageLp = matchWrap();
        packageLp.topMargin = dp(3);
        details.addView(packageName, packageLp);
        row.addView(details, new LinearLayout.LayoutParams(0, -2, 1f));

        Switch toggle = new Switch(this);
        toggle.setShowText(false);
        toggle.setChecked(isBlocked);
        toggle.setContentDescription("Blokir internet untuk " + item.label);
        row.addView(toggle, new LinearLayout.LayoutParams(-2, -2));
        toggle.setOnCheckedChangeListener((button, checked) -> {
            Set<String> updated = getBlockedPackages();
            if (checked) updated.add(item.packageName); else updated.remove(item.packageName);
            saveBlockedPackages(updated);
            if (FirewallVpnService.isRunning(this)) {
                FirewallVpnService.reload(this);
            }
            renderApps();
        });

        View divider = new View(this);
        divider.setBackgroundColor(Color.rgb(39, 49, 64));
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.addView(row, matchWrap());
        container.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));
        return container;
    }

    private void onFirewallButtonPressed() {
        if (FirewallVpnService.isRunning(this)) {
            stopService(new Intent(this, FirewallVpnService.class));
            refreshStatus();
            return;
        }
        Set<String> blocked = getBlockedPackages();
        if (blocked.isEmpty()) {
            Toast.makeText(this, "Pilih setidaknya satu aplikasi untuk diblokir.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATIONS);
            return;
        }
        beginVpnAuthorization();
    }

    private void beginVpnAuthorization() {
        Intent prepare = VpnService.prepare(this);
        if (prepare != null) {
            startActivityForResult(prepare, REQUEST_VPN);
        } else {
            startVpnService();
        }
    }

    private void startVpnService() {
        Intent intent = new Intent(this, FirewallVpnService.class);
        intent.setAction(FirewallVpnService.ACTION_RELOAD);
        try {
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent);
            else startService(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Tidak dapat menjalankan firewall: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
        refreshStatus();
        mainHandler.postDelayed(this::refreshStatus, 700);
        mainHandler.postDelayed(this::refreshStatus, 1600);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_NOTIFICATIONS) {
            // A denied notification permission does not prevent a VPN from running.
            beginVpnAuthorization();
        }
    }

    @Override
    @Deprecated
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_VPN) {
            if (resultCode == RESULT_OK) startVpnService();
            else Toast.makeText(this, "Izin VPN belum diberikan. Firewall tidak diaktifkan.", Toast.LENGTH_LONG).show();
            refreshStatus();
        }
    }

    private void refreshStatus() {
        if (statusTitle == null) return;
        boolean active = FirewallVpnService.isRunning(this);
        Set<String> blocked = getBlockedPackages();
        blockedCount.setText(blocked.size() + " DIPILIH");
        if (active) {
            statusTitle.setText("Firewall aktif");
            statusTitle.setTextColor(Color.rgb(105, 215, 196));
            statusSummary.setText("Koneksi internet dari " + blocked.size() + " aplikasi terpilih sedang diblokir.");
            firewallButton.setText("MATIKAN FIREWALL");
            firewallButton.setTextColor(Color.WHITE);
            firewallButton.setBackground(roundRect(Color.rgb(161, 74, 82), dp(14), Color.TRANSPARENT));
        } else {
            statusTitle.setText("Firewall tidak aktif");
            statusTitle.setTextColor(Color.WHITE);
            String lastError = getSharedPreferences("netblocker_prefs", MODE_PRIVATE).getString("last_error", "");
            if (lastError != null && !lastError.isEmpty()) {
                statusSummary.setText("Firewall gagal aktif: " + lastError);
            } else {
                statusSummary.setText(blocked.size() + " aplikasi dipilih. Aplikasi lain tetap memakai internet normal.");
            }
            firewallButton.setText("AKTIFKAN FIREWALL");
            firewallButton.setTextColor(Color.rgb(10, 27, 30));
            firewallButton.setBackground(roundRect(Color.rgb(105, 215, 196), dp(14), Color.TRANSPARENT));
        }
    }

    static Set<String> readBlockedPackages(android.content.Context context) {
        Set<String> values = context.getSharedPreferences(PREFS, MODE_PRIVATE).getStringSet(KEY_BLOCKED, Collections.emptySet());
        return new HashSet<>(values == null ? Collections.emptySet() : values);
    }

    private Set<String> getBlockedPackages() {
        return readBlockedPackages(this);
    }

    private void saveBlockedPackages(Set<String> values) {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putStringSet(KEY_BLOCKED, new HashSet<>(values)).apply();
    }

    private TextView text(String value, float size, int color, int style) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTypeface(Typeface.DEFAULT, style);
        return view;
    }

    private GradientDrawable roundRect(int color, int radius, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        if (strokeColor != Color.TRANSPARENT) drawable.setStroke(dp(1), strokeColor);
        return drawable;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(-1, -2);
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private static class AppItem {
        final String packageName;
        final String label;
        final ApplicationInfo info;
        final boolean isSystem;
        AppItem(String packageName, String label, ApplicationInfo info, boolean isSystem) {
            this.packageName = packageName;
            this.label = label;
            this.info = info;
            this.isSystem = isSystem;
        }
    }
}
