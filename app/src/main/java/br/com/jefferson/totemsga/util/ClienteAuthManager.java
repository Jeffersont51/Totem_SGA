package br.com.jefferson.totemsga.util;

import android.util.Log;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import br.com.jefferson.totemsga.api.ApiService;
import br.com.jefferson.totemsga.api.RetrofitClient;
import br.com.jefferson.totemsga.model.Cliente;
import br.com.jefferson.totemsga.model.ClienteResponse;
import br.com.jefferson.totemsga.model.AgendamentoResponse;
import br.com.jefferson.totemsga.model.TicketResponse;
import br.com.jefferson.totemsga.model.TicketTriageResponse;
import okhttp3.ResponseBody;
import retrofit2.Response;

public class ClienteAuthManager {
    private static final String TAG = "ClienteAuthManager";
    private static ClienteAuthManager instance;
    private final SessionManager sessionManager;
    private boolean isLoggedIn = false;
    
    private String testUrl;
    private String testUser;
    private String testPass;
    private String lastLoginDiagnostic = "";

    private ClienteAuthManager(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    public static synchronized ClienteAuthManager getInstance(SessionManager sessionManager) {
        if (instance == null) {
            instance = new ClienteAuthManager(sessionManager);
        }
        return instance;
    }

    public interface AuthCallback {
        void onFound(Cliente cliente);
        void onNotFound();
        void onError(String message);
    }

    public interface AgendamentoCallback {
        void onFound(java.util.List<br.com.jefferson.totemsga.model.Agendamento> agendamentos);
        void onNotFound();
        void onError(String message);
    }

    public interface TicketCallback {
        void onSuccess(br.com.jefferson.totemsga.model.TicketResponse response);
        void onError(String message);
    }

    public interface FilaCallback {
        void onFound(br.com.jefferson.totemsga.model.SenhasResponse response);
        void onNotFound();
        void onError(String message);
    }

    public interface MonitorCallback {
        void onFound(br.com.jefferson.totemsga.model.MonitorResponse response);
        void onNotFound();
        void onError(String message);
    }

    public void setTestOverrides(String url, String user, String pass) {
        this.testUrl = url;
        this.testUser = user;
        this.testPass = pass;
        this.isLoggedIn = false; // Force re-login if overrides change
        RetrofitClient.clearSession();
    }

    public void clearTestOverrides() {
        this.testUrl = null;
        this.testUser = null;
        this.testPass = null;
        this.isLoggedIn = false;
        RetrofitClient.clearSession();
    }

    public synchronized void ensureLoggedIn() throws Exception {
        // Auditoria: se a flag está true mas o servidor retornou HTML (detetado via flag externa ou limpeza manual)
        if (!isLoggedIn) {
            Log.d(TAG, "🔐 [Sessão] Iniciando login automático...");
            if (!realizarLogin()) {
                Log.e(TAG, "❌ [ensureLoggedIn] " + lastLoginDiagnostic);
                throw new Exception("Não foi possível conectar ao sistema. Tente novamente.");
            }
        } else {
            Log.d(TAG, "✅ [Sessão] Flag isLoggedIn já está ativa.");
        }
    }

    public boolean isLoggedIn() {
        return isLoggedIn;
    }

    public void buscarCliente(String documento, AuthCallback callback) {
        new Thread(() -> {
            try {
                if (!isLoggedIn) {
                    if (!realizarLogin()) {
                        callback.onError("Falha na autenticação com NovoSGA");
                        return;
                    }
                }

                ApiService api = RetrofitClient.getAutocompleteInstance(sessionManager, testUrl);
                if (api == null) {
                    callback.onError("URL de Autocomplete não configurada");
                    return;
                }

                Response<ClienteResponse> response = api.buscarCliente(documento).execute();
                
                // Trata redirecionamento para login (Sessão expirada)
                if (response.code() == 302 || (response.body() != null && !response.body().success && "expired".equals(response.body().sessionStatus))) {
                    isLoggedIn = false;
                    RetrofitClient.clearSession();
                    if (realizarLogin()) {
                        response = api.buscarCliente(documento).execute();
                    } else {
                        callback.onError("Sessão expirada e falha ao relogar");
                        return;
                    }
                }

                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    if (response.body().data != null && !response.body().data.isEmpty()) {
                        callback.onFound(response.body().data.get(0));
                    } else {
                        callback.onNotFound();
                    }
                } else {
                    callback.onNotFound();
                }

            } catch (Exception e) {
                Log.e(TAG, "Erro ao buscar cliente", e);
                callback.onError("Erro de conexão: " + e.getMessage());
            }
        }).start();
    }

    public void buscarAgendamentosPorServico(int servicoId, AgendamentoCallback callback) {
        try {
            if (!isLoggedIn) {
                if (!realizarLogin()) {
                    callback.onError("Falha na autenticação com NovoSGA");
                    return;
                }
            }

            ApiService api = RetrofitClient.getAutocompleteInstance(sessionManager, testUrl);
            if (api == null) {
                callback.onError("URL não configurada");
                return;
            }

            retrofit2.Response<AgendamentoResponse> response = api.buscarAgendamentosPorServico(servicoId).execute();

            if (response.code() == 302 || (response.body() != null && !response.body().success && "expired".equals(response.body().sessionStatus))) {
                isLoggedIn = false;
                RetrofitClient.clearSession();
                if (realizarLogin()) {
                    response = api.buscarAgendamentosPorServico(servicoId).execute();
                } else {
                    callback.onError("Sessão expirada");
                    return;
                }
            }

            if (response.isSuccessful() && response.body() != null && response.body().success) {
                if (response.body().data != null && !response.body().data.isEmpty()) {
                    callback.onFound(response.body().data);
                } else {
                    callback.onNotFound();
                }
            } else {
                callback.onNotFound();
            }

        } catch (Exception e) {
            Log.e(TAG, "Erro ao buscar agendamentos do serviço " + servicoId, e);
            callback.onError(e.getMessage());
        }
    }

    public void confirmarAgendamento(int agendamentoId, TicketCallback callback) {
        new Thread(() -> {
            try {
                if (!isLoggedIn) {
                    if (!realizarLogin()) {
                        callback.onError("Falha na autenticação com NovoSGA");
                        return;
                    }
                }

                ApiService api = RetrofitClient.getAutocompleteInstance(sessionManager, testUrl);
                if (api == null) {
                    callback.onError("URL não configurada");
                    return;
                }

                retrofit2.Response<TicketTriageResponse> response = api.confirmarAgendamento(agendamentoId).execute();

                if (response.code() == 302 || (response.body() != null && response.code() == 401)) {
                    isLoggedIn = false;
                    RetrofitClient.clearSession();
                    if (realizarLogin()) {
                        response = api.confirmarAgendamento(agendamentoId).execute();
                    } else {
                        callback.onError("Sessão expirada");
                        return;
                    }
                }

                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    callback.onSuccess(response.body().data);
                } else {
                    callback.onError("Falha ao confirmar agendamento");
                }

            } catch (Exception e) {
                Log.e(TAG, "Erro ao confirmar agendamento", e);
                callback.onError(e.getMessage());
            }
        }).start();
    }

    public void buscarSenhasFila(String ids, FilaCallback callback) {
        new Thread(() -> {
            try {
                if (!isLoggedIn) {
                    if (!realizarLogin()) {
                        callback.onError("Falha na autenticação com NovoSGA");
                        return;
                    }
                }

                ApiService api = RetrofitClient.getAutocompleteInstance(sessionManager, testUrl);
                if (api == null) {
                    callback.onError("URL não configurada");
                    return;
                }

                retrofit2.Response<br.com.jefferson.totemsga.model.SenhasResponse> response = api.buscarSenhasFila(ids).execute();

                if (response.code() == 302 || (response.body() != null && !response.body().success && "expired".equals(response.body().sessionStatus))) {
                    isLoggedIn = false;
                    RetrofitClient.clearSession();
                    if (realizarLogin()) {
                        response = api.buscarSenhasFila(ids).execute();
                    } else {
                        callback.onError("Sessão expirada e falha ao relogar");
                        return;
                    }
                }

                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    if (response.body().data != null && response.body().data.senhas != null && !response.body().data.senhas.isEmpty()) {
                        callback.onFound(response.body());
                    } else {
                        callback.onNotFound();
                    }
                } else {
                    callback.onNotFound();
                }

            } catch (Exception e) {
                Log.e(TAG, "Erro ao buscar fila", e);
                callback.onError("Erro de conexão: " + e.getMessage());
            }
        }).start();
    }

    public void buscarSenhasMonitor(String ids, MonitorCallback callback) {
        buscarSenhasMonitorInternal(ids, callback, false);
    }

    private void buscarSenhasMonitorInternal(String ids, MonitorCallback callback, boolean isRetry) {
        new Thread(() -> {
            try {
                if (!isLoggedIn) {
                    if (!realizarLogin()) {
                        Log.e(TAG, "❌ [buscarSenhasMonitor] " + lastLoginDiagnostic);
                        callback.onError("Não foi possível buscar a senha agora. Tente novamente.");
                        return;
                    }
                }

                ApiService api = RetrofitClient.getAutocompleteInstance(sessionManager, testUrl);
                if (api == null) {
                    callback.onError("URL não configurada");
                    return;
                }

                String hostMonitor = android.net.Uri.parse(sessionManager.getApiUrl()).getHost();
                Log.d("DEBUG_SGA", "🍪 [Monitor] Cookies antes da consulta: " + RetrofitClient.debugCookies(hostMonitor));

                // Usando chamada RAW para capturar o corpo bruto em caso de erro de parse (HTML de login)
                retrofit2.Response<okhttp3.ResponseBody> response = api.buscarSenhasMonitorRaw(ids).execute();

                String rawBody = "";
                if (response.body() != null) {
                    rawBody = response.body().string();
                } else if (response.errorBody() != null) {
                    rawBody = response.errorBody().string();
                }

                Log.d("DEBUG_SGA", "📡 Resposta Monitor Code: " + response.code());
                
                // Diagnóstico Visual para o Totem
                boolean isJson = rawBody.trim().startsWith("{") || rawBody.trim().startsWith("[");
                if (!isJson || response.code() != 200) {
                    Log.w("DEBUG_SGA", "⚠️ Resposta não é JSON ou erro. Verificando necessidade de re-login...");

                    if (!isRetry && (rawBody.contains("<!DOCTYPE") || rawBody.contains("<html"))) {
                        Log.w("DEBUG_SGA", "🛑 HTML detectado onde deveria ser JSON. Forçando recuperação de sessão...");
                        isLoggedIn = false;
                        RetrofitClient.clearSession();
                        if (realizarLogin()) {
                            Log.d("DEBUG_SGA", "🔄 Sessão recuperada. Repetindo consulta...");
                            buscarSenhasMonitorInternal(ids, callback, true);
                            return;
                        }
                    }

                    String tipo = (rawBody.contains("<!DOCTYPE") || rawBody.contains("<html")) ? "HTML" : "DESCONHECIDO";
                    String preview = rawBody.length() > 200 ? rawBody.substring(0, 200) + "..." : rawBody;
                    Log.e("DEBUG_SGA", "❌ [Monitor] Status: " + response.code() + " | Tipo: " + tipo
                            + " | Cookies: " + RetrofitClient.debugCookies(hostMonitor)
                            + " | Último login: " + lastLoginDiagnostic
                            + " | Resposta: " + preview);
                    callback.onError("Não foi possível buscar a senha agora. Tente novamente.");
                    return;
                }

                // Se for JSON, fazemos o parse manual
                br.com.jefferson.totemsga.model.MonitorResponse monitorResponse = 
                        new com.google.gson.Gson().fromJson(rawBody, br.com.jefferson.totemsga.model.MonitorResponse.class);

                if (monitorResponse != null && monitorResponse.success) {
                    if (monitorResponse.data != null && !monitorResponse.data.isEmpty()) {
                        callback.onFound(monitorResponse);
                    } else {
                        callback.onNotFound();
                    }
                } else if (!isRetry && monitorResponse != null && "expired".equals(monitorResponse.sessionStatus)) {
                    Log.w("DEBUG_SGA", "⚠️ Sessão expirada no Monitor. Tentando relogar...");
                    isLoggedIn = false;
                    RetrofitClient.clearSession();
                    if (realizarLogin()) {
                        buscarSenhasMonitorInternal(ids, callback, true); // Tenta novamente após relogar
                    } else {
                        callback.onError("Sessão expirada e falha ao relogar");
                    }
                } else {
                    callback.onNotFound();
                }

            } catch (Exception e) {
                Log.e(TAG, "Erro ao buscar monitor", e);
                callback.onError("Erro de conexão: " + e.getMessage());
            }
        }).start();
    }

    private synchronized boolean realizarLogin() {
        try {
            Log.d("DEBUG_SGA", "🔑 [Login] Passo 1: Abrindo página de login para extrair CSRF...");
            ApiService api = RetrofitClient.getAutocompleteInstance(sessionManager, testUrl);
            if (api == null) {
                lastLoginDiagnostic = "URL de Autocomplete não configurada";
                Log.e(TAG, "❌ " + lastLoginDiagnostic);
                return false;
            }

            // 1. Get Login Page to extract CSRF
            Response<ResponseBody> loginPageResponse = api.getLoginPage().execute();
            if (!loginPageResponse.isSuccessful() || loginPageResponse.body() == null) {
                lastLoginDiagnostic = "Falha ao carregar página de login. HTTP " + loginPageResponse.code();
                Log.e(TAG, "❌ " + lastLoginDiagnostic);
                return false;
            }

            String host = loginPageResponse.raw().request().url().host();
            boolean setCookieNoGet = !loginPageResponse.headers().values("Set-Cookie").isEmpty();
            Log.d("DEBUG_SGA", "🍪 [Login] Set-Cookie no GET /login: " + setCookieNoGet
                    + " | Jar após GET: " + RetrofitClient.debugCookies(host));

            String html = loginPageResponse.body().string();
            String csrfToken = extractCsrfToken(html);
            if (csrfToken == null) {
                lastLoginDiagnostic = "CSRF Token não encontrado no HTML da página de login";
                Log.e(TAG, "❌ " + lastLoginDiagnostic);
                return false;
            }
            Log.d("DEBUG_SGA", "📝 [Login] Passo 2: CSRF capturado: " + csrfToken);

            // 2. Perform POST Login
            String user = (testUser != null) ? testUser : sessionManager.getUsername();
            String pass = (testPass != null) ? testPass : sessionManager.getPassword();

            Log.d("DEBUG_SGA", "📤 [Login] Passo 3: Enviando credenciais para o servidor...");
            Response<ResponseBody> authResponse = api.login(user, pass, csrfToken).execute();
            String authBody = authResponse.body() != null ? authResponse.body().string() : "";

            boolean setCookieNoPost = !authResponse.headers().values("Set-Cookie").isEmpty();
            String jarAposPost = RetrofitClient.debugCookies(host);
            Log.d("DEBUG_SGA", "🍪 [Login] Set-Cookie no POST /login: " + setCookieNoPost
                    + " | Jar após POST: " + jarAposPost);

            // O NovoSGA/Symfony devolve HTTP 200 com o próprio formulário de login
            // (não um erro HTTP) quando a autenticação é rejeitada. Checar apenas o
            // código de status não é suficiente - é preciso confirmar que a resposta
            // final não é novamente a tela de login.
            boolean aindaNaTelaDeLogin = authBody.contains("_csrf_token") && authBody.contains("type=\"password\"");

            if (!aindaNaTelaDeLogin && (authResponse.code() == 302 || authResponse.isSuccessful())) {
                Log.d("DEBUG_SGA", "✅ [Login] Passo 4: Autenticação realizada com sucesso!");
                lastLoginDiagnostic = "OK (Cookies: " + jarAposPost + ")";
                isLoggedIn = true;
                return true;
            }

            String preview = authBody.length() > 250 ? authBody.substring(0, 250) + "..." : authBody;
            lastLoginDiagnostic = "Login rejeitado pelo servidor. HTTP " + authResponse.code()
                    + (aindaNaTelaDeLogin ? " (formulário de login retornado - usuário/senha ou CSRF inválidos)" : "")
                    + "\nUsuário: " + user
                    + "\nSet-Cookie GET: " + setCookieNoGet + " | Set-Cookie POST: " + setCookieNoPost
                    + "\nCookies no jar: " + jarAposPost
                    + "\nResposta: " + preview;
            Log.e(TAG, "❌ " + lastLoginDiagnostic);

        } catch (IOException e) {
            lastLoginDiagnostic = "Erro de rede no login: " + e.getClass().getSimpleName() + " - " + e.getMessage();
            Log.e(TAG, "❌ " + lastLoginDiagnostic, e);
        }
        return false;
    }

    public String getLastLoginDiagnostic() {
        return lastLoginDiagnostic;
    }

    private String extractCsrfToken(String html) {
        // Padrão comum no NovoSGA / Symfony
        Pattern pattern = Pattern.compile("name=\"_csrf_token\"\\s+value=\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
    
    public void logout() {
        isLoggedIn = false;
        RetrofitClient.clearSession();
    }
}
