package br.com.jefferson.totemsga;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import br.com.jefferson.totemsga.util.SessionManager;
import br.com.jefferson.totemsga.ads.AdManager;

public abstract class BaseKioskFragment extends Fragment {

    private Handler inactivityHandler = new Handler(Looper.getMainLooper());
    private Runnable inactivityRunnable = this::onInactivityTimeout;
    protected SessionManager sessionManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionManager = new SessionManager(requireContext());
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Listen for touches on the root view to reset timers
        view.setOnTouchListener((v, event) -> {
            resetInactivityTimer();
            return false; // Don't consume the event
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (sessionManager.isKioskMode() && getActivity() instanceof MainActivity) {
            // Re-aplicação suave na troca de telas
            View decorView = getActivity().getWindow().getDecorView();
            if ((decorView.getSystemUiVisibility() & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                ((MainActivity) getActivity()).setInteractingWithSystem(false);
            }
        }
    }

    public void resetInactivityTimer() {
        // Reseta o timer local do fragmento (Voltar ao início)
        inactivityHandler.removeCallbacks(inactivityRunnable);
        if (isTimerEnabled()) {
            int timeout = sessionManager.getScreeningTimeout();
            if (timeout > 0) {
                inactivityHandler.postDelayed(inactivityRunnable, timeout * 1000L);
            }
        }

        // Reseta o timer global de publicidade
        if (sessionManager.isAdsEnabled()) {
            AdManager.getInstance().resetTimer();
        }
    }

    protected void setupBackButton(android.widget.Button btn, android.view.View contentContainer) {
        if (btn == null) return;

        float density = getResources().getDisplayMetrics().density;
        int screenWidth = getResources().getDisplayMetrics().widthPixels;

        // Size and Font (dynamic logic)
        int widthPercent = sessionManager.getBackButtonWidthPercent();
        int heightDp = sessionManager.getBackButtonHeight();
        int fontSizeSp = sessionManager.getBackButtonFontSize();

        android.view.ViewGroup.LayoutParams lp = btn.getLayoutParams();
        if (lp != null) {
            if (widthPercent > 0) {
                lp.width = (int) (screenWidth * (widthPercent / 100.0f));
            }
            if (heightDp > 0) {
                lp.height = (int) (heightDp * density);
            }
            btn.setLayoutParams(lp);
        }
        if (fontSizeSp > 0) btn.setTextSize(fontSizeSp);

        // Position and Alignment Logic
        if (contentContainer != null && contentContainer.getLayoutParams() instanceof android.widget.LinearLayout.LayoutParams) {
            android.widget.LinearLayout.LayoutParams containerLp = (android.widget.LinearLayout.LayoutParams) contentContainer.getLayoutParams();
            if (sessionManager.getBackButtonPosition() == 1) { // Fixed Footer
                containerLp.height = 0;
                containerLp.weight = 1.0f;
            } else { // Below Buttons
                containerLp.height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
                containerLp.weight = 0;
            }
            contentContainer.setLayoutParams(containerLp);
        }

        // Horizontal Alignment
        if (btn.getLayoutParams() instanceof android.widget.LinearLayout.LayoutParams) {
            android.widget.LinearLayout.LayoutParams btnLp = (android.widget.LinearLayout.LayoutParams) btn.getLayoutParams();
            int alignment = sessionManager.getBackButtonAlignment();
            switch (alignment) {
                case 1: // Centro
                    btnLp.gravity = android.view.Gravity.CENTER_HORIZONTAL;
                    break;
                case 2: // Direita
                    btnLp.gravity = android.view.Gravity.END;
                    break;
                default: // Esquerda
                    btnLp.gravity = android.view.Gravity.START;
                    break;
            }
            btn.setLayoutParams(btnLp);
        }
    }

    protected void applyGlobalSpacing(android.view.View root) {
        if (root == null) return;
        float density = getResources().getDisplayMetrics().density;

        // 1. Decor Heights
        android.view.View top = root.findViewById(R.id.viewTopDecoration);
        if (top != null) {
            android.view.ViewGroup.LayoutParams lp = top.getLayoutParams();
            lp.height = (int) (sessionManager.getTopGradientHeight() * density);
            top.setLayoutParams(lp);
        }
        android.view.View topBack = root.findViewById(R.id.viewTopDecorationBack);
        if (topBack != null) {
            android.view.ViewGroup.LayoutParams lp = topBack.getLayoutParams();
            lp.height = (int) ((sessionManager.getTopGradientHeight() + 20) * density);
            topBack.setLayoutParams(lp);
        }
        android.view.View bottom = root.findViewById(R.id.viewBottomDecoration);
        if (bottom != null) {
            android.view.ViewGroup.LayoutParams lp = bottom.getLayoutParams();
            lp.height = (int) (sessionManager.getBottomGradientHeight() * density);
            bottom.setLayoutParams(lp);
        }
        android.view.View bottomBack = root.findViewById(R.id.viewBottomDecorationBack);
        if (bottomBack != null) {
            android.view.ViewGroup.LayoutParams lp = bottomBack.getLayoutParams();
            lp.height = (int) ((sessionManager.getBottomGradientHeight() + 20) * density);
            bottomBack.setLayoutParams(lp);
        }

        // 2. Logo Margin
        android.view.View logo = root.findViewById(R.id.ivLogoSelection);
        if (logo == null) logo = root.findViewById(R.id.ivLogoScreening);
        if (logo == null) logo = root.findViewById(R.id.ivLogoSuccess);
        
        if (logo != null && logo.getLayoutParams() instanceof android.view.ViewGroup.MarginLayoutParams) {
            android.view.ViewGroup.MarginLayoutParams mlp = (android.view.ViewGroup.MarginLayoutParams) logo.getLayoutParams();
            mlp.topMargin = (int) (sessionManager.getLogoMarginTop() * density);
            logo.setLayoutParams(mlp);
        }

        // 3. Title (Header) Margin
        android.view.View header = root.findViewById(R.id.llHeader);
        if (header != null && header.getLayoutParams() instanceof android.view.ViewGroup.MarginLayoutParams) {
            android.view.ViewGroup.MarginLayoutParams mlp = (android.view.ViewGroup.MarginLayoutParams) header.getLayoutParams();
            mlp.topMargin = (int) (sessionManager.getTitleMarginTop() * density);
            header.setLayoutParams(mlp);
        }
    }

    protected boolean isTimerEnabled() {
        return false;
    }

    protected void onInactivityTimeout() {
        if (isAdded() && !getParentFragmentManager().isStateSaved()) {
            if (sessionManager.isAdsEnabled()) {
                // Dispara publicidade centralizada
                AdManager.getInstance().forceShowAd();
            } else {
                // Volta para o início
                getParentFragmentManager().popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).startFlow();
                }
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        inactivityHandler.removeCallbacks(inactivityRunnable);
    }
}
