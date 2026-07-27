package br.com.jefferson.totemsga.ads;

import android.os.Handler;
import android.os.Looper;
import br.com.jefferson.totemsga.util.SessionManager;

public class AdManager {
    private static AdManager instance;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable adRunnable;
    private AdListener listener;
    private int inactivityTimeSeconds;
    private boolean isAdShowing = false;
    private boolean enabled = false;
    private boolean suspended = false;

    public interface AdListener {
        void onInactivityDetected();
    }

    private AdManager() {}

    public static synchronized AdManager getInstance() {
        if (instance == null) {
            instance = new AdManager();
        }
        return instance;
    }

    public void init(SessionManager sessionManager, AdListener listener) {
        this.enabled = sessionManager.isAdsEnabled();
        this.inactivityTimeSeconds = sessionManager.getAdsInactivityTime();
        this.listener = listener;
        
        if (enabled) {
            startTimer();
        }
    }

    public void startTimer() {
        if (!enabled || isAdShowing || suspended) return;
        
        stopTimer();
        adRunnable = this::forceShowAd;
        handler.postDelayed(adRunnable, inactivityTimeSeconds * 1000L);
    }

    public void stopTimer() {
        if (adRunnable != null) {
            handler.removeCallbacks(adRunnable);
        }
    }

    public void resetTimer() {
        stopTimer();
        if (!suspended) {
            startTimer();
        }
    }

    public void forceShowAd() {
        if (listener != null && !isAdShowing) {
            isAdShowing = true;
            handler.post(() -> listener.onInactivityDetected());
        }
    }

    public void setSuspended(boolean suspended) {
        this.suspended = suspended;
        // Cancelar imediatamente qualquer execução pendente
        stopTimer();
        
        if (!suspended) {
            startTimer();
        }
    }

    public void onAdDismissed() {
        isAdShowing = false;
        startTimer();
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) stopTimer();
        else startTimer();
    }

    public void reloadConfig(SessionManager sessionManager) {
        this.enabled = sessionManager.isAdsEnabled();
        this.inactivityTimeSeconds = sessionManager.getAdsInactivityTime();
        if (enabled) {
            resetTimer();
        } else {
            stopTimer();
        }
    }

    public boolean isAdShowing() {
        return isAdShowing;
    }
}
