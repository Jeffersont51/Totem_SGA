package br.com.jefferson.totemsga.util;

import java.util.regex.Pattern;

public class ValidationUtils {

    public static boolean isValidCPF(String cpf) {
        if (cpf == null) return false;
        cpf = cpf.replaceAll("\\D", "");
        if (cpf.length() != 11) return false;

        // Common invalid CPFs
        if (cpf.matches("(\\d)\\1{10}")) return false;

        try {
            int d1 = 0, d2 = 0;
            int digit1, digit2, rest, sum;
            int weight = 10;

            for (int i = 0; i < 9; i++) {
                sum = (Character.getNumericValue(cpf.charAt(i)) * weight);
                d1 += sum;
                weight--;
            }

            rest = d1 % 11;
            if (rest < 2) digit1 = 0;
            else digit1 = 11 - rest;

            if (digit1 != Character.getNumericValue(cpf.charAt(9))) return false;

            weight = 11;
            for (int i = 0; i < 10; i++) {
                sum = (Character.getNumericValue(cpf.charAt(i)) * weight);
                d2 += sum;
                weight--;
            }

            rest = d2 % 11;
            if (rest < 2) digit2 = 0;
            else digit2 = 11 - rest;

            return digit2 == Character.getNumericValue(cpf.charAt(10));
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isValidCNPJ(String cnpj) {
        if (cnpj == null || cnpj.isEmpty()) {
            return false;
        }

        // 1. Remover máscara e normalizar (letras maiúsculas)
        String limpo = cnpj.replaceAll("[^0-9a-zA-Z]", "").toUpperCase();

        // 2. Verificar tamanho (14 caracteres)
        if (limpo.length() != 14) {
            return false;
        }

        // 3. Verificar se os 2 últimos são números (dígitos verificadores)
        String dv = limpo.substring(12, 14);
        if (!dv.matches("\\d{2}")) {
            return false;
        }

        // 4. Verificar se todos os caracteres são iguais (evitar sequências repetidas comuns)
        if (limpo.matches("(\\w)\\1{13}")) {
            return false;
        }

        // 5. Calcular e validar os dígitos verificadores
        return calcularDigitosCNPJ(limpo);
    }

    private static boolean calcularDigitosCNPJ(String cnpj) {
        try {
            // Cálculo do Primeiro Dígito
            int soma = 0;
            int[] peso1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
            for (int i = 0; i < 12; i++) {
                soma += charToNumber(cnpj.charAt(i)) * peso1[i];
            }
            int digito1 = (soma % 11 < 2) ? 0 : 11 - (soma % 11);

            // Cálculo do Segundo Dígito
            soma = 0;
            int[] peso2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
            for (int i = 0; i < 13; i++) {
                soma += charToNumber(cnpj.charAt(i)) * peso2[i];
            }
            int digito2 = (soma % 11 < 2) ? 0 : 11 - (soma % 11);

            // Obter dígitos reais do documento
            int dv1 = Character.getNumericValue(cnpj.charAt(12));
            int dv2 = Character.getNumericValue(cnpj.charAt(13));

            return digito1 == dv1 && digito2 == dv2;
        } catch (Exception e) {
            return false;
        }
    }

    private static int charToNumber(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0'; // 0-9
        } else if (c >= 'A' && c <= 'Z') {
            return c - 'A' + 17; // A=17, B=18, ... Z=42 (Conforme especificação RFB)
        }
        return 0;
    }

    public static boolean isValidPhone(String phone) {
        if (phone == null) return false;
        String clean = phone.replaceAll("\\D", "");
        // Valid Brazilian phones: 10 or 11 digits (after removing leading 0)
        return clean.length() == 10 || clean.length() == 11;
    }

    public static String formatCPF(String cpf) {
        String clean = cpf.replaceAll("\\D", "");
        if (clean.length() > 11) clean = clean.substring(0, 11);
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < clean.length(); i++) {
            if (i == 3 || i == 6) sb.append(".");
            else if (i == 9) sb.append("-");
            sb.append(clean.charAt(i));
        }
        return sb.toString();
    }

    public static String formatCNPJ(String cnpj) {
        String clean = cnpj.replaceAll("[^a-zA-Z0-9]", "");
        if (clean.length() > 14) clean = clean.substring(0, 14);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < clean.length(); i++) {
            if (i == 2 || i == 5) sb.append(".");
            else if (i == 8) sb.append("/");
            else if (i == 12) sb.append("-");
            sb.append(clean.charAt(i));
        }
        return sb.toString();
    }

    public static String formatPhone(String phone) {
        String clean = phone.replaceAll("\\D", "");
        if (clean.startsWith("0")) clean = clean.substring(1);
        
        if (clean.length() > 11) clean = clean.substring(0, 11);

        StringBuilder sb = new StringBuilder();
        if (clean.length() > 0) sb.append("(");
        for (int i = 0; i < clean.length(); i++) {
            if (i == 2) sb.append(") ");
            else if (i == 7 && clean.length() == 11) sb.append("-");
            else if (i == 6 && clean.length() == 10) sb.append("-");
            sb.append(clean.charAt(i));
        }
        return sb.toString();
    }
    
    public static String cleanDocument(String doc) {
        if (doc == null) return "";
        return doc.replaceAll("\\D", "");
    }
}
