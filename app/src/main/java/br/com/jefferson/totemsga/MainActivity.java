package br.com.jefferson.totemsga;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import br.com.jefferson.totemsga.ads.AdFragment;
import br.com.jefferson.totemsga.ads.AdManager;
import br.com.jefferson.totemsga.util.SessionManager;
import br.com.jefferson.totemsga.util.SunmiPrinterHelper;

public class MainActivity extends BaseActivity implements AdManager.AdListener {

    private View adminTrigger;
    private final android.os.Handler pingHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final android.os.Handler kioskHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable kioskRunnable = this::enableKioskMode;
    private boolean isKeyboardVisible = false;
    private boolean isInteractingWithSystem = false;
    
    private final Runnable pingRunnable = new Runnable() {
        @Override
        public void run() {
            performPing();
            pingHandler.postDelayed(this, 30 * 60 * 1000); // 30 min
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (sessionManager.getApiUrl().isEmpty()) {
            startActivity(new Intent(this, ConfigActivity.class));
            finish();
            return;
        }

        adminTrigger = findViewById(R.id.adminTrigger);
        View btnSettings = findViewById(R.id.btnSettings);

        adminTrigger.setOnLongClickListener(v -> {
            showAdminLogin();
            return true;
        });

        btnSettings.setOnClickListener(v -> showAdminLogin());

        applyTheme();
        startFlow();
        
        AdManager.getInstance().init(sessionManager, this);
        SunmiPrinterHelper.getInstance().initPrinter(this);
        setupKioskListeners();
    }

    private void setupKioskListeners() {
        View decorView = getWindow().getDecorView();
        
        // Listener de visibilidade do sistema (Recuperação do modo imersivo)
        decorView.setOnSystemUiVisibilityChangeListener(visibility -> {
            if (sessionManager.isKioskMode() && (visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                // Sistema "acordou" as barras, agendar re-ocultação se seguro
                scheduleKioskRetry(1500);
            }
        });

        // Detector de teclado via Layout Height
        decorView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            android.graphics.Rect r = new android.graphics.Rect();
            decorView.getWindowVisibleDisplayFrame(r);
            int screenHeight = decorView.getRootView().getHeight();
            int keypadHeight = screenHeight - r.bottom;

            boolean wasVisible = isKeyboardVisible;
            isKeyboardVisible = keypadHeight > screenHeight * 0.15; // > 15% da tela

            if (wasVisible && !isKeyboardVisible) {
                // Teclado fechou, recuperar Kiosk imediatamente
                scheduleKioskRetry(300);
            }
        });
    }

    public void setInteractingWithSystem(boolean interacting) {
        this.isInteractingWithSystem = interacting;
        if (!interacting) scheduleKioskRetry(500);
    }

    private void scheduleKioskRetry(int delay) {
        kioskHandler.removeCallbacks(kioskRunnable);
        if (!isKeyboardVisible && !isInteractingWithSystem && sessionManager.isKioskMode()) {
            kioskHandler.postDelayed(kioskRunnable, delay);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && sessionManager != null && sessionManager.isKioskMode()) {
            hideSystemUI();
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        
        // Ensure settings are fresh
        if (sessionManager != null) {
            sessionManager.reload();
        }

        if (sessionManager.isKioskMode()) {
            enableKioskMode();
        } else {
            disableKioskMode();
        }
        
        // Refresh flow to apply new settings (like grid columns) immediately
        applyTheme();
        startFlow();
        
        // Inicia o timer de publicidade ao voltar para a atividade
        AdManager.getInstance().startTimer();

        // Inicia o Ping de manutenção de sessão
        pingHandler.removeCallbacks(pingRunnable);
        pingHandler.postDelayed(pingRunnable, 5000); // Primeiro ping após 5s
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Para o timer de publicidade ao sair da atividade (Admin, etc)
        AdManager.getInstance().stopTimer();

        // Para o Ping ao sair
        pingHandler.removeCallbacks(pingRunnable);
    }

    private void enableKioskMode() {
        if (!isKeyboardVisible && !isInteractingWithSystem) {
            hideSystemUI();
        }
        try {
            android.app.ActivityManager am = (android.app.ActivityManager) getSystemService(android.content.Context.ACTIVITY_SERVICE);
            if (am != null && am.getLockTaskModeState() == android.app.ActivityManager.LOCK_TASK_MODE_NONE) {
                startLockTask();
            }
        } catch (Exception e) {}
    }

    private void hideSystemUI() {
        if (isKeyboardVisible || isInteractingWithSystem) return;
        
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    private void disableKioskMode() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        try {
            stopLockTask();
        } catch (Exception e) {}
    }

    @Override
    public boolean dispatchTouchEvent(android.view.MotionEvent ev) {
        // Reseta o timer do fragmento atual se for um BaseKioskFragment
        androidx.fragment.app.Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.container);
        if (currentFragment instanceof BaseKioskFragment) {
            ((BaseKioskFragment) currentFragment).resetInactivityTimer();
        }

        // Reseta o timer de publicidade global em qualquer toque
        if (sessionManager.isAdsEnabled() && !AdManager.getInstance().isAdShowing()) {
            AdManager.getInstance().resetTimer();
        }
        
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public void onInactivityDetected() {
        if (!isFinishing() && !isDestroyed()) {
            // Fechar teclado forçadamente antes de mostrar publicidade
            hideKeyboard();

            // Reaplica o modo Kiosk com um pequeno delay para garantir que o fechamento do teclado
            // não restaure as barras de sistema permanentemente
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                if (sessionManager.isKioskMode()) {
                    hideSystemUI();
                }
            }, 300);

            // Verifica se o anúncio já não está sendo mostrado
            if (getSupportFragmentManager().findFragmentByTag("ADS") == null) {
                getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                        .add(android.R.id.content, new AdFragment(), "ADS")
                        .commitAllowingStateLoss();
            }
        }
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    public void startFlow() {
        // Clear backstack thoroughly to avoid overlapping screens
        androidx.fragment.app.FragmentManager fm = getSupportFragmentManager();
        fm.popBackStackImmediate(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);

        // Remove any existing fragments manually to ensure a clean state
        androidx.fragment.app.FragmentTransaction ft = fm.beginTransaction();
        for (androidx.fragment.app.Fragment fragment : fm.getFragments()) {
            if (fragment != null && !(fragment instanceof AdFragment)) {
                ft.remove(fragment);
            }
        }

        if (sessionManager.isGroupByDept()) {
            ft.replace(R.id.container, SelectionFragment.newInstance(1, -1))
                    .commitAllowingStateLoss();
        } else {
            ft.replace(R.id.container, SelectionFragment.newInstance(2, -1))
                    .commitAllowingStateLoss();
        }
    }

    @Override
    public void onBackPressed() {
        androidx.fragment.app.Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.container);
        if (fragment instanceof SuccessFragment) {
            startFlow();
        } else if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            super.onBackPressed();
        } else {
            if (!sessionManager.isKioskMode()) {
                super.onBackPressed();
            }
        }
    }

    private void showAdminLogin() {
        android.widget.EditText et = new android.widget.EditText(this);
        et.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Acesso Admin")
                .setMessage("Digite a senha:")
                .setView(et)
                .setPositiveButton("Entrar", (dialog, which) -> {
                    if (et.getText().toString().equals(sessionManager.getAdminPass())) {
                        startActivity(new Intent(this, AdminActivity.class));
                    } else {
                        android.widget.Toast.makeText(this, "Senha incorreta", android.widget.Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void performPing() {
        if (sessionManager == null || sessionManager.getApiUrl().isEmpty()) return;
        br.com.jefferson.totemsga.api.ApiService api = br.com.jefferson.totemsga.api.RetrofitClient.getInstance(sessionManager);
        if (api != null) {
            api.getUnidades().enqueue(new retrofit2.Callback<java.util.List<br.com.jefferson.totemsga.model.Unidade>>() {
                @Override public void onResponse(retrofit2.Call<java.util.List<br.com.jefferson.totemsga.model.Unidade>> call, retrofit2.Response<java.util.List<br.com.jefferson.totemsga.model.Unidade>> response) {
                    android.util.Log.d("MainActivity", "Ping realizado com sucesso (Unidades)");
                }
                @Override public void onFailure(retrofit2.Call<java.util.List<br.com.jefferson.totemsga.model.Unidade>> call, Throwable t) {
                    android.util.Log.e("MainActivity", "Falha no Ping: " + t.getMessage());
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SunmiPrinterHelper.getInstance().deinitPrinter(this);
    }
}
