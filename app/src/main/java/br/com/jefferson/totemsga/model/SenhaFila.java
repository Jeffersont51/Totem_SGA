package br.com.jefferson.totemsga.model;

import com.google.gson.annotations.SerializedName;

public class SenhaFila {
    public int id;
    public Senha senha;
    public Servico servico;
    public Cliente cliente;
    
    @SerializedName("dataChegada")
    public String dataChegada;
    
    public String status;
    public Prioridade prioridade;
    public String hash;

    public static class Senha {
        public String sigla;
        public int numero;
        public String format;
    }

    public static class Servico {
        public int id;
        public String nome;
    }

    public static class Cliente {
        public int id;
        public String nome;
        public String documento;
    }
}
