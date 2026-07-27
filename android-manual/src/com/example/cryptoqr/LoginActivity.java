package com.example.cryptoqr;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

/**
 * 登录 / 注册页面。
 */
public class LoginActivity extends Activity implements View.OnClickListener {

    private EditText usernameInput;
    private EditText passwordInput;
    private Button actionButton;
    private TextView switchText;
    private TextView statusText;
    private LinearLayout loadingView;

    private ApiClient apiClient;
    private boolean isLoginMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        apiClient = new ApiClient(this);

        // 如果已有登录态，直接进入主页面
        if (apiClient.getToken() != null) {
            startMainActivity();
            return;
        }

        setContentView(R.layout.activity_login);

        usernameInput = findViewById(R.id.username_input);
        passwordInput = findViewById(R.id.password_input);
        actionButton = findViewById(R.id.action_button);
        switchText = findViewById(R.id.switch_text);
        statusText = findViewById(R.id.status_text);
        loadingView = findViewById(R.id.loading_view);

        actionButton.setOnClickListener(this);
        switchText.setOnClickListener(this);

        updateModeUI();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.action_button) {
            performAction();
        } else if (v.getId() == R.id.switch_text) {
            isLoginMode = !isLoginMode;
            updateModeUI();
        }
    }

    private void updateModeUI() {
        if (isLoginMode) {
            actionButton.setText("登录");
            switchText.setText("还没有账号？点击注册");
        } else {
            actionButton.setText("注册");
            switchText.setText("已有账号？点击登录");
        }
        statusText.setText("");
    }

    private void performAction() {
        final String username = usernameInput.getText().toString().trim();
        final String password = passwordInput.getText().toString().trim();

        if (username.length() < 3) {
            statusText.setText("用户名至少 3 位");
            statusText.setTextColor(0xFFFF4444);
            return;
        }
        if (password.length() < 6) {
            statusText.setText("密码至少 6 位");
            statusText.setTextColor(0xFFFF4444);
            return;
        }

        setLoading(true);

        ApiClient.ApiCallback callback = new ApiClient.ApiCallback() {
            @Override
            public void onResult(boolean success, String message, JSONObject data) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setLoading(false);
                        if (success && data != null) {
                            JSONObject user = data.optJSONObject("user");
                            String token = data.optString("token", null);
                            if (token != null) {
                                apiClient.setToken(token);
                                apiClient.setUsername(user != null ? user.optString("username", username) : username);
                                Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
                                startMainActivity();
                                return;
                            }
                        }
                        statusText.setText(message);
                        statusText.setTextColor(0xFFFF4444);
                    }
                });
            }
        };

        if (isLoginMode) {
            apiClient.login(username, password, callback);
        } else {
            apiClient.register(username, password, callback);
        }
    }

    private void setLoading(boolean loading) {
        actionButton.setEnabled(!loading);
        loadingView.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
