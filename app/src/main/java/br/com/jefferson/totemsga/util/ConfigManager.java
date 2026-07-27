package br.com.jefferson.totemsga.util;

import android.content.Context;

/**
 * Utilitário para gerenciar configurações globais do aplicativo,
 * centralizando o acesso via SessionManager.
 */
public class ConfigManager {

    /**
     * Retorna o número de colunas para o grid de Departamentos.
     * Mínimo de 2 colunas, Default de 3 se não configurado.
     */
    public static int getDepartamentoColunas(Context context) {
        SessionManager sessionManager = new SessionManager(context);
        int cols = sessionManager.getDeptGrid();
        if (cols < 2) return 3; // Default 3 e nunca 1
        return cols;
    }

    /**
     * Retorna o número de colunas para o grid de Serviços.
     * Mínimo de 2 colunas, Default de 3 se não configurado.
     */
    public static int getServicoColunas(Context context) {
        SessionManager sessionManager = new SessionManager(context);
        int cols = sessionManager.getServiceGrid();
        if (cols < 2) return 3; // Default 3 e nunca 1
        return cols;
    }
}
