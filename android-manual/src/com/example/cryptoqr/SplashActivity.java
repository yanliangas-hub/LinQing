package com.example.cryptoqr;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

/**
 * 启动页：判断登录状态，自动跳转到对应页面。
 */
public class SplashActivity extends Activity implements Runnable {

    private TextView statusText;
    private ApiClient apiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        statusText = findViewById(R.id.status_text);
        apiClient = new ApiClient(this);

        statusText.setText("正在启动...");

        // 延迟 500ms 避免闪屏
        statusText.postDelayed(this, 500);
    }

    @Override
    public void run() {
        checkLoginState();
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
