package com.example.cryptoqr;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * 后端 API 客户端：负责设备激活、账户登录注册、卡密兑换、用户信息查询。
 */
public class ApiClient {

    private static final String TAG = "ApiClient";
    // 本地测试地址；生产环境请替换为真实域名或 IP
    private static final String BASE_URL = "http://10.0.2.2:3000/api";

    private static final String PREFS_NAME = "CryptoQRAuth";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_USERNAME = "username";

    public interface ApiCallback {
        void onResult(boolean success, String message, JSONObject data);
    }

    private final Context context;

    public ApiClient(Context context) {
        this.context = context.getApplicationContext();
    }

    public static String getDeviceId(Context context) {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    public String getBaseUrl() {
        return BASE_URL;
    }

    public void setToken(String token) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_TOKEN, token).apply();
    }

    public String getToken() {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_TOKEN, null);
    }

    public void setUsername(String username) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_USERNAME, username).apply();
    }

    public String getUsername() {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_USERNAME, null);
    }

    public void clearAuth() {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().remove(KEY_TOKEN).remove(KEY_USERNAME).apply();
    }

    // 设备激活
    public void activateDevice(String cardCode, ApiCallback callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("device_id", getDeviceId(context));
            body.put("card_code", cardCode);
        } catch (Exception e) {
            callback.onResult(false, "构造请求失败", null);
            return;
        }
        post("/activate-device", body, false, callback);
    }

    // 检查设备激活状态
    public void checkDevice(ApiCallback callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("device_id", getDeviceId(context));
        } catch (Exception e) {
            callback.onResult(false, "构造请求失败", null);
            return;
        }
        post("/check-device", body, false, callback);
    }

    // 登录
    public void login(String username, String password, ApiCallback callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("username", username);
            body.put("password", password);
        } catch (Exception e) {
            callback.onResult(false, "构造请求失败", null);
            return;
        }
        post("/login", body, false, callback);
    }

    // 注册
    public void register(String username, String password, ApiCallback callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("username", username);
            body.put("password", password);
        } catch (Exception e) {
            callback.onResult(false, "构造请求失败", null);
            return;
        }
        post("/register", body, false, callback);
    }

    // 获取用户信息
    public void getUserInfo(ApiCallback callback) {
        get("/user", callback);
    }

    // 修改用户名
    public void rename(String newUsername, ApiCallback callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("new_username", newUsername);
        } catch (Exception e) {
            callback.onResult(false, "构造请求失败", null);
            return;
        }
        post("/user/rename", body, true, callback);
    }

    // 兑换卡密
    public void redeemCard(String cardCode, ApiCallback callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("card_code", cardCode);
        } catch (Exception e) {
            callback.onResult(false, "构造请求失败", null);
            return;
        }
        post("/redeem", body, true, callback);
    }

    private void get(String path, ApiCallback callback) {
        new Thread(new HttpTask("GET", path, null, needAuth(false), callback)).start();
    }

    private void post(String path, JSONObject body, boolean needAuth, ApiCallback callback) {
        new Thread(new HttpTask("POST", path, body, needAuth(needAuth), callback)).start();
    }

    private String needAuth(boolean needAuth) {
        return needAuth ? getToken() : null;
    }

    private static class HttpTask implements Runnable {
        private final String method;
        private final String path;
        private final JSONObject body;
        private final String authToken;
        private final ApiCallback callback;

        HttpTask(String method, String path, JSONObject body, String authToken, ApiCallback callback) {
            this.method = method;
            this.path = path;
            this.body = body;
            this.authToken = authToken;
            this.callback = callback;
        }

        @Override
        public void run() {
            try {
                URL url = new URL(BASE_URL + path);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod(method);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                if ("POST".equals(method)) {
                    conn.setRequestProperty("Content-Type", "application/json");
                }
                if (authToken != null) {
                    conn.setRequestProperty("Authorization", "Bearer " + authToken);
                }

                if (body != null) {
                    conn.setDoOutput(true);
                    OutputStream os = conn.getOutputStream();
                    os.write(body.toString().getBytes(StandardCharsets.UTF_8));
                    os.close();
                }

                String response = readResponse(conn);
                JSONObject json = new JSONObject(response);
                handleResponse(json, callback);
            } catch (Exception e) {
                Log.e(TAG, method + " " + path + " failed", e);
                callback.onResult(false, "网络错误：" + e.getMessage(), null);
            }
        }
    }

    private static String readResponse(HttpURLConnection conn) throws Exception {
        int responseCode = conn.getResponseCode();
        BufferedReader reader;
        if (responseCode >= 200 && responseCode < 300) {
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        } else {
            reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
        }
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        return sb.toString();
    }

    private static void handleResponse(JSONObject json, ApiCallback callback) {
        try {
            boolean success = json.optBoolean("success", false);
            String message = json.optString("message", "");
            JSONObject data = json.optJSONObject("data");
            callback.onResult(success, message, data);
        } catch (Exception e) {
            callback.onResult(false, "解析响应失败", null);
        }
    }
}
