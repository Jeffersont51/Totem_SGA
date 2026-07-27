package br.com.jefferson.totemsga.model;

import java.io.Serializable;

public class LetreiroConfig implements Serializable {
    public boolean habilitado;
    public String mensagem = "Tire sua senha aqui!";
    public String posicao = "RODAPE"; // TOPO, CENTRO, RODAPE
    public String corFonte = "#FFFFFF";
    public String corFundo = "#88000000";
    public int tamanhoFonte = 24;
    public String estilo = "NORMAL"; // NORMAL, NEGRITO, ITALICO
    
    public String efeito = "DESLIZAR"; // DESLIZAR, PISCAR, ESTATICO
    public int velocidadeSegundos = 5; // segundos (1-30)
    public String direcao = "DIREITA_ESQUERDA";
}
