package br.com.jefferson.totemsga.api;

import java.security.cert.CertificateException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import br.com.jefferson.totemsga.util.SessionCookieJar;
import br.com.jefferson.totemsga.util.SessionManager;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static ApiService apiService;
    private static ApiService autocompleteService;
    private static String lastAutocompleteUrl;
    private static SessionCookieJar cookieJar = new SessionCookieJar();

    public static ApiService getInstance(SessionManager sessionManager) {
        String baseUrl = sessionManager.getApiUrl();
        if (baseUrl == null || baseUrl.isEmpty()) return null;
        
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        
        // Habilita suporte a certificados auto-assinados e contextos inseguros (comum em redes locais)
        TrustManager[] trustManagers = null;
        SSLSocketFactory sslSocketFactory = null;
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
            sslSocketFactory = sslContext.getSocketFactory();
            trustManagers = trustAllCerts;

            builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
            builder.hostnameVerifier((hostname, session) -> true);
        } catch (Exception e) {}

        final TrustManager[] finalTrustManagers = trustManagers;
        final SSLSocketFactory finalSslSocketFactory = sslSocketFactory;

        builder.addInterceptor(logging)
                .addInterceptor(chain -> {
                    Request original = chain.request();
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
                    synchronized (RetrofitClient.class) {
                        String currentToken = sessionManager.getAccessToken();
                        String failedToken = response.request().header("Authorization");

                        // Se o token no sessionManager já mudou (por outra thread), tenta com o novo
                        if (failedToken != null && currentToken != null && !failedToken.equals("Bearer " + currentToken)) {
                            return response.request().newBuilder()
                                    .header("Authorization", "Bearer " + currentToken)
                                    .build();
                        }

                        if (response.code() == 401) {
                            // Usar um cliente limpo para evitar enviar o token expirado no header
                            OkHttpClient.Builder cleanBuilder = new OkHttpClient.Builder();
                            if (finalSslSocketFactory != null && finalTrustManagers != null) {
                                cleanBuilder.sslSocketFactory(finalSslSocketFactory, (X509TrustManager) finalTrustManagers[0]);
                                cleanBuilder.hostnameVerifier((hostname, session) -> true);
                            }
                            OkHttpClient cleanClient = cleanBuilder.build();

                            ApiService authApi = new Retrofit.Builder()
                                    .baseUrl(sessionManager.getApiUrl().endsWith("/") ? sessionManager.getApiUrl() : sessionManager.getApiUrl() + "/")
                                    .addConverterFactory(GsonConverterFactory.create())
                                    .client(cleanClient)
                                    .build()
                                    .create(ApiService.class);

                            java.util.Map<String, String> params = new java.util.HashMap<>();
                            params.put("grant_type", "refresh_token");
                            params.put("client_id", sessionManager.getClientId());
                            params.put("client_secret", sessionManager.getClientSecret());
                            params.put("refresh_token", sessionManager.getRefreshToken());

                            try {
                                retrofit2.Response<br.com.jefferson.totemsga.model.TokenResponse> refreshResponse = authApi.getToken(params).execute();
                                if (refreshResponse.isSuccessful() && refreshResponse.body() != null) {
                                    sessionManager.saveTokens(refreshResponse.body().accessToken, refreshResponse.body().refreshToken);
                                    return response.request().newBuilder()
                                            .header("Authorization", "Bearer " + refreshResponse.body().accessToken)
                                            .build();
                                }
                            } catch (java.io.IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    return null;
                });

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(builder.build())
                .build();

        apiService = retrofit.create(ApiService.class);
        return apiService;
    }

    public static ApiService getAutocompleteInstance(SessionManager sessionManager) {
        return getAutocompleteInstance(sessionManager, null);
    }

    public static ApiService getAutocompleteInstance(SessionManager sessionManager, String overrideUrl) {
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

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.connectTimeout(4, java.util.concurrent.TimeUnit.SECONDS);
        builder.readTimeout(4, java.util.concurrent.TimeUnit.SECONDS);
        builder.cookieJar(cookieJar);

        // Habilita suporte a certificados auto-assinados
        try {
            final javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[]{
                    new javax.net.ssl.X509TrustManager() {
                        @Override public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {}
                        @Override public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {}
                        @Override public java.security.cert.X509Certificate[] getAcceptedIssuers() { return new java.security.cert.X509Certificate[]{}; }
                    }
            };

            final javax.net.ssl.SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            builder.sslSocketFactory(sslContext.getSocketFactory(), (javax.net.ssl.X509TrustManager) trustAllCerts[0]);
            builder.hostnameVerifier((hostname, session) -> true);
        } catch (Exception e) {}

        builder.addInterceptor(logging);

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
