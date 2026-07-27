package com.example.cryptoqr;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

/**
 * 启动页：使用设备 ID 自动登录，无需账号密码。
 */
public class SplashActivity extends Activity implements Runnable, ApiClient.ApiCallback {

    private TextView statusText;
    private ApiClient apiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        statusText = findViewById(R.id.status_text);
        apiClient = new ApiClient(this);

        statusText.setText("正在初始化...");

        // 延迟 500ms 避免闪屏
        statusText.postDelayed(this, 500);
    }

    @Override
    public void run() {
        statusText.setText("正在同步设备信息...");
        apiClient.deviceLogin(this);
    }

    @Override
    public void onResult(boolean success, String message, JSONObject data) {
        if (success && data != null) {
            String token = data.optString("token", null);
            JSONObject user = data.optJSONObject("user");
            if (token != null) {
                apiClient.setToken(token);
                if (user != null) {
                    apiClient.setUsername(user.optString("username", ""));
                }
            }
        } else {
            Toast.makeText(this, "设备登录失败：" + message, Toast.LENGTH_LONG).show();
        }

        // 无论自动登录是否成功，都进入主界面（二维码生成不依赖登录）
        statusText.setText("正在进入...");
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
