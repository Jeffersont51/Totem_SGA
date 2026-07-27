package br.com.jefferson.totemsga.teste;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import br.com.jefferson.totemsga.R;
import br.com.jefferson.totemsga.util.SessionManager;

public class TesteClienteActivity extends AppCompatActivity {
    private static final String TAG = "TesteCliente";

    private EditText etDocumento;
    private TextView tvLogs;
    private ScrollView svLogs;
    private TesteApiService apiService;
    private SessionManager sessionManager;
    private boolean isLoggedIn = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teste_cliente);

        sessionManager = new SessionManager(this);
        etDocumento = findViewById(R.id.etTesteDocumento);
        tvLogs = findViewById(R.id.tvTesteLogs);
        svLogs = findViewById(R.id.svTesteLogs);
        Button btnConsultar = findViewById(R.id.btnTesteConsultar);

        btnConsultar.setOnClickListener(v -> {
            String doc = etDocumento.getText().toString();
            if (!doc.isEmpty()) {
                iniciarFluxo(doc);
            }
        });
    }

    private boolean setupRetrofit() {
        String baseUrl = sessionManager.getApiUrl();
        if (baseUrl == null || baseUrl.isEmpty()) {
            addLog("ERRO: URL da API não configurada no SessionManager!");
            return false;
        }

        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }

        addLog("Configurando Retrofit para: " + baseUrl);

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .cookieJar(new TesteSessionCookieJar())
                .connectTimeout(6, TimeUnit.SECONDS)
                .readTimeout(6, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .addInterceptor(logging)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .build();

        apiService = retrofit.create(TesteApiService.class);
        return true;
    }

    private void addLog(String message) {
        runOnUiThread(() -> {
            tvLogs.append("\n> " + message);
            svLogs.post(() -> svLogs.fullScroll(ScrollView.FOCUS_DOWN));
            Log.d(TAG, message);
        });
    }

    private void iniciarFluxo(String documento) {
        tvLogs.setText("Iniciando consulta...");
        
        if (apiService == null) {
            if (!setupRetrofit()) return;
        }

        if (isLoggedIn) {
            addLog("Sessão já ativa, consultando direto...");
            consultarCliente(documento, true);
        } else {
            addLog("Iniciando novo login...");
            executarLoginERetentar(documento);
        }
    }

    private void executarLoginERetentar(String documento) {
        String usuario = sessionManager.getUsername();
        String senha = sessionManager.getPassword();

        addLog("Buscando página de login para extrair CSRF...");
        apiService.getLoginPage().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String html = response.body().string();
                        String csrf = extrairCsrf(html);
                        if (csrf != null) {
                            addLog("Token capturado: " + csrf);
                            realizarLogin(usuario, senha, csrf, documento);
                        } else {
                            addLog("FALHA: Token CSRF não encontrado no HTML!");
                        }
                    } catch (IOException e) {
                        addLog("ERRO ao ler HTML: " + e.getMessage());
                    }
                } else {
                    addLog("ERRO ao buscar login: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                addLog("FALHA de rede (CSRF): " + t.getMessage());
            }
        });
    }

    private String extrairCsrf(String html) {
        Pattern p1 = Pattern.compile("name=[\"']_csrf_token[\"']\\s+value=[\"']([^\"']+)[\"']");
        Matcher m1 = p1.matcher(html);
        if (m1.find()) return m1.group(1);

        Pattern p2 = Pattern.compile("value=[\"']([^\"']+)[\"']\\s+name=[\"']_csrf_token[\"']");
        Matcher m2 = p2.matcher(html);
        if (m2.find()) return m2.group(1);

        return null;
    }

    private void realizarLogin(String user, String pass, String csrf, String doc) {
        addLog("Enviando login (POST /login)...");
        apiService.login(user, pass, csrf).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                addLog("Login status: " + response.code());
                String finalUrl = response.raw().request().url().toString();
                addLog("Redirecionado para: " + finalUrl);
                
                // Se o followRedirects nos levou pra longe do /login, assumimos sucesso inicial
                if (!finalUrl.contains("/login") || response.code() == 302) {
                    isLoggedIn = true;
                    consultarCliente(doc, false);
                } else {
                    addLog("FALHA: Login parece não ter funcionado (permaneceu no /login)");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                addLog("FALHA no login (POST): " + t.getMessage());
            }
        });
    }

    private void consultarCliente(String doc, boolean podeRetentar) {
        addLog("Buscando cliente (GET /novosga.triage/clientes?q=" + doc + ")...");
        apiService.buscarCliente(doc).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String body = response.body().string();
                        String finalUrl = response.raw().request().url().toString();

                        // Detecção de sessão expirada:
                        // 1. Redirecionamento para página de login (URL contém /login)
                        // 2. Resposta contém HTML em vez de JSON (começa com <)
                        if (finalUrl.contains("/login") || body.trim().startsWith("<")) {
                            addLog("Sessão expirada (redirect ou HTML detected).");
                            isLoggedIn = false;
                            if (podeRetentar) {
                                addLog("Refazendo login automático...");
                                executarLoginERetentar(doc);
                            } else {
                                addLog("FALHA: Sessão expirou logo após login.");
                            }
                            return;
                        }

                        addLog("Consulta status: " + response.code());
                        addLog("RESULTADO:\n" + body);
                        
                        // Checagem de dados vazios
                        if (body.contains("\"data\":[]")) {
                            addLog("INFO: Cliente não encontrado - preenchimento manual necessário.");
                        }

                    } catch (IOException e) {
                        addLog("ERRO ao ler resposta: " + e.getMessage());
                    }
                } else {
                    addLog("FALHA na consulta (Status " + response.code() + "). Tratando como não encontrado.");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                addLog("FALHA na rede (Consulta): " + t.getMessage() + ". Tratando como não encontrado.");
            }
        });
    }
}
