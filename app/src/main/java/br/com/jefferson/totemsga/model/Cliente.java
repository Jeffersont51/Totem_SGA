package br.com.jefferson.totemsga.model;

import com.google.gson.annotations.SerializedName;

public class Cliente {
    public int id;
    public String nome;
    public String documento;
    public String email;
    public String telefone;
    public String genero;
    public String observacao;
    
    @SerializedName("dataNascimento")
    public String dataNascimento;
    
    public Endereco endereco;
}
