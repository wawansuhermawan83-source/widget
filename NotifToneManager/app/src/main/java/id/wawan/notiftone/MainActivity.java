package id.wawan.notiftone;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private LinearLayout list;
    private EditText search;
    private final List<AppItem> apps = new ArrayList<>();

    static class AppItem {
        final String label, pkg;
        final android.graphics.drawable.Drawable icon;
        AppItem(String label, String pkg, android.graphics.drawable.Drawable icon) {
            this.label = label; this.pkg = pkg; this.icon = icon;
        }
    }

    private int dp(float v) {
        return (int)(v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView tv(String s, float size, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);
        return t;
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        buildUi();
        loadApps();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), 0);
        root.setBackgroundColor(Color.rgb(11, 11, 15));

        TextView title = tv("Notif Tone Manager", 25, Color.WHITE);
        title.setTypeface(null, 1);
        root.addView(title, new LinearLayout.LayoutParams(-1, dp(40)));

        root.addView(tv("Kelola nada notifikasi aplikasi", 13, Color.rgb(160,160,170)),
                new LinearLayout.LayoutParams(-1, dp(28)));

        search = new EditText(this);
        search.setHint("Cari aplikasi…");
        search.setHintTextColor(Color.rgb(120,120,130));
        search.setTextColor(Color.WHITE);
        search.setSingleLine(true);
        search.setPadding(dp(14), 0, dp(14), 0);
        GradientDrawable searchBg = new GradientDrawable();
        searchBg.setColor(Color.rgb(25,25,32));
        searchBg.setCornerRadius(dp(16));
        search.setBackground(searchBg);
        LinearLayout.LayoutParams searchLp = new LinearLayout.LayoutParams(-1, dp(52));
        searchLp.setMargins(0, dp(8), 0, dp(10));
        root.addView(search, searchLp);

        ScrollView scroll = new ScrollView(this);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        search.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) { render(s.toString()); }
            public void afterTextChanged(android.text.Editable e) {}
        });

        setContentView(root);
    }

    private void loadApps() {
        PackageManager pm = getPackageManager();
        for (ApplicationInfo ai : pm.getInstalledApplications(PackageManager.GET_META_DATA)) {
            if (ai.packageName.equals(getPackageName())) continue;
            apps.add(new AppItem(
                    pm.getApplicationLabel(ai).toString(),
                    ai.packageName,
                    pm.getApplicationIcon(ai)
            ));
        }
        apps.sort(Comparator.comparing(a -> a.label.toLowerCase(Locale.ROOT)));
        render("");
    }

    private void render(String q) {
        list.removeAllViews();
        String query = q.toLowerCase(Locale.ROOT).trim();

        for (AppItem a : apps) {
            if (!query.isEmpty()
                    && !a.label.toLowerCase(Locale.ROOT).contains(query)
                    && !a.pkg.toLowerCase(Locale.ROOT).contains(query)) {
                continue;
            }
            list.addView(makeRow(a));
        }
    }

    private View makeRow(AppItem a) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(14), dp(10), dp(10), dp(10));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.rgb(20,20,27));
        bg.setCornerRadius(dp(18));
        card.setBackground(bg);

        ImageView icon = new ImageView(this);
        icon.setImageDrawable(a.icon);
        card.addView(icon, new LinearLayout.LayoutParams(dp(48), dp(48)));

        LinearLayout names = new LinearLayout(this);
        names.setOrientation(LinearLayout.VERTICAL);
        names.setPadding(dp(12), 0, dp(6), 0);
        names.addView(tv(a.label, 16, Color.WHITE));
        names.addView(tv(a.pkg, 10.5f, Color.rgb(135,135,145)));
        card.addView(names, new LinearLayout.LayoutParams(0, dp(58), 1));

        TextView arrow = tv("›", 32, Color.rgb(184,167,255));
        arrow.setGravity(Gravity.CENTER);
        card.addView(arrow, new LinearLayout.LayoutParams(dp(32), dp(58)));

        card.setOnClickListener(v -> openNotificationSettings(a.pkg));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(78));
        lp.setMargins(0, 0, 0, dp(8));
        card.setLayoutParams(lp);
        return card;
    }

    private void openNotificationSettings(String pkg) {
        try {
            Intent i = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            i.putExtra(Settings.EXTRA_APP_PACKAGE, pkg);
            startActivity(i);
        } catch (Exception e) {
            Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            i.setData(Uri.parse("package:" + pkg));
            startActivity(i);
        }
    }
}
