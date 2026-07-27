package br.com.jefferson.totemsga.model;

public class ServicoUnidade {
    public String sigla;
    public int peso;
    public Servico servico;
    public Departamento departamento;
    public boolean ativo;
    public String mensagem;

    public static class Servico {
        public int id;
        public String nome;
        public String descricao;
        public boolean ativo;

        // Feature Flags (transient/local state)
        public boolean hasFacial;
        public boolean hasTriagem;
        public boolean isTriagemObrigatoria;
        public boolean hasPrioridade;
        public boolean hasVisivel;
        public boolean hasNome;
    }
}
