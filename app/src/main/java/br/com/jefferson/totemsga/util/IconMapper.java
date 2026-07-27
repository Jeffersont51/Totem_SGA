package br.com.jefferson.totemsga.util;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;
import br.com.jefferson.totemsga.R;

public class IconMapper {

    private static final Map<String, Integer> DEPARTMENT_ICONS = new HashMap<>();
    private static final Map<String, Integer> SERVICE_ICONS = new HashMap<>();

    static {
        DEPARTMENT_ICONS.put("COMERCIAL", R.drawable.ic_briefcase);
        DEPARTMENT_ICONS.put("MEDICINA DO TRABALHO", R.drawable.ic_health_cross);
        DEPARTMENT_ICONS.put("MEDICO", R.drawable.ic_stethoscope);
        DEPARTMENT_ICONS.put("CONTROLADORIA", R.drawable.ic_chart);
        DEPARTMENT_ICONS.put("DEPARTAMENTO PESSOAL", R.drawable.ic_folder_hr);
        DEPARTMENT_ICONS.put("DIRETORIA", R.drawable.ic_star);
        DEPARTMENT_ICONS.put("E-COMMERCE", R.drawable.ic_cart);
        DEPARTMENT_ICONS.put("FINANCEIRO", R.drawable.ic_money);
        DEPARTMENT_ICONS.put("FISCAL", R.drawable.ic_document_seal);
        DEPARTMENT_ICONS.put("GESTAO DE GENTE", R.drawable.ic_people);
        DEPARTMENT_ICONS.put("INOVACOES", R.drawable.ic_lightbulb);
        DEPARTMENT_ICONS.put("JURIDICO", R.drawable.ic_scale);
        DEPARTMENT_ICONS.put("MANUTENCAO", R.drawable.ic_wrench);
        DEPARTMENT_ICONS.put("RECURSOS HUMANOS", R.drawable.ic_people);
        DEPARTMENT_ICONS.put("TECNOLOGIA DA INFORMACAO", R.drawable.ic_monitor);

        SERVICE_ICONS.put("ADMISSIONAL", R.drawable.ic_clipboard_check);
        SERVICE_ICONS.put("AVALIACAO MEDICA", R.drawable.ic_stethoscope);
        SERVICE_ICONS.put("DEMISSIONAL", R.drawable.ic_exit_arrow);
        SERVICE_ICONS.put("MEDICINA DO TRABALHO", R.drawable.ic_health_cross);
        SERVICE_ICONS.put("MEDICO", R.drawable.ic_stethoscope);
        SERVICE_ICONS.put("MUDANCA DE FUNCAO", R.drawable.ic_gears);
        SERVICE_ICONS.put("PERIODICO", R.drawable.ic_calendar);
        SERVICE_ICONS.put("RETORNO AO TRABALHO", R.drawable.ic_briefcase_cross);
    }

    private static final int DEPARTMENT_FALLBACK_ICON = R.drawable.ic_building;
    private static final int SERVICE_FALLBACK_ICON = R.drawable.ic_person; // pessoa, cobre nomes de atendentes

    public static int getDepartmentIcon(String nome) {
        String key = normalize(nome);
        for (Map.Entry<String, Integer> entry : DEPARTMENT_ICONS.entrySet()) {
            if (key.contains(normalize(entry.getKey()))) {
                return entry.getValue();
            }
        }
        return DEPARTMENT_FALLBACK_ICON;
    }

    public static int getServiceIcon(String nome) {
        String key = normalize(nome);
        for (Map.Entry<String, Integer> entry : SERVICE_ICONS.entrySet()) {
            if (key.contains(normalize(entry.getKey()))) {
                return entry.getValue();
            }
        }
        return SERVICE_FALLBACK_ICON;
    }

    private static String normalize(String input) {
        if (input == null) return "";
        return Normalizer.normalize(input.toUpperCase().trim(), Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "");
    }
}
