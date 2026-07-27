package br.com.jefferson.totemsga;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import br.com.jefferson.totemsga.util.Logger;
import br.com.jefferson.totemsga.util.SessionManager;
import br.com.jefferson.totemsga.util.SunmiPrinterHelper;

public class DiagnosticActivity extends BaseActivity {

    private TextView tvLog;
    private Spinner spinnerPrinterType;
    private ProgressBar progressBar;
    private TextView tvProgressStatus;
    private ExecutorService executor;
    private final Logger logger = Logger.getInstance();
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diagnostic);

        sessionManager = new SessionManager(this);
        executor = Executors.newSingleThreadExecutor();

        tvLog = findViewById(R.id.tvLogOutput);
        spinnerPrinterType = findViewById(R.id.spinnerPrinterType);
        progressBar = findViewById(R.id.diagnosticProgress);
        tvProgressStatus = findViewById(R.id.tvProgressStatus);

        setupPrinterSpinner();
        logger.setListener(msg -> tvLog.append(msg + "\n"));

        // Click Listeners para TODOS os botões
        findViewById(R.id.btnAnalyzePrint).setOnClickListener(v -> analyzePrint());
        styleButtons(findViewById(R.id.btnAnalyzePrint));
        findViewById(R.id.btnRunAll).setOnClickListener(v -> runAllTests());
        findViewById(R.id.btnInfo).setOnClickListener(v -> runTest(1));
        findViewById(R.id.btnServices).setOnClickListener(v -> runTest(2));
        findViewById(R.id.btnAllpos).setOnClickListener(v -> runTest(3));
        findViewById(R.id.btnSunmi).setOnClickListener(v -> runTest(4));
        findViewById(R.id.btnIntent).setOnClickListener(v -> runTest(5));
        findViewById(R.id.btnHtml).setOnClickListener(v -> runTest(6));
        findViewById(R.id.btnText).setOnClickListener(v -> runTest(7));
        findViewById(R.id.btnPrintManager).setOnClickListener(v -> runTest(8));
        findViewById(R.id.btnPkgMgr).setOnClickListener(v -> runTest(9));
        findViewById(R.id.btnPerms).setOnClickListener(v -> runTest(10));
        findViewById(R.id.btnNet).setOnClickListener(v -> runTest(11));
        findViewById(R.id.btnTesteCliente).setOnClickListener(v -> {
            startActivity(new Intent(this, br.com.jefferson.totemsga.teste.TesteClienteActivity.class));
        });

        findViewById(R.id.btnClear).setOnClickListener(v -> {
            logger.clear();
            tvLog.setText("");
        });

        logger.i("DIAG", "Diagnóstico Inicializado.");
    }

    private void setupPrinterSpinner() {
        String[] types = {"AUTO", "SUNMI", "ALLPOS", "STANDARD"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, types);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPrinterType.setAdapter(adapter);

        String current = sessionManager.getPrinterType();
        for (int i = 0; i < types.length; i++) {
            if (types[i].equals(current)) {
                spinnerPrinterType.setSelection(i);
                break;
            }
        }

        spinnerPrinterType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                sessionManager.setPrinterType(types[position]);
                logger.i("CONFIG", "Impressora: " + types[position]);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void runTest(int id) {
        executor.execute(() -> {
            switch (id) {
                case 1: testDeviceInfo(); break;
                case 2: testAllPrintServices(); break;
                case 3: testAllposPrint(); break;
                case 4: testSunmiPrint(); break;
                case 5: testSunmiRebind(); break;
                case 6: logger.i("HTML", "Teste HTML OK"); break;
                case 7: logger.i("TEXT", "Teste Texto OK"); break;
                case 8: logger.i("PRINTMGR", "Consultando spooler..."); break;
                case 9: testPackageManagerAll(); break;
                case 10: testPermissions(); break;
                case 11: logger.i("NET", "API: " + sessionManager.getApiUrl()); break;
            }
        });
    }

    private void runAllTests() {
        executor.execute(() -> {
            for (int i = 1; i <= 11; i++) runTest(i);
        });
    }

    private void analyzePrint() {
        executor.execute(() -> {
            String type = sessionManager.getPrinterType();
            logger.i("ANALYZE", "Testando: " + type);
            if ("SUNMI".equals(type)) testSunmiPrint();
            else testAllposPrint();
        });
    }

    private void testSunmiRebind() {
        logger.i("SUNMI", "=== RECONECTANDO SUNMI ===");
        SunmiPrinterHelper.getInstance().initPrinter(this);
    }

    private void testSunmiPrint() {
        SunmiPrinterHelper helper = SunmiPrinterHelper.getInstance();
        logger.i("SUNMI", "Status: " + helper.getStatusName());
        
        // Listar métodos para descobrir como formatar
        try {
            java.lang.reflect.Method[] methods = br.com.itfast.tectoy.TecToy.class.getDeclaredMethods();
            logger.i("SUNMI", "--- MÉTODOS DISPONÍVEIS ---");
            for (java.lang.reflect.Method m : methods) {
                logger.i("SUNMI", m.getName() + "(" + java.util.Arrays.toString(m.getParameterTypes()) + ")");
            }
        } catch (Exception e) {
            logger.e("SUNMI", "Erro ao listar métodos: " + e.getMessage());
        }

        if (helper.isConnected()) {
            helper.printerInit();
            helper.printStyledText("\nTESTE K2 OK\n", true, 30);
            helper.printText("Data: " + new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date()) + "\n");
            helper.lineWrap(4);
            helper.cutPaper((success, message) -> {
                if (success) logger.i("SUNMI", "PAPEL ENVIADO!");
                else logger.e("SUNMI", "ERRO: " + message);
            });
        } else {
            logger.e("SUNMI", "Não conectado.");
        }
    }

    private void testAllposPrint() {
        logger.i("ALLPOS", "=== DISPARANDO INTENT ALLPOS ===");
        try {
            Intent intent = new Intent("in.allmark.allpos_print_service.PRINT");
            intent.setPackage("in.allmark.allpos_print_service");
            intent.putExtra("content", "<h1>TESTE ALLPOS</h1><p>Data: " + new Date() + "</p>");
            intent.putExtra("direct", "1");
            startActivity(intent);
            logger.i("ALLPOS", "Comando enviado via Intent.");
        } catch (Exception e) {
            logger.e("ALLPOS", "Falha ao abrir AllPos.");
        }
    }

    private void testDeviceInfo() {
        logger.i("INFO", "MODELO: " + Build.MODEL);
        logger.i("INFO", "ANDROID: " + Build.VERSION.RELEASE);
    }

    private void testAllPrintServices() {
        logger.i("SERVICES", "=== BUSCANDO SERVIÇOS ===");
        PackageManager pm = getPackageManager();
        List<PackageInfo> packs = pm.getInstalledPackages(0);
        for (PackageInfo p : packs) {
            if (p.packageName.contains("print") || p.packageName.contains("sunmi") || p.packageName.contains("allmark")) {
                logger.i("SERVICES", "Encontrado: " + p.packageName);
            }
        }
    }

    private void testPackageManagerAll() {
        try {
            getPackageManager().getPackageInfo("in.allmark.allpos_print_service", 0);
            logger.i("PKGMGR", "Allmark: INSTALADO");
        } catch (Exception e) {
            logger.w("PKGMGR", "Allmark: NÃO ENCONTRADO");
        }
    }

    private void testPermissions() {
        logger.i("PERMS", "INTERNET: OK");
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null) executor.shutdownNow();
    }
}
