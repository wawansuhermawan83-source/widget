package id.wawan.netblocker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.net.VpnService;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class FirewallVpnService extends VpnService {
    public static final String ACTION_RELOAD = "id.wawan.netblocker.action.RELOAD";
    public static final String ACTION_STOP = "id.wawan.netblocker.action.STOP";

    private static final String PREFS = "netblocker_prefs";
    private static final String KEY_RUNNING = "firewall_running";
    private static final String CHANNEL_ID = "netblocker_firewall";
    private static final int NOTIFICATION_ID = 7201;

    private ParcelFileDescriptor vpnInterface;
    private Thread packetReader;
    private final AtomicBoolean readerActive = new AtomicBoolean(false);

    public static boolean isRunning(android.content.Context context) {
        return context.getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(KEY_RUNNING, false);
    }

    public static void reload(android.content.Context context) {
        Intent intent = new Intent(context, FirewallVpnService.class);
        intent.setAction(ACTION_RELOAD);
        try {
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent);
            else context.startService(intent);
        } catch (Exception ignored) {
            // The visible Activity refreshes the status; the user can retry from the main button.
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? ACTION_RELOAD : intent.getAction();
        if (ACTION_STOP.equals(action)) {
            stopFirewall();
            stopSelf(startId);
            return START_NOT_STICKY;
        }

        try {
            startAsForeground();
            rebuildTunnel();
        } catch (Exception e) {
            setRunning(false);
            setLastError(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            closeTunnel();
            stopForeground(true);
            stopSelf(startId);
        }
        return START_NOT_STICKY;
    }

    private void startAsForeground() {
        Intent openApp = new Intent(this, MainActivity.class);
        openApp.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                this, 0, openApp, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent stopIntent = new Intent(this, FirewallVpnService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this, 1, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        builder.setSmallIcon(android.R.drawable.ic_lock_lock)
                .setContentTitle("NetBlocker aktif")
                .setContentText("Koneksi aplikasi pilihan sedang diblokir")
                .setContentIntent(contentIntent)
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Matikan", stopPendingIntent);

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, builder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED);
        } else {
            startForeground(NOTIFICATION_ID, builder.build());
        }
    }

    private void rebuildTunnel() throws Exception {
        closeTunnel();
        Set<String> blockedPackages = MainActivity.readBlockedPackages(this);
        if (blockedPackages.isEmpty()) {
            setRunning(false);
            setLastError("");
            stopForeground(true);
            stopSelf();
            return;
        }

        if (VpnService.prepare(this) != null) {
            throw new SecurityException("Izin VPN belum diberikan");
        }

        Builder builder = new Builder()
                .setSession("NetBlocker")
                .setMtu(1500)
                .addAddress("10.111.0.1", 32)
                .addDnsServer("10.111.0.2")
                .addRoute("0.0.0.0", 0);

        // Android lets us route only the selected packages into this local VPN.
        // Every packet arriving here is discarded; unselected apps bypass this VPN.
        int accepted = 0;
        PackageManager pm = getPackageManager();
        for (String packageName : blockedPackages) {
            if (packageName == null || packageName.equals(getPackageName())) continue;
            try {
                pm.getApplicationInfo(packageName, 0);
                builder.addAllowedApplication(packageName);
                accepted++;
            } catch (PackageManager.NameNotFoundException ignored) {
                // A selected app may have been uninstalled since the list was saved.
            }
        }

        if (accepted == 0) {
            setRunning(false);
            setLastError("Aplikasi yang dipilih tidak ditemukan.");
            stopForeground(true);
            stopSelf();
            return;
        }

        // Capture IPv6 as well. Fail closed if the IPv6 route cannot be configured,
        // rather than claiming a complete block while a second IP family might escape.
        builder.addAddress("fd42:111::1", 128);
        builder.addRoute("::", 0);

        vpnInterface = builder.establish();
        if (vpnInterface == null) {
            throw new IllegalStateException("Android tidak dapat membuat koneksi VPN lokal.");
        }

        readerActive.set(true);
        packetReader = new Thread(this::discardPackets, "NetBlocker-PacketDropper");
        packetReader.setDaemon(true);
        packetReader.start();
        setLastError("");
        setRunning(true);
    }

    private void discardPackets() {
        ParcelFileDescriptor descriptor = vpnInterface;
        if (descriptor == null) return;
        byte[] buffer = new byte[32767];
        try (FileInputStream input = new FileInputStream(descriptor.getFileDescriptor())) {
            while (readerActive.get()) {
                int length = input.read(buffer);
                if (length < 0) break;
                // Intentionally discard the packet. There is no upstream tunnel or forwarding.
            }
        } catch (IOException ignored) {
            // Closing the interface during reload/stop ends this loop normally.
        } finally {
            if (readerActive.get()) {
                readerActive.set(false);
                setRunning(false);
                stopSelf();
            }
        }
    }

    private void stopFirewall() {
        closeTunnel();
        setRunning(false);
        stopForeground(true);
    }

    private void closeTunnel() {
        readerActive.set(false);
        ParcelFileDescriptor oldInterface = vpnInterface;
        vpnInterface = null;
        if (oldInterface != null) {
            try { oldInterface.close(); } catch (IOException ignored) { }
        }
        Thread oldReader = packetReader;
        packetReader = null;
        if (oldReader != null && oldReader != Thread.currentThread()) {
            oldReader.interrupt();
        }
    }

    private void setRunning(boolean running) {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(KEY_RUNNING, running).apply();
    }

    private void setLastError(String message) {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString("last_error", message == null ? "" : message).apply();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Status firewall", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Menampilkan status NetBlocker saat pemblokiran aktif.");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onRevoke() {
        stopFirewall();
        stopSelf();
        super.onRevoke();
    }

    @Override
    public void onDestroy() {
        closeTunnel();
        setRunning(false);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }
}
