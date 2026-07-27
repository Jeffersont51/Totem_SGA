package br.com.jefferson.totemsga.model;

public class TicketResponse {
    public int id;
    public Senha senha;
    public String hash;
    public Servico servico;
    public Prioridade prioridade;
    public Cliente cliente;
    public String mensagem;

    public static class Senha {
        public String sigla;
        public int numero;
        public String format;
    }

    public static class Servico {
        public int id;
        public String nome;
        public String descricao;
    }

    public static class Prioridade {
        public int id;
        public String nome;
        public int peso;
    }

    public static class Cliente {
        public int id;
        public String nome;
        public String documento;
    }
}
