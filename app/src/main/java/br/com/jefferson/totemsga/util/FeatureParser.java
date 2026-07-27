package br.com.jefferson.totemsga.util;

import java.util.HashMap;
import java.util.Map;

public class FeatureParser {

    public static final String FACIAL = "FACIAL";
    public static final String TRIAGEM = "TRIAGEM";
    public static final String TRIAGEM_OBRIGATORIA = "TRIAGEM_OBRIGATORIA";
    public static final String PRIORIDADE = "PRIORIDADE";
    public static final String VISIVEL = "VISIVEL";
    public static final String NOME = "NOME";
    public static final String TOLERANCIA = "tolerancia";
    public static final String SEGUNDAVIA = "SEGUNDAVIA";

    /**
     * Parses a string like "FACIAL;TOLERANCIA30;TRIAGEM=false"
     * @param descricao The string to parse
     * @return A map of keys and values as strings
     */
    public static Map<String, String> parseParams(String descricao) {
        Map<String, String> params = new HashMap<>();
        if (descricao == null || descricao.isEmpty()) return params;

        String[] parts = descricao.split(";");
        for (String part : parts) {
            String cleanPart = part.trim();
            if (cleanPart.isEmpty()) continue;

            // Case: TOLERANCIA30 (Key followed by digits)
            if (cleanPart.matches("(?i)" + TOLERANCIA + "\\d+")) {
                String value = cleanPart.replaceAll("(?i)" + TOLERANCIA, "");
                params.put(TOLERANCIA, value);
            }
            // Case: KEY=VALUE
            else if (cleanPart.contains("=")) {
                String[] kv = cleanPart.split("=");
                if (kv.length == 2) {
                    params.put(kv[0].trim().toLowerCase(), kv[1].trim().toLowerCase());
                }
            } else {
                // If present without = value, it's considered "true"
                params.put(cleanPart.toLowerCase(), "true");
            }
        }
        return params;
    }

    /**
     * Parses a string like "FACIAL;BIOMETRIA" or "FACIAL=false;BIOMETRIA"
     * @param descricao The string to parse
     * @return A map of features and their boolean values
     */
    public static Map<String, Boolean> parse(String descricao) {
        Map<String, Boolean> features = new HashMap<>();
        if (descricao == null || descricao.isEmpty()) return features;

        String[] parts = descricao.split(";");
        for (String part : parts) {
            String cleanPart = part.trim().toUpperCase();
            if (cleanPart.isEmpty()) continue;

            if (cleanPart.contains("=")) {
                String[] kv = cleanPart.split("=");
                if (kv.length == 2) {
                    features.put(kv[0].trim(), Boolean.parseBoolean(kv[1].trim().toLowerCase()));
                }
            } else {
                // If present without = value, it's true
                features.put(cleanPart, true);
            }
        }
        return features;
    }

    /**
     * Checks if a specific feature is enabled in the map.
     */
    public static boolean isEnabled(Map<String, Boolean> features, String featureName) {
        if (features == null) return false;
        Boolean val = features.get(featureName.toUpperCase());
        return val != null && val;
    }
}
