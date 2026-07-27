package br.com.jefferson.totemsga.util;

import android.os.Handler;
import android.os.Looper;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;

/**
 * Logger Singleton para diagnóstico do Totem SGA.
 * Mantém um buffer FIFO de 1000 entradas para visualização em tempo real e exportação.
 */
public class Logger {
    private static Logger instance;
    private final LinkedList<String> logs = new LinkedList<>();
    private static final int MAX_LOGS = 1000;
    private String lastHtml = null;
    private LogListener listener;
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    public interface LogListener {
        void onLogAdded(String message);
    }

    private Logger() {}

    public static synchronized Logger getInstance() {
        if (instance == null) {
            instance = new Logger();
        }
        return instance;
    }

    public void setListener(LogListener listener) {
        this.listener = listener;
    }

    private void addLog(String level, String tag, String msg, Throwable tr) {
        String time = sdf.format(new Date());
        StringBuilder sb = new StringBuilder()
                .append(time).append(" [").append(level).append("] ")
                .append(tag).append(": ").append(msg);
        
        if (tr != null) {
            sb.append("\nException: ").append(tr.getMessage());
            sb.append("\nStackTrace: ").append(android.util.Log.getStackTraceString(tr));
        }

        final String logLine = sb.toString();
        
        synchronized (logs) {
            if (logs.size() >= MAX_LOGS) {
                logs.removeFirst();
            }
            logs.add(logLine);
        }

        // Notifica o listener (Geralmente a DiagnosticActivity) na UI Thread
        if (listener != null) {
            new Handler(Looper.getMainLooper()).post(() -> listener.onLogAdded(logLine));
        }
    }

    public void i(String tag, String msg) { addLog("INFO", tag, msg, null); }
    public void e(String tag, String msg, Throwable tr) { addLog("ERROR", tag, msg, tr); }
    public void e(String tag, String msg) { addLog("ERROR", tag, msg, null); }
    public void w(String tag, String msg) { addLog("WARN", tag, msg, null); }
    public void d(String tag, String msg) { addLog("DEBUG", tag, msg, null); }

    public void setLastHtml(String html) { this.lastHtml = html; }
    public String getLastHtml() { return lastHtml; }

    public String getAllLogs() {
        StringBuilder sb = new StringBuilder();
        synchronized (logs) {
            for (String log : logs) {
                sb.append(log).append("\n");
            }
        }
        return sb.toString();
    }

    public void clear() {
        synchronized (logs) {
            logs.clear();
        }
    }
}
