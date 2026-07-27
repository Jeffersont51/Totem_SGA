package br.com.jefferson.totemsga.model;

public class Unidade {
    public int id;
    public String nome;
    public String descricao;
    public boolean ativo;
    public ImpressaoConfig impressao;

    public static class ImpressaoConfig {
        public String cabecalho;
        public String rodape;
        public boolean exibirData;
        public boolean exibirPrioridade;
        public boolean exibirNomeUnidade;
        public boolean exibirNomeServico;
        public boolean exibirMensagemServico;
    }
}
