package br.com.jefferson.totemsga.util;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

public class SessionCookieJar implements CookieJar {
    private final Map<String, List<Cookie>> cookieStore = new HashMap<>();

    @Override
    public void saveFromResponse(@NonNull HttpUrl url, @NonNull List<Cookie> cookies) {
        if (cookies.isEmpty()) return;

        // Mescla por nome em vez de sobrescrever: uma resposta sem Set-Cookie
        // (comum após seguir redirect de login) nao pode apagar o PHPSESSID
        // ja armazenado por uma resposta anterior.
        Map<String, Cookie> merged = new LinkedHashMap<>();
        List<Cookie> existing = cookieStore.get(url.host());
        if (existing != null) {
            for (Cookie c : existing) {
                if (c.expiresAt() > System.currentTimeMillis()) {
                    merged.put(c.name(), c);
                }
            }
        }
        for (Cookie c : cookies) {
            merged.put(c.name(), c);
        }
        cookieStore.put(url.host(), new ArrayList<>(merged.values()));
    }

    @NonNull
    @Override
    public List<Cookie> loadForRequest(@NonNull HttpUrl url) {
        List<Cookie> cookies = cookieStore.get(url.host());
        return cookies != null ? cookies : new ArrayList<>();
    }

    public void clear() {
        cookieStore.clear();
    }

    public String debugDump(String host) {
        List<Cookie> cookies = cookieStore.get(host);
        if (cookies == null || cookies.isEmpty()) return "(nenhum cookie armazenado para " + host + ")";
        StringBuilder sb = new StringBuilder();
        for (Cookie c : cookies) {
            String value = c.value();
            String preview = value.length() > 12 ? value.substring(0, 12) + "..." : value;
            sb.append(c.name()).append("=").append(preview)
              .append(" [expiresAt=").append(c.expiresAt())
              .append(", persistent=").append(c.persistent())
              .append("] ");
        }
        return sb.toString();
    }
}
