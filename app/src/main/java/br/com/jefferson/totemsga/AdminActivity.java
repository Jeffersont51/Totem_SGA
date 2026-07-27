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

        styleButtons(btnSave);
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
        int selectedUnidadeId = -1;
        String selectedUnidadeNome = "";
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
