package com.rtsoft.growtopia;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class LoginSpoof {

    private static final String TAG = "LoginSpoof";
    private static final String PREFS = "launcher_data";
    private static final String PREFS_ALT = "LoginSpoof";

    private static final String K_LTOKEN        = "spoof_ltoken";
    private static final String K_ENABLE        = "spoof_ltoken_enable";
    private static final String K_MAC           = "spoof_login_mac";
    private static final String K_RID           = "spoof_login_rid";
    private static final String K_WK            = "spoof_login_wk";
    private static final String K_PLATFORM      = "spoof_login_platform";
    private static final String K_REFRESH_TOKEN = "spoof_refresh_token";
    private static final String K_GOOGLE_TOKEN  = "spoof_google_token";
    private static final String K_GOOGLE_LOGS   = "spoof_google_logs";

    private static final String CHECKTOKEN_URL =
            "https://login.growtopiagame.com/player/growid/checktoken";

    public static final int PLATFORM_ANDROID = 0;
    public static final int PLATFORM_WINDOWS = 1;

    private static final ExecutorService NET = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final SecureRandom RNG = new SecureRandom();

    private final SharedPreferences prefs;
    private final SharedPreferences alt;

    public LoginSpoof(Context context) {
        Context app = context.getApplicationContext();
        this.prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        this.alt = app.getSharedPreferences(PREFS_ALT, Context.MODE_PRIVATE);
    }

    public interface ExchangeCallback {
        void onSuccess(String ltoken);
        void onFailure(String message, String rawResponse);
    }

    public String getLtoken() {
        String a = prefs.getString(K_LTOKEN, "");
        if (!TextUtils.isEmpty(a)) return a;
        return alt.getString("saved_token", "");
    }
    public boolean isEnabled() {
        return prefs.getBoolean(K_ENABLE, false) || alt.getBoolean("auto_login", false);
    }
    public String getMac()          { return prefs.getString(K_MAC, ""); }
    public String getRid()          { return prefs.getString(K_RID, ""); }
    public String getWk()           { return prefs.getString(K_WK, ""); }
    public int getPlatform()        { return prefs.getInt(K_PLATFORM, PLATFORM_WINDOWS); }
    public String getRefreshToken() { return prefs.getString(K_REFRESH_TOKEN, ""); }
    public String getGoogleToken()  { return prefs.getString(K_GOOGLE_TOKEN, ""); }
    public String getGoogleLogs()   { return prefs.getString(K_GOOGLE_LOGS, "Not started."); }

    public void setLtoken(String v) {
        prefs.edit().putString(K_LTOKEN, safe(v)).apply();
        alt.edit().putString("saved_token", safe(v)).apply();
    }
    public void setEnabled(boolean v) {
        prefs.edit().putBoolean(K_ENABLE, v).apply();
        alt.edit().putBoolean("auto_login", v).apply();
    }
    public void setMac(String v)          { prefs.edit().putString(K_MAC, safe(v)).apply(); }
    public void setRid(String v)          { prefs.edit().putString(K_RID, safe(v)).apply(); }
    public void setWk(String v)           { prefs.edit().putString(K_WK, safe(v)).apply(); }
    public void setPlatform(int v)        { prefs.edit().putInt(K_PLATFORM, v).apply(); }
    public void setRefreshToken(String v) { prefs.edit().putString(K_REFRESH_TOKEN, safe(v)).apply(); }
    public void setGoogleToken(String v)  {
        prefs.edit().putString(K_GOOGLE_TOKEN, safe(v)).apply();
        alt.edit().putString("saved_token", safe(v)).apply();
    }
    public void setGoogleLogs(String v)   { prefs.edit().putString(K_GOOGLE_LOGS, safe(v)).apply(); }

    public void clearLtoken() {
        prefs.edit().remove(K_LTOKEN).putBoolean(K_ENABLE, false).apply();
        alt.edit().remove("saved_token").putBoolean("auto_login", false).apply();
    }
    public void clearGoogleToken() {
        prefs.edit().remove(K_GOOGLE_TOKEN).apply();
        alt.edit().remove("saved_token").putBoolean("auto_login", false).apply();
    }

    private void clearTokensOnIdentityChange() {
        clearLtoken();
        clearGoogleToken();
    }

    public String generateMac() {
        byte[] b = new byte[6];
        RNG.nextBytes(b);
        b[0] = (byte) ((b[0] & 0xFC) | 0x02);
        StringBuilder sb = new StringBuilder(17);
        for (int i = 0; i < b.length; i++) {
            if (i > 0) sb.append(':');
            sb.append(String.format("%02X", b[i] & 0xFF));
        }
        String mac = sb.toString();
        setMac(mac);
        clearTokensOnIdentityChange();
        return mac;
    }

    public String generateRid() {
        String rid = randomHex(16);
        setRid(rid);
        clearTokensOnIdentityChange();
        return rid;
    }

    public String generateWk() {
        String wk = randomHex(16);
        setWk(wk);
        clearTokensOnIdentityChange();
        return wk;
    }

    private static String randomHex(int bytes) {
        byte[] b = new byte[bytes];
        RNG.nextBytes(b);
        StringBuilder sb = new StringBuilder(bytes * 2);
        for (byte value : b) sb.append(String.format("%02X", value & 0xFF));
        return sb.toString();
    }

    public void exchangeStoredRefreshToken(ExchangeCallback cb) {
        exchangeRefreshToken(getRefreshToken(), cb);
    }

    public void exchangeRefreshToken(final String refreshToken, final ExchangeCallback cb) {
        final ExchangeCallback callback = cb != null ? cb : NOOP;
        if (TextUtils.isEmpty(refreshToken)) {
            MAIN.post(() -> callback.onFailure("Refresh token is empty", ""));
            return;
        }
        setRefreshToken(refreshToken);
        NET.execute(() -> {
            HttpURLConnection conn = null;
            String raw = "";
            try {
                String body = "refreshToken=" + enc(refreshToken)
                        + "&clientData=" + enc(buildClientData());
                conn = (HttpURLConnection) new URL(CHECKTOKEN_URL).openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setRequestProperty("User-Agent", "UbiServices_SDK_2022.Release.9_PC64_ansi_static");
                byte[] payload = body.getBytes(StandardCharsets.UTF_8);
                conn.setFixedLengthStreamingMode(payload.length);
                try (OutputStream os = conn.getOutputStream()) { os.write(payload); }
                int status = conn.getResponseCode();
                raw = readBody(status >= 200 && status < 400 ? conn.getInputStream() : conn.getErrorStream());
                String ltoken = parseLtoken(raw);
                if (!TextUtils.isEmpty(ltoken)) {
                    setLtoken(ltoken);
                    setEnabled(true);
                    final String out = ltoken;
                    MAIN.post(() -> callback.onSuccess(out));
                } else {
                    final String reason = describeFailure(status, raw);
                    final String rawFinal = raw;
                    MAIN.post(() -> callback.onFailure(reason, rawFinal));
                }
            } catch (Exception e) {
                Log.e(TAG, "refresh token exchange failed: " + e.getMessage());
                final String rawFinal = raw;
                MAIN.post(() -> callback.onFailure("Network error: " + e.getMessage(), rawFinal));
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    private String buildClientData() {
        String mac = getMac(); if (TextUtils.isEmpty(mac)) mac = generateMac();
        String rid = getRid(); if (TextUtils.isEmpty(rid)) rid = generateRid();
        String wk  = getWk();  if (TextUtils.isEmpty(wk))  wk  = generateWk();
        int platformId = getPlatform() == PLATFORM_WINDOWS ? 0 : 4;
        return "platformID|" + platformId + "\ndeviceVersion|0\nmac|" + mac + "\nrid|" + rid + "\nwk|" + wk + "\nlmode|1\n";
    }

    private static String parseLtoken(String raw) {
        if (TextUtils.isEmpty(raw)) return "";
        try {
            JSONObject json = new JSONObject(raw.trim());
            String status = json.optString("status", "");
            if (!TextUtils.isEmpty(status) && !"success".equalsIgnoreCase(status)) return "";
            for (String key : new String[] {"token", "ltoken", "loginToken"}) {
                String v = json.optString(key, "");
                if (!TextUtils.isEmpty(v)) return v;
            }
        } catch (Exception e) {
            Log.w(TAG, "checktoken response was not JSON: " + e.getMessage());
        }
        return "";
    }

    private static String describeFailure(int status, String raw) {
        String msg = "";
        if (!TextUtils.isEmpty(raw)) {
            try { msg = new JSONObject(raw.trim()).optString("message", ""); } catch (Exception ignored) {}
        }
        if (TextUtils.isEmpty(msg)) msg = "No ltoken in response (HTTP " + status + ")";
        return msg;
    }

    private static String readBody(InputStream in) throws Exception {
        if (in == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line; while ((line = r.readLine()) != null) sb.append(line).append('\n');
        }
        return sb.toString().trim();
    }

    private static String enc(String v) {
        try { return URLEncoder.encode(v, "UTF-8"); } catch (Exception e) { return ""; }
    }

    private static String safe(String v) { return v == null ? "" : v; }

    private static final ExchangeCallback NOOP = new ExchangeCallback() {
        @Override public void onSuccess(String ltoken) {}
        @Override public void onFailure(String message, String rawResponse) {}
    };
}