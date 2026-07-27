package com.example.cryptoqr;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import org.json.JSONObject;

/**
 * 启动页：判断设备激活与登录状态，自动跳转到对应页面。
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

        statusText.setText("正在检查授权状态...");

        // 延迟 500ms 避免闪屏
        statusText.postDelayed(this, 500);
    }

    @Override
    public void run() {
        apiClient.checkDevice(this);
    }

    @Override
    public void onResult(boolean success, String message, JSONObject data) {
        if (success && data != null && data.optBoolean("activated", false)) {
            checkLoginState();
        } else {
            statusText.setText("需要激活设备");
            startActivity(new Intent(SplashActivity.this, ActivationActivity.class));
            finish();
        }
    }

    private void checkLoginState() {
        if (apiClient.getToken() != null) {
            statusText.setText("正在进入...");
            startActivity(new Intent(this, MainActivity.class));
        } else {
            statusText.setText("需要登录");
            startActivity(new Intent(this, LoginActivity.class));
        }
        finish();
    }
}
