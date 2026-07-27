package br.com.jefferson.totemsga.model;

import java.util.List;

public class MonitorResponse {
    public boolean success;
    public String sessionStatus;
    public long time;
    public List<MonitorData> data;

    public static class MonitorData {
        public ServicoUnidade servicoUnidade;
        public List<SenhaFila> fila;
    }
}
