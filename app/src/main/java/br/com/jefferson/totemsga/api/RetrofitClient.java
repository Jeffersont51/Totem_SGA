package br.com.jefferson.totemsga.api;

import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import br.com.jefferson.totemsga.model.TokenResponse;
import br.com.jefferson.totemsga.util.Logger;
import br.com.jefferson.totemsga.util.SessionCookieJar;
import br.com.jefferson.totemsga.util.SessionManager;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static final String TAG = "AUTH";
    private static final String TOKEN_PATH = "api/token";
    // Limite de tentativas de re-autenticação por requisição (evita loop infinito de 401)
    private static final int MAX_AUTH_RETRIES = 2;

    private static ApiService apiService;
    private static String lastApiUrl;
    private static ApiService autocompleteService;
    private static String lastAutocompleteUrl;
    private static SessionCookieJar cookieJar = new SessionCookieJar();
    private static final Object AUTH_LOCK = new Object();

    public static synchronized ApiService getInstance(SessionManager sessionManager) {
        String baseUrl = sessionManager.getApiUrl();
        if (baseUrl == null || baseUrl.isEmpty()) return null;

        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }

        // Reaproveita o mesmo cliente (pool de conexões e threads) enquanto a URL não mudar
        if (apiService != null && baseUrl.equals(lastApiUrl)) {
            return apiService;
        }
        lastApiUrl = baseUrl;

        final String finalBaseUrl = baseUrl;

        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        applyUnsafeSsl(builder);

        builder.addInterceptor(newLogging())
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    // O endpoint de token não leva Bearer: enviar um token vencido ali
                    // faz o servidor recusar a própria renovação.
                    if (isTokenRequest(original)) {
                        return chain.proceed(original);
                    }
                    String token = sessionManager.getAccessToken();
                    if (token != null) {
                        Request request = original.newBuilder()
                                .header("Authorization", "Bearer " + token)
                                .build();
                        return chain.proceed(request);
                    }
                    return chain.proceed(original);
                })
                .authenticator((route, response) -> {
                    if (isTokenRequest(response.request())) return null;
                    if (responseCount(response) > MAX_AUTH_RETRIES) {
                        Logger.getInstance().e(TAG, "401 persistente mesmo após re-autenticar. Desistindo desta requisição.");
                        return null;
                    }

                    synchronized (AUTH_LOCK) {
                        String currentToken = sessionManager.getAccessToken();
                        String failedToken = response.request().header("Authorization");

                        // Se o token no sessionManager já mudou (por outra thread), tenta com o novo
                        if (failedToken != null && currentToken != null && !failedToken.equals("Bearer " + currentToken)) {
                            return response.request().newBuilder()
                                    .header("Authorization", "Bearer " + currentToken)
                                    .build();
                        }

                        String newToken = renewToken(sessionManager, finalBaseUrl);
                        if (newToken == null) return null;

                        return response.request().newBuilder()
                                .header("Authorization", "Bearer " + newToken)
                                .build();
                    }
                });

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(builder.build())
                .build();

        apiService = retrofit.create(ApiService.class);
        return apiService;
    }

    /**
     * Obtém um novo access token. Tenta primeiro o refresh_token; se o servidor
     * recusar (refresh vencido, revogado ou perdido numa queda de rede), faz login
     * completo de novo com o usuário e senha salvos na configuração.
     */
    private static String renewToken(SessionManager sessionManager, String baseUrl) {
        // Cliente limpo: sem interceptor de Bearer e sem authenticator
        OkHttpClient.Builder cleanBuilder = new OkHttpClient.Builder();
        applyUnsafeSsl(cleanBuilder);
        cleanBuilder.connectTimeout(10, TimeUnit.SECONDS);
        cleanBuilder.readTimeout(10, TimeUnit.SECONDS);

        ApiService authApi = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(cleanBuilder.build())
                .build()
                .create(ApiService.class);

        String refreshToken = sessionManager.getRefreshToken();
        if (refreshToken != null && !refreshToken.isEmpty()) {
            Map<String, String> params = new HashMap<>();
            params.put("grant_type", "refresh_token");
            params.put("client_id", sessionManager.getClientId());
            params.put("client_secret", sessionManager.getClientSecret());
            params.put("refresh_token", refreshToken);
            try {
                retrofit2.Response<TokenResponse> r = authApi.getToken(params).execute();
                if (r.isSuccessful() && r.body() != null && r.body().accessToken != null) {
                    sessionManager.saveTokens(r.body().accessToken, r.body().refreshToken);
                    Logger.getInstance().i(TAG, "Token renovado via refresh_token.");
                    return r.body().accessToken;
                }
                Logger.getInstance().w(TAG, "Refresh recusado pelo servidor (HTTP " + r.code() + "). Tentando login completo...");
            } catch (Exception e) {
                // Sem rede: o login completo também falharia. A próxima requisição tenta de novo.
                Logger.getInstance().e(TAG, "Falha de rede ao renovar token", e);
                return null;
            }
        }

        String username = sessionManager.getUsername();
        String password = sessionManager.getPassword();
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            Logger.getInstance().e(TAG, "Sem usuário/senha salvos para refazer o login.");
            return null;
        }

        Map<String, String> params = new HashMap<>();
        params.put("grant_type", "password");
        params.put("client_id", sessionManager.getClientId());
        params.put("client_secret", sessionManager.getClientSecret());
        params.put("username", username);
        params.put("password", password);
        try {
            retrofit2.Response<TokenResponse> r = authApi.getToken(params).execute();
            if (r.isSuccessful() && r.body() != null && r.body().accessToken != null) {
                sessionManager.saveTokens(r.body().accessToken, r.body().refreshToken);
                Logger.getInstance().i(TAG, "Login completo refeito com sucesso.");
                return r.body().accessToken;
            }
            Logger.getInstance().e(TAG, "Login completo recusado (HTTP " + r.code() + "). Verifique usuário, senha e Client ID/Secret.");
        } catch (Exception e) {
            Logger.getInstance().e(TAG, "Falha de rede no login completo", e);
        }
        return null;
    }

    private static boolean isTokenRequest(Request request) {
        return request.url().encodedPath().endsWith(TOKEN_PATH);
    }

    private static int responseCount(Response response) {
        int count = 1;
        while ((response = response.priorResponse()) != null) count++;
        return count;
    }

    private static HttpLoggingInterceptor newLogging() {
        // BASIC: registra só método/URL/código. O nível BODY gravava senha e tokens no log.
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BASIC);
        return logging;
    }

    // Habilita suporte a certificados auto-assinados e contextos inseguros (comum em redes locais)
    private static void applyUnsafeSsl(OkHttpClient.Builder builder) {
        try {
            final TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {}
                    @Override
                    public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {}
                    @Override
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new java.security.cert.X509Certificate[]{};
                    }
                }
            };

            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            builder.sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0]);
            builder.hostnameVerifier((hostname, session) -> true);
        } catch (Exception e) {}
    }

    public static ApiService getAutocompleteInstance(SessionManager sessionManager) {
        return getAutocompleteInstance(sessionManager, null);
    }

    public static synchronized ApiService getAutocompleteInstance(SessionManager sessionManager, String overrideUrl) {
        String baseUrl = (overrideUrl != null && !overrideUrl.isEmpty()) ? overrideUrl : sessionManager.getApiUrl();
        if (baseUrl == null || baseUrl.isEmpty()) return null;

        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }

        // Recreate if URL changed
        if (autocompleteService != null && baseUrl.equals(lastAutocompleteUrl)) {
            return autocompleteService;
        }

        lastAutocompleteUrl = baseUrl;

        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.connectTimeout(4, TimeUnit.SECONDS);
        builder.readTimeout(4, TimeUnit.SECONDS);
        builder.cookieJar(cookieJar);
        applyUnsafeSsl(builder);
        builder.addInterceptor(newLogging());

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(builder.build())
                .build();

        autocompleteService = retrofit.create(ApiService.class);
        return autocompleteService;
    }

    public static void clearSession() {
        cookieJar.clear();
    }

    public static String debugCookies(String host) {
        return cookieJar.debugDump(host);
    }
}
