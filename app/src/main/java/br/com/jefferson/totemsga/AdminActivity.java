package br.com.jefferson.totemsga;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import br.com.jefferson.totemsga.api.ApiService;
import br.com.jefferson.totemsga.api.RetrofitClient;
import br.com.jefferson.totemsga.model.Departamento;
import br.com.jefferson.totemsga.model.ServicoUnidade;
import br.com.jefferson.totemsga.model.Unidade;
import br.com.jefferson.totemsga.util.SessionManager;
import br.com.jefferson.totemsga.util.SunmiPrinterHelper;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminActivity extends BaseActivity {

    private Spinner spinnerUnidade;
    private SwitchMaterial switchEnablePrint, switchEnableScreening;
    private TextInputEditText etAdminPass, etScreeningTimeout;
    private LinearLayout layoutHeaders;
    private Button btnSave, btnAddHeader, btnLayoutConfig, btnKioskMode, btnDiagnostic, btnAdsConfig, btnPrintLayout, btnAdminDevice, btnReopenConfig;
    private SessionManager sessionManager;
    private List<Unidade> unidadesList = new ArrayList<>();
    private final Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        sessionManager = new SessionManager(this);

        spinnerUnidade = findViewById(R.id.spinnerUnidade);
        switchEnablePrint = findViewById(R.id.switchEnablePrint);
        switchEnableScreening = findViewById(R.id.switchEnableScreening);
        etAdminPass = findViewById(R.id.etAdminPass);
        etScreeningTimeout = findViewById(R.id.etScreeningTimeout);
        layoutHeaders = findViewById(R.id.layoutHeaders);
        btnAddHeader = findViewById(R.id.btnAddHeader);
        btnLayoutConfig = findViewById(R.id.btnLayoutConfig);
        btnKioskMode = findViewById(R.id.btnKioskMode);
        btnDiagnostic = findViewById(R.id.btnDiagnostic);
        btnAdsConfig = findViewById(R.id.btnAdsConfig);
        btnPrintLayout = findViewById(R.id.btnPrintLayout);
        btnAdminDevice = findViewById(R.id.btnAdminDevice);
        btnReopenConfig = findViewById(R.id.btnReopenConfig);
        btnSave = findViewById(R.id.btnSaveAdmin);

        loadSettings();
        fetchUnidades();

        btnAddHeader.setOnClickListener(v -> addHeaderView("", ""));
        btnLayoutConfig.setOnClickListener(v -> {
            // Intent to LayoutConfigActivity
            startActivity(new Intent(this, LayoutConfigActivity.class));
        });
        btnKioskMode.setOnClickListener(v -> toggleKioskMode());
        btnDiagnostic.setOnClickListener(v -> {
            startActivity(new Intent(this, DiagnosticActivity.class));
        });
        btnAdsConfig.setOnClickListener(v -> {
            startActivity(new Intent(this, AdConfigActivity.class));
        });
        btnPrintLayout.setOnClickListener(v -> {
            startActivity(new Intent(this, PrintLayoutActivity.class));
        });
        btnAdminDevice.setOnClickListener(v -> requestAdminPermission());
        btnReopenConfig.setOnClickListener(v -> {
            Intent intent = new Intent(this, ConfigActivity.class);
            intent.putExtra("from_admin", true);
            startActivity(intent);
        });
        btnSave.setOnClickListener(v -> saveSettings());
        findViewById(R.id.btnUpdateApp).setOnClickListener(v -> showUpdateDialog());

        styleButtons(btnSave);

        showVersion();
        warnDefaultAdminPass();
        sessionManager.clearSavedUpdateCredentials();
    }

    // Mostra a versão instalada no botão de Diagnóstico, para saber qual APK está em cada totem
    private void showVersion() {
        try {
            String version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            btnDiagnostic.setText(btnDiagnostic.getText() + " (v" + version + ")");
        } catch (Exception e) {}
    }

    // ---------- Atualização do app pela pasta de rede ----------

    private void showUpdateDialog() {
        float density = getResources().getDisplayMetrics().density;
        int pad = (int) (20 * density);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(pad, pad / 2, pad, 0);

        final EditText etPath = new EditText(this);
        etPath.setHint("Pasta de rede (\\\\servidor\\pasta)");
        etPath.setSingleLine(true);
        etPath.setText(sessionManager.getUpdatePath());
        box.addView(etPath);

        final EditText etUser = new EditText(this);
        etUser.setHint("Usuário de rede (ex: ALVORADA\\usuario)");
        etUser.setSingleLine(true);

        box.addView(etUser);

        final EditText etPass = new EditText(this);
        etPass.setHint("Senha de rede");
        etPass.setSingleLine(true);
        etPass.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);

        box.addView(etPass);

        final android.widget.TextView tvStatus = new android.widget.TextView(this);
        tvStatus.setPadding(0, pad / 2, 0, 0);
        tvStatus.setText("Versão instalada: " + installedVersion());
        box.addView(tvStatus);

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Atualizar aplicativo")
                .setView(box)
                .setPositiveButton("Verificar", null)
                .setNegativeButton("Fechar", null)
                .create();

        dialog.setOnShowListener(d -> {
            final Button btn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            btn.setOnClickListener(v -> {
                String path = etPath.getText().toString().trim();
                String user = etUser.getText().toString().trim();
                String pass = etPass.getText().toString();
                sessionManager.saveUpdatePath(path);
                runUpdate(dialog, btn, tvStatus, path, user, pass);
            });
        });
        dialog.show();
    }

    private String installedVersion() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "?";
        }
    }

    /** 1º toque: procura a versão na pasta. 2º toque (se houver mais nova): baixa e instala. */
    private br.com.jefferson.totemsga.util.AppUpdater.Found pendingUpdate;

    private void runUpdate(AlertDialog dialog, Button btn, android.widget.TextView tvStatus, String path, String user, String pass) {
        final br.com.jefferson.totemsga.util.AppUpdater.Found toInstall = pendingUpdate;
        pendingUpdate = null;
        btn.setEnabled(false);
        tvStatus.setText(toInstall == null ? "Conectando à pasta de rede..." : "Baixando " + toInstall.fileName + "...");

        new Thread(() -> {
            try {
                if (toInstall == null) {
                    br.com.jefferson.totemsga.util.AppUpdater.Found found =
                            br.com.jefferson.totemsga.util.AppUpdater.findLatest(path, user, pass);
                    runOnUiThread(() -> {
                        if (isFinishing() || !dialog.isShowing()) return;
                        btn.setEnabled(true);
                        String installed = installedVersion();
                        if (found == null) {
                            tvStatus.setText("Nenhum APK encontrado na pasta.\nVersão instalada: " + installed);
                        } else if (br.com.jefferson.totemsga.util.AppUpdater.compareVersions(found.version, installed) > 0) {
                            pendingUpdate = found;
                            btn.setText("Baixar e instalar");
                            tvStatus.setText("Versão instalada: " + installed
                                    + "\nDisponível na pasta: " + found.version + " (" + found.fileName + ")");
                        } else {
                            tvStatus.setText("O aplicativo já está atualizado.\nVersão instalada: " + installed
                                    + "\nMais recente na pasta: " + found.version);
                        }
                    });
                    return;
                }

                java.io.File apk = br.com.jefferson.totemsga.util.AppUpdater.download(this, path, user, pass, toInstall,
                        percent -> runOnUiThread(() -> {
                            if (dialog.isShowing()) tvStatus.setText("Baixando " + toInstall.fileName + "... " + percent + "%");
                        }));

                String problem = br.com.jefferson.totemsga.util.AppUpdater.validate(this, apk);
                runOnUiThread(() -> {
                    if (isFinishing() || !dialog.isShowing()) return;
                    btn.setEnabled(true);
                    btn.setText("Verificar");
                    if (problem != null) {
                        tvStatus.setText(problem);
                        return;
                    }
                    tvStatus.setText("Download concluído. Confirme a instalação na tela do Android.");
                    launchInstaller(apk);
                });
            } catch (Throwable e) {
                // Throwable: inclui falha da biblioteca de rede no aparelho, que não pode derrubar o Admin
                br.com.jefferson.totemsga.util.Logger.getInstance().e("UPDATE", "Falha na atualização: " + e, null);
                final String msg = friendlyUpdateError(e);
                runOnUiThread(() -> {
                    if (isFinishing() || !dialog.isShowing()) return;
                    btn.setEnabled(true);
                    btn.setText("Verificar");
                    tvStatus.setText(msg);
                });
            }
        }).start();
    }

    private String friendlyUpdateError(Throwable e) {
        String raw = String.valueOf(e.getMessage());
        String all = (e.getClass().getSimpleName() + " " + raw).toUpperCase();
        if (all.contains("LOGON_FAILURE") || all.contains("ACCESS_DENIED") || all.contains("PASSWORD")
                || all.contains("ACCOUNT")) {
            return "Acesso negado à pasta. Confira o usuário e a senha de rede.";
        }
        if (all.contains("BAD_NETWORK_NAME") || all.contains("OBJECT_NAME_NOT_FOUND") || all.contains("OBJECT_PATH_NOT_FOUND")) {
            return "Pasta não encontrada no servidor. Confira o caminho.";
        }
        if (all.contains("UNKNOWNHOST") || all.contains("CONNECT") || all.contains("TIMEOUT") || all.contains("UNREACHABLE")) {
            return "O totem não conseguiu chegar ao servidor. Verifique a rede.";
        }
        return "Falha na atualização: " + e.getClass().getSimpleName() + " - " + raw;
    }

    private void launchInstaller(java.io.File apk) {
        try {
            // Em modo Kiosk (tela fixada) o Android bloqueia abrir o instalador
            try { stopLockTask(); } catch (Exception ignored) {}

            android.net.Uri uri = androidx.core.content.FileProvider.getUriForFile(
                    this, getPackageName() + ".provider", apk);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Não foi possível abrir o instalador: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void warnDefaultAdminPass() {
        if (!SessionManager.DEFAULT_ADMIN_PASS.equals(sessionManager.getAdminPass())) return;
        new AlertDialog.Builder(this)
                .setTitle("Senha padrão em uso")
                .setMessage("A senha de acesso ao Admin ainda é a senha padrão de fábrica. "
                        + "Troque no campo de senha do Admin e toque em Salvar.")
                .setPositiveButton("Entendi", null)
                .show();
    }

    private void requestAdminPermission() {
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName adminName = new ComponentName(this, br.com.jefferson.totemsga.receiver.AdminReceiver.class);
        if (!dpm.isAdminActive(adminName)) {
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminName);
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Necessário para o Modo Kiosk profissional sem avisos do sistema.");
            startActivity(intent);
        } else {
            Toast.makeText(this, "O aplicativo já é Administrador do Dispositivo", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadSettings() {
        switchEnablePrint.setChecked(sessionManager.isEnablePrint());
        switchEnableScreening.setChecked(sessionManager.isEnableScreening());
        etAdminPass.setText(sessionManager.getAdminPass());
        etScreeningTimeout.setText(String.valueOf(sessionManager.getScreeningTimeout()));

        updateKioskButtonText();

        String headersJson = sessionManager.getAutocompleteHeaders();
        if (!headersJson.isEmpty()) {
            Map<String, String> headers = gson.fromJson(headersJson, new TypeToken<Map<String, String>>(){}.getType());
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                addHeaderView(entry.getKey(), entry.getValue());
            }
        }
    }

    private void updateKioskButtonText() {
        btnKioskMode.setText(sessionManager.isKioskMode() ? "Sair do Modo Kiosk" : "Ativar Modo Kiosk");
    }

    private void toggleKioskMode() {
        boolean current = sessionManager.isKioskMode();
        sessionManager.setKioskMode(!current);
        updateKioskButtonText();
        Toast.makeText(this, "Modo Kiosk " + (!current ? "ativado" : "desativado"), Toast.LENGTH_SHORT).show();
    }

    private void addHeaderView(String key, String value) {
        View view = getLayoutInflater().inflate(R.layout.item_header, layoutHeaders, false);
        EditText etKey = view.findViewById(R.id.etHeaderKey);
        EditText etValue = view.findViewById(R.id.etHeaderValue);
        etKey.setText(key);
        etValue.setText(value);
        view.findViewById(R.id.btnRemoveHeader).setOnClickListener(v -> layoutHeaders.removeView(view));
        layoutHeaders.addView(view);
    }

    private void fetchUnidades() {
        ApiService api = RetrofitClient.getInstance(sessionManager);
        if (api == null) return;
        api.getUnidades().enqueue(new Callback<List<Unidade>>() {
            @Override
            public void onResponse(Call<List<Unidade>> call, Response<List<Unidade>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    unidadesList = response.body();
                    List<String> names = new ArrayList<>();
                    int selectedIndex = 0;
                    for (int i = 0; i < unidadesList.size(); i++) {
                        names.add(unidadesList.get(i).nome);
                        if (unidadesList.get(i).id == sessionManager.getUnidadeId()) selectedIndex = i;
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(AdminActivity.this, android.R.layout.simple_spinner_item, names);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerUnidade.setAdapter(adapter);
                    spinnerUnidade.setSelection(selectedIndex);
                }
            }
            @Override public void onFailure(Call<List<Unidade>> call, Throwable t) {}
        });
    }


    private void saveSettings() {
        // Se a lista de unidades não carregou (servidor fora, 401...), mantém a
        // unidade já salva. Antes gravava -1 e o totem passava a dar 404 nos serviços.
        int selectedUnidadeId = sessionManager.getUnidadeId();
        String selectedUnidadeNome = sessionManager.getUnidadeNome();
        if (spinnerUnidade.getSelectedItemPosition() >= 0 && !unidadesList.isEmpty()) {
            Unidade u = unidadesList.get(spinnerUnidade.getSelectedItemPosition());
            selectedUnidadeId = u.id;
            selectedUnidadeNome = u.nome;
        }

        Map<String, String> headersMap = new HashMap<>();
        for (int i = 0; i < layoutHeaders.getChildCount(); i++) {
            View v = layoutHeaders.getChildAt(i);
            String k = ((EditText)v.findViewById(R.id.etHeaderKey)).getText().toString().trim();
            String val = ((EditText)v.findViewById(R.id.etHeaderValue)).getText().toString().trim();
            if (!k.isEmpty()) headersMap.put(k, val);
        }

        sessionManager.saveAdminSettings(
                selectedUnidadeId,
                selectedUnidadeNome,
                sessionManager.isGroupByDept(),
                switchEnablePrint.isChecked(),
                switchEnableScreening.isChecked(),
                sessionManager.getLogoUrl(),
                sessionManager.getPrimaryColor(),
                sessionManager.getAutocompleteUrl(),
                gson.toJson(headersMap),
                sessionManager.getGridColumns(),
                Integer.parseInt(etScreeningTimeout.getText().toString()),
                sessionManager.getSelectedDepts(),
                sessionManager.getSelectedServices()
        );

        String newAdminPass = etAdminPass.getText().toString();
        if (!newAdminPass.isEmpty()) sessionManager.setAdminPass(newAdminPass);

        Toast.makeText(this, "Configurações salvas", Toast.LENGTH_SHORT).show();
        finish();
    }
}
