package br.com.jefferson.totemsga.teste;

import android.util.Log;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

public class TesteSessionCookieJar implements CookieJar {
    private static final String TAG = "TesteCookieJar";
    private final Map<String, List<Cookie>> cookieStore = new HashMap<>();

    @Override
    public void saveFromResponse(@NonNull HttpUrl url, @NonNull List<Cookie> cookies) {
        String host = url.host();
        List<Cookie> existingCookies = cookieStore.get(host);
        if (existingCookies == null) {
            existingCookies = new ArrayList<>();
        }

        for (Cookie newCookie : cookies) {
            Log.d(TAG, "Salvando cookie de " + host + ": " + newCookie.name() + "=" + newCookie.value());
            // Remove cookie antigo com mesmo nome para atualizar
            java.util.Iterator<Cookie> it = existingCookies.iterator();
            while (it.hasNext()) {
                if (it.next().name().equals(newCookie.name())) {
                    it.remove();
                }
            }
            existingCookies.add(newCookie);
        }
        cookieStore.put(host, existingCookies);
    }

    @NonNull
    @Override
    public List<Cookie> loadForRequest(@NonNull HttpUrl url) {
        String host = url.host();
        List<Cookie> cookies = cookieStore.get(host);
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                Log.d(TAG, "Enviando cookie para " + host + ": " + cookie.name());
            }
            return cookies;
        }
        return new ArrayList<>();
    }
}
