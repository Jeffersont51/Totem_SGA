package br.com.jefferson.totemsga.model;

import java.util.List;

public class SenhasResponse {
    public boolean success;
    public String sessionStatus;
    public Data data;

    public static class Data {
        public List<SenhaFila> senhas;
    }
}
