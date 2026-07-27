package br.com.jefferson.totemsga.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * Verifica periodicamente se o totem tem conectividade de rede. Se ficar
 * sem conexão por tempo demais, religa o rádio Wi-Fi automaticamente
 * (desliga e liga de novo) para recuperar sozinho, sem intervenção manual.
 */
public class WifiWatchdog {
    private static final String TAG = "WifiWatchdog";
    private static final long CHECK_INTERVAL_MS = 60_000; // 1 min
    private static final int FAILURES_BEFORE_RECOVERY = 2; // ~2 min sem conexão
    private static final long RECOVERY_COOLDOWN_MS = 3 * 60_000; // 3 min entre tentativas
    private static final long WIFI_TOGGLE_DELAY_MS = 3_000; // tempo desligado antes de religar

    private static WifiWatchdog instance;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Context appContext;
    private int consecutiveFailures = 0;
    private long lastRecoveryAttempt = 0;
    private boolean running = false;

    private WifiWatchdog() {}

    public static synchronized WifiWatchdog getInstance() {
        if (instance == null) instance = new WifiWatchdog();
        return instance;
    }

    public void start(Context context) {
        if (running) return;
        this.appContext = context.getApplicationContext();
        running = true;
        handler.postDelayed(checkRunnable, CHECK_INTERVAL_MS);
        Log.d(TAG, "🐕 Watchdog de Wi-Fi iniciado.");
    }

    public void stop() {
        running = false;
        handler.removeCallbacks(checkRunnable);
    }

    private final Runnable checkRunnable = new Runnable() {
        @Override
        public void run() {
            checkConnectivity();
            if (running) handler.postDelayed(this, CHECK_INTERVAL_MS);
        }
    };

    private void checkConnectivity() {
        if (appContext == null) return;
        boolean connected = isNetworkConnected();

        if (connected) {
            if (consecutiveFailures > 0) Log.d(TAG, "✅ Conectividade normalizada.");
            consecutiveFailures = 0;
            return;
        }

        consecutiveFailures++;
        Log.w(TAG, "⚠️ Sem conectividade (" + consecutiveFailures + "/" + FAILURES_BEFORE_RECOVERY + ")");

        if (consecutiveFailures >= FAILURES_BEFORE_RECOVERY) {
            long now = System.currentTimeMillis();
            if (now - lastRecoveryAttempt >= RECOVERY_COOLDOWN_MS) {
                lastRecoveryAttempt = now;
                consecutiveFailures = 0;
                recoverWifi();
            } else {
                Log.d(TAG, "⏳ Em cooldown, aguardando antes de tentar recuperar de novo.");
            }
        }
    }

    private boolean isNetworkConnected() {
        try {
            ConnectivityManager cm = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return false;
            NetworkInfo info = cm.getActiveNetworkInfo();
            return info != null && info.isConnected();
        } catch (Exception e) {
            return false;
        }
    }

    private void recoverWifi() {
        try {
            Log.w(TAG, "🔄 Religando rádio Wi-Fi para recuperar a conexão...");
            final WifiManager wifiManager = (WifiManager) appContext.getSystemService(Context.WIFI_SERVICE);
            if (wifiManager == null) return;
            wifiManager.setWifiEnabled(false);
            handler.postDelayed(() -> {
                try {
                    wifiManager.setWifiEnabled(true);
                    Log.w(TAG, "✅ Wi-Fi religado.");
                } catch (Exception e) {
                    Log.e(TAG, "Erro ao religar Wi-Fi", e);
                }
            }, WIFI_TOGGLE_DELAY_MS);
        } catch (Exception e) {
            Log.e(TAG, "Erro ao tentar recuperar Wi-Fi", e);
        }
    }
}
