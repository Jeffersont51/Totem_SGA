package br.com.jefferson.totemsga.util;

import android.content.Context;
import android.util.Log;

import br.com.itfast.tectoy.Dispositivo;
import br.com.itfast.tectoy.StatusImpressora;
import br.com.itfast.tectoy.TecToy;

/**
 * Helper para gerenciar a impressora térmica Sunmi K2 usando a biblioteca IT4R (TecToy).
 * Utiliza comandos ESC/POS diretos via imprimir(byte[]) para controle de layout.
 */
public class SunmiPrinterHelper {
    private static final String TAG = "SunmiPrinterHelper";
    
    private static SunmiPrinterHelper instance;
    private TecToy tecToy;
    
    public static final int ALIGN_LEFT = 0;
    public static final int ALIGN_CENTER = 1;
    public static final int ALIGN_RIGHT = 2;

    public interface PrintCallback {
        void onResult(boolean success, String message);
    }

    private SunmiPrinterHelper() {}

    public static synchronized SunmiPrinterHelper getInstance() {
        if (instance == null) {
            instance = new SunmiPrinterHelper();
        }
        return instance;
    }

    public void initPrinter(Context context) {
        try {
            tecToy = new TecToy(Dispositivo.K2, context.getApplicationContext());
            Log.d(TAG, "Biblioteca IT4R TecToy inicializada.");
        } catch (Exception e) {
            Log.e(TAG, "Erro ao inicializar IT4R: " + e.getMessage());
        }
    }

    public void deinitPrinter(Context context) {
        tecToy = null;
    }

    public boolean isConnected() {
        return tecToy != null;
    }

    public void printerInit() {
        if (tecToy == null) return;
        // Comando ESC @ (Initialize printer)
        sendRawData(new byte[]{0x1B, 0x40});
    }

    public void setAlignment(int align) {
        if (tecToy == null) return;
        // ESC a n (0=left, 1=center, 2=right)
        sendRawData(new byte[]{0x1B, 0x61, (byte) align});
    }

    public void setFontSize(int size) {
        if (tecToy == null) return;
        // GS ! n 
        // 0x00 = Normal
        // 0x11 = Double width & height
        // 0x22 = Triple width & height
        byte b = 0x00;
        if (size == 1) b = 0x11;
        else if (size == 2) b = 0x22;
        sendRawData(new byte[]{0x1D, 0x21, b});
    }

    public void setBold(boolean bold) {
        if (tecToy == null) return;
        // ESC E n (1=bold, 0=normal)
        sendRawData(new byte[]{0x1B, 0x45, (byte) (bold ? 1 : 0)});
    }

    public void printText(String text) {
        if (tecToy == null) return;
        try {
            tecToy.imprimir(text);
        } catch (Exception e) {
            Log.e(TAG, "Erro printText: " + e.getMessage());
        }
    }

    public void sendRawData(byte[] data) {
        if (tecToy == null) return;
        try {
            tecToy.imprimir(data);
        } catch (Exception e) {
            Log.e(TAG, "Erro sendRawData: " + e.getMessage());
        }
    }

    public void printStyledText(String text, boolean isBold, float fontSize) {
        if (tecToy == null) return;
        try {
            setBold(isBold);
            setFontSize((int)fontSize);
            tecToy.imprimir(text);
            // Reset
            setBold(false);
            setFontSize(0);
        } catch (Exception e) {
            Log.e(TAG, "Erro styledText: " + e.getMessage());
        }
    }

    public void printQrCode(String data, int dotSize, int errorLevel) {
        if (tecToy == null) return;
        try {
            tecToy.imprimirQrCode(data, String.valueOf(dotSize), dotSize * 20);
        } catch (Exception e) {
            Log.e(TAG, "Erro QR: " + e.getMessage());
        }
    }

    public void lineWrap(int lines) {
        if (tecToy == null) return;
        try {
            StringBuilder sb = new StringBuilder();
            for(int i=0; i<lines; i++) sb.append("\n");
            tecToy.imprimir(sb.toString());
        } catch (Exception e) {}
    }

    public void cutPaper() {
        cutPaper(null);
    }

    public void cutPaper(PrintCallback callback) {
        if (tecToy == null) {
            if (callback != null) callback.onResult(false, "TecToy não inicializado");
            return;
        }
        try {
            tecToy.acionarGuilhotina();
            if (callback != null) callback.onResult(true, "Sucesso");
        } catch (Exception e) {
            if (callback != null) callback.onResult(false, e.getMessage());
        }
    }

    /** Imprime direto na Sunmi, sem consulta ao servidor. */
    public static final int ROUTE_NATIVE = 1;
    /** Sunmi é a impressora em uso, mas está sem papel/tampa aberta: avisar e não imprimir. */
    public static final int ROUTE_BLOCKED = 2;
    /** Sunmi indisponível: seguir pelos outros caminhos (AllPos / impressão do Android). */
    public static final int ROUTE_OTHER = 3;

    /**
     * Decide o caminho de impressão a partir do tipo configurado no Admin.
     * - "SUNMI": sempre nativo (o aviso de sem papel/tampa aparece junto).
     * - "AUTO": nativo se a Sunmi estiver OK; bloqueado se ela existir mas estiver
     *   sem papel/tampa aberta (antes caía no diálogo de impressão do Android na
     *   frente do cliente); demais casos seguem o fluxo antigo.
     */
    public int resolveRoute(String printerType) {
        if ("SUNMI".equals(printerType)) return ROUTE_NATIVE;
        if ("AUTO".equals(printerType) && isConnected()) {
            int status = getStatus();
            if (status == 1) return ROUTE_NATIVE;
            if (status == 4 || status == 6 || status == 7) return ROUTE_BLOCKED;
        }
        return ROUTE_OTHER;
    }

    /** Mensagem para o cliente quando a impressora não consegue imprimir; null se estiver tudo certo. */
    public String getProblemMessage() {
        int status = getStatus();
        if (status == 4) return "Impressora sem papel. Anote sua senha e avise um atendente.";
        if (status == 6) return "Impressora com a tampa aberta. Anote sua senha e avise um atendente.";
        if (status == 7) return "Impressora travada (papel preso). Anote sua senha e avise um atendente.";
        return null;
    }

    // Conferencia feita alguns segundos DEPOIS de mandar imprimir: papel preso
    // so aparece durante a impressao. Estados conhecidos tem mensagem propria;
    // qualquer outro estado de erro vira um aviso generico (so aviso, nao
    // bloqueia nada). Devolve null se a impressora estiver normal.
    public String getPostPrintProblem() {
        if (tecToy == null) return null;
        String name = getStatusName();
        if ("OK".equals(name) || "PREPARANDO_IMPRESSORA".equals(name) || "GUILHOTINA_RESTAURADA".equals(name)) {
            return null;
        }
        Logger.getInstance().e(TAG, "Impressora em estado " + name + " logo apos imprimir.");
        String known = getProblemMessage();
        if (known != null) return known;
        return "A impressão pode não ter saído. Se o papel não saiu, anote sua senha e avise um atendente.";
    }

    // Motivo para NAO emitir senha agora, ou null se pode emitir. So bloqueia
    // quando a Sunmi e a impressora em uso e informa claramente sem papel ou
    // tampa aberta; estado indefinido nao bloqueia (evita parar o totem por
    // alarme falso).
    public String getBlockingMessage(String printerType) {
        if (!isConnected()) return null;
        boolean sunmiInUse = "SUNMI".equals(printerType) || "AUTO".equals(printerType);
        if (!sunmiInUse) return null;
        int status = getStatus();
        if (status == 4) return "Impressora sem papel.\nAvise um atendente.";
        if (status == 6) return "Impressora com a tampa aberta.\nAvise um atendente.";
        if (status == 7) return "Impressora travada (papel preso).\nAvise um atendente.";
        return null;
    }

    public String getStatusName() {
        if (tecToy == null) return "DESCONECTADO";
        try {
            StatusImpressora status = tecToy.statusImpressora();
            return status.name();
        } catch (Exception e) {
            return "ERRO";
        }
    }

    public int getStatus() {
        if (tecToy == null) return -1;
        try {
            StatusImpressora s = tecToy.statusImpressora();
            if (s == StatusImpressora.OK) return 1;
            if (s == StatusImpressora.SEM_PAPEL) return 4;
            if (s == StatusImpressora.TAMPA_ABERTA) return 6;
            // Guilhotina travada: e como a impressora informa papel preso no corte
            if (s == StatusImpressora.ERRO_GUILHOTINA) return 7;
            return 0;
        } catch (Exception e) {
            return -1;
        }
    }
}
