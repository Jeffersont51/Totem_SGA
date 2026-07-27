package br.com.jefferson.totemsga;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;

import br.com.jefferson.totemsga.model.Cliente;
import br.com.jefferson.totemsga.util.ClienteAuthManager;
import br.com.jefferson.totemsga.util.SessionManager;

public class TestAutocompleteActivity extends BaseActivity {

    private TextInputEditText etUrl, etUser, etPass, etDocument;
    private Button btnConsultar;
    private ProgressBar progressBar;
    private TextView tvResult;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_autocomplete);

        sessionManager = new SessionManager(this);

        etUrl = findViewById(R.id.etTestUrl);
        etUser = findViewById(R.id.etTestUser);
        etPass = findViewById(R.id.etTestPass);
        etDocument = findViewById(R.id.etTestDocument);
        btnConsultar = findViewById(R.id.btnConsultar);
        progressBar = findViewById(R.id.progressTest);
        tvResult = findViewById(R.id.tvTestResult);

        // Load configs
        etUrl.setText(sessionManager.getApiUrl());
        etUser.setText(sessionManager.getUsername());
        etPass.setText(sessionManager.getPassword());

        btnConsultar.setOnClickListener(v -> realizarTeste());
        styleButtons(btnConsultar);
    }

    private void realizarTeste() {
        String url = etUrl.getText().toString().trim();
        String user = etUser.getText().toString().trim();
        String pass = etPass.getText().toString().trim();
        String doc = etDocument.getText().toString().trim();

        if (url.isEmpty()) {
            Toast.makeText(this, "Informe a URL Base", Toast.LENGTH_SHORT).show();
            return;
        }

        if (doc.isEmpty()) {
            Toast.makeText(this, "Informe um documento para teste", Toast.LENGTH_SHORT).show();
            return;
        }

        btnConsultar.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        tvResult.setText("Iniciando consulta...");

        ClienteAuthManager manager = ClienteAuthManager.getInstance(sessionManager);
        manager.setTestOverrides(url, user, pass);
        
        manager.buscarCliente(doc, new ClienteAuthManager.AuthCallback() {
            @Override
            public void onFound(Cliente cliente) {
                runOnUiThread(() -> {
                    btnConsultar.setEnabled(true);
                    progressBar.setVisibility(View.GONE);
                    
                    StringBuilder sb = new StringBuilder();
                    sb.append("✅ CLIENTE ENCONTRADO\n\n");
                    sb.append("Nome: ").append(cliente.nome).append("\n");
                    sb.append("Documento: ").append(cliente.documento).append("\n");
                    sb.append("Email: ").append(cliente.email).append("\n");
                    sb.append("Telefone: ").append(cliente.telefone).append("\n");
                    
                    if (cliente.endereco != null) {
                        sb.append("\n--- Endereço ---\n");
                        sb.append("Logradouro: ").append(cliente.endereco.logradouro).append(", ").append(cliente.endereco.numero).append("\n");
                        sb.append("CEP: ").append(cliente.endereco.cep).append("\n");
                        sb.append("Cidade: ").append(cliente.endereco.cidade).append(" - ").append(cliente.endereco.estado).append("\n");
                    }
                    
                    tvResult.setText(sb.toString());
                    tvResult.setTextColor(android.graphics.Color.BLACK);
                });
            }

            @Override
            public void onNotFound() {
                runOnUiThread(() -> {
                    btnConsultar.setEnabled(true);
                    progressBar.setVisibility(View.GONE);
                    tvResult.setText("❌ Cliente não encontrado no NovoSGA.");
                    tvResult.setTextColor(android.graphics.Color.RED);
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    btnConsultar.setEnabled(true);
                    progressBar.setVisibility(View.GONE);
                    tvResult.setText("⚠️ ERRO: " + message);
                    tvResult.setTextColor(android.graphics.Color.RED);
                });
            }
        });
    }
}
