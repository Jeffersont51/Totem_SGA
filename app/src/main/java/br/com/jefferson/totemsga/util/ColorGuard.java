package br.com.jefferson.totemsga.util;

import android.graphics.Color;

/**
 * Proteção de contraste. As cores do app são configuráveis no Admin; se alguém
 * escolher uma cor de tema parecida com a cor de fundo, botões e ícones somem.
 * Estes métodos trocam a cor por uma legível quando o contraste é insuficiente.
 */
public final class ColorGuard {

    public static final int BRAND_ORANGE = Color.parseColor("#F47B20");
    private static final int DARK_TEXT = Color.parseColor("#333333");

    // Contraste mínimo para um elemento (botão, ícone) se destacar do fundo
    private static final double MIN_SHAPE_CONTRAST = 1.3;
    // Contraste mínimo para um texto ser lido sobre o próprio fundo
    private static final double MIN_TEXT_CONTRAST = 2.0;

    private ColorGuard() {}

    /** Devolve a cor original se ela se destaca do fundo; senão, o laranja da marca. */
    public static int visibleOn(int color, int background) {
        if (contrast(color, background) >= MIN_SHAPE_CONTRAST) return color;
        if (contrast(BRAND_ORANGE, background) >= MIN_SHAPE_CONTRAST) return BRAND_ORANGE;
        return DARK_TEXT;
    }

    /** Devolve a cor de texto original se for legível sobre o fundo; senão, branco ou escuro. */
    public static int readableOn(int textColor, int background) {
        if (contrast(textColor, background) >= MIN_TEXT_CONTRAST) return textColor;
        return contrast(Color.WHITE, background) >= contrast(DARK_TEXT, background) ? Color.WHITE : DARK_TEXT;
    }

    public static int parse(String hex, int fallback) {
        try {
            return Color.parseColor(hex);
        } catch (Exception e) {
            return fallback;
        }
    }

    /** Razão de contraste WCAG entre duas cores (1 = iguais, 21 = preto sobre branco). */
    public static double contrast(int a, int b) {
        double la = luminance(a);
        double lb = luminance(b);
        double lighter = Math.max(la, lb);
        double darker = Math.min(la, lb);
        return (lighter + 0.05) / (darker + 0.05);
    }

    private static double luminance(int color) {
        return 0.2126 * channel(Color.red(color))
                + 0.7152 * channel(Color.green(color))
                + 0.0722 * channel(Color.blue(color));
    }

    private static double channel(int value) {
        double v = value / 255.0;
        return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
    }
}
