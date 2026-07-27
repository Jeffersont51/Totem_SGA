package br.com.jefferson.totemsga.model;

import com.google.gson.annotations.SerializedName;
import java.util.Date;

public class Agendamento {
    public int id;
    public String data; // ISO 8601 (Ex: 2023-10-27T15:00:00)
    public String hora;
    
    @SerializedName("dataConfirmacao")
    public String dataConfirmacao; // Se preenchido, já foi confirmado
    
    public Cliente cliente;
    public Unidade unidade;
    
    // No NovoSGA 2, o serviço vem direto ou dentro de servicoUnidade
    public Servico servico;
    
    public static class Servico {
        public int id;
        public String nome;
    }

    public boolean isHoje() {
        if (data == null) return false;
        java.text.SimpleDateFormat sdfIso = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
        java.text.SimpleDateFormat sdfBr = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
        Date agora = new Date();
        String hojeIso = sdfIso.format(agora);
        String hojeBr = sdfBr.format(agora);
        
        return data.startsWith(hojeIso) || data.startsWith(hojeBr);
    }
    
    public boolean isConfirmado() {
        return dataConfirmacao != null && !dataConfirmacao.isEmpty();
    }
}
