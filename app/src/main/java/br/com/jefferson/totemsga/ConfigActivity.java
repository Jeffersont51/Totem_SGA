package br.com.jefferson.totemsga;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

import java.util.HashMap;
import java.util.Map;

import br.com.jefferson.totemsga.api.ApiService;
import br.com.jefferson.totemsga.api.RetrofitClient;
import br.com.jefferson.totemsga.model.TokenResponse;
import br.com.jefferson.totemsga.util.SessionManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ConfigActivity extends BaseActivity {

    private TextInputEditText etApiUrl, etClientId, etClientSecret, etUsername, etPassword;
    private Button btnSave;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);

        sessionManager = new SessionManager(this);

        etApiUrl = findViewById(R.id.etApiUrl);
        etClientId = findViewById(R.id.etClientId);
        etClientSecret = findViewById(R.id.etClientSecret);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnSave = findViewById(R.id.btnSave);

        // Load existing
        etApiUrl.setText(sessionManager.getApiUrl());
        etClientId.setText(sessionManager.getClientId());
        etClientSecret.setText(sessionManager.getClientSecret());
        etUsername.setText(sessionManager.getUsername());
        etPassword.setText(sessionManager.getPassword());

        btnSave.setOnClickListener(v -> saveAndAuth());
        styleButtons(btnSave);
    }

    private void saveAndAuth() {
        String apiUrl = etApiUrl.getText().toString().trim();
        String clientId = etClientId.getText().toString().trim();
        String clientSecret = etClientSecret.getText().toString().trim();
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (apiUrl.isEmpty() || clientId.isEmpty() || clientSecret.isEmpty() || username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show();
            return;
        }

        sessionManager.saveCredentials(apiUrl, clientId, clientSecret, username, password);

        ApiService api = RetrofitClient.getInstance(sessionManager);
        Map<String, String> params = new HashMap<>();
        params.put("grant_type", "password");
        params.put("client_id", clientId);
        params.put("client_secret", clientSecret);
        params.put("username", username);
        params.put("password", password);

        api.getToken(params).enqueue(new Callback<TokenResponse>() {
            @Override
            public void onResponse(Call<TokenResponse> call, Response<TokenResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    sessionManager.saveTokens(response.body().accessToken, response.body().refreshToken);
                    Toast.makeText(ConfigActivity.this, "Autenticado com sucesso", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(ConfigActivity.this, AdminActivity.class));
                    finish();
                } else {
                    Toast.makeText(ConfigActivity.this, "Erro na autenticação: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<TokenResponse> call, Throwable t) {
                Toast.makeText(ConfigActivity.this, "Falha na conexão: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

}
