package br.com.jefferson.totemsga.model;

public class TicketRequest {
    public int unidade;
    public int servico;
    public int prioridade;
    public Cliente cliente;

    public static class Cliente {
        public String nome;
        public String documento;

        public Cliente(String nome, String documento) {
            this.nome = nome;
            this.documento = documento;
        }
    }
}
