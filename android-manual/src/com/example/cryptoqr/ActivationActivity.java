package com.example.cryptoqr;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

/**
 * 设备激活页面：输入卡密完成设备激活。
 */
public class ActivationActivity extends Activity implements View.OnClickListener {

    private EditText codeInput;
    private TextView statusText;
    private Button activateButton;
    private ApiClient apiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        apiClient = new ApiClient(this);

        setContentView(R.layout.activity_activation);

        codeInput = findViewById(R.id.code_input);
        statusText = findViewById(R.id.status_text);
        activateButton = findViewById(R.id.activate_button);

        codeInput.addTextChangedListener(new CardCodeFormatter(codeInput));
        activateButton.setOnClickListener(this);

        statusText.setText("请输入卡密激活设备");
        statusText.setTextColor(0xFFa0a0a0);
    }

    @Override
    public void onClick(View v) {
        activateDevice();
    }

    private void activateDevice() {
        String code = codeInput.getText().toString().replace("-", "").trim().toUpperCase();

        if (code.length() != 16) {
            statusText.setText("卡密应为 16 位字符");
            statusText.setTextColor(0xFFFF4444);
            return;
        }

        activateButton.setEnabled(false);
        statusText.setText("正在激活...");
        statusText.setTextColor(0xFF00d4ff);

        apiClient.activateDevice(code, new ApiClient.ApiCallback() {
            @Override
            public void onResult(boolean success, String message, JSONObject data) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        activateButton.setEnabled(true);
                        if (success) {
                            statusText.setText("激活成功！");
                            statusText.setTextColor(0xFF00CC66);
                            Toast.makeText(ActivationActivity.this, message, Toast.LENGTH_SHORT).show();
                            startLoginActivity();
                        } else {
                            statusText.setText(message);
                            statusText.setTextColor(0xFFFF4444);
                        }
                    }
                });
            }
        });
    }

    private void startLoginActivity() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private static class CardCodeFormatter implements TextWatcher {
        private final EditText editText;
        private boolean isSelfChange = false;

        CardCodeFormatter(EditText editText) {
            this.editText = editText;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (isSelfChange) return;

            String raw = s.toString().replace("-", "").toUpperCase();
            StringBuilder formatted = new StringBuilder();
            for (int i = 0; i < raw.length() && i < 16; i++) {
                if (i > 0 && i % 4 == 0) {
                    formatted.append('-');
                }
                formatted.append(raw.charAt(i));
            }

            isSelfChange = true;
            editText.setText(formatted.toString());
            editText.setSelection(formatted.length());
            isSelfChange = false;
        }
    }
}
