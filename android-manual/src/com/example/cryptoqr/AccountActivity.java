package com.example.cryptoqr;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 账户中心：显示用户信息、会员等级、积分，并支持兑换卡密。
 */
public class AccountActivity extends Activity implements View.OnClickListener {

    private TextView usernameText;
    private TextView membershipText;
    private TextView levelText;
    private TextView pointsText;
    private TextView expiryText;
    private EditText cardInput;
    private Button redeemButton;
    private Button logoutButton;
    private Button renameButton;
    private TextView statusText;

    private ApiClient apiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        apiClient = new ApiClient(this);

        usernameText = findViewById(R.id.username_text);
        membershipText = findViewById(R.id.membership_text);
        levelText = findViewById(R.id.level_text);
        pointsText = findViewById(R.id.points_text);
        expiryText = findViewById(R.id.expiry_text);
        cardInput = findViewById(R.id.card_input);
        redeemButton = findViewById(R.id.redeem_button);
        logoutButton = findViewById(R.id.logout_button);
        renameButton = findViewById(R.id.rename_button);
        statusText = findViewById(R.id.status_text);

        redeemButton.setOnClickListener(this);
        logoutButton.setOnClickListener(this);
        renameButton.setOnClickListener(this);

        usernameText.setText("用户名：" + (apiClient.getUsername() != null ? apiClient.getUsername() : "未知"));
        loadUserInfo();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.redeem_button) {
            redeemCard();
        } else if (v.getId() == R.id.logout_button) {
            logout();
        } else if (v.getId() == R.id.rename_button) {
            showRenameDialog();
        }
    }

    private void loadUserInfo() {
        apiClient.getUserInfo(new ApiClient.ApiCallback() {
            @Override
            public void onResult(boolean success, String message, JSONObject data) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (success && data != null) {
                            JSONObject user = data.optJSONObject("user");
                            if (user != null) {
                                membershipText.setText("会员：" + formatMembership(user.optString("membership_type", "free")));
                                levelText.setText("等级：LV" + user.optInt("level", 1));
                                pointsText.setText("积分：" + user.optInt("points", 0));

                                long expiry = user.optLong("expiry_date", 0) * 1000;
                                if (expiry > 0) {
                                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                                    expiryText.setText("到期：" + sdf.format(new Date(expiry)));
                                } else {
                                    expiryText.setText("到期：未开通会员");
                                }
                            }
                        } else {
                            statusText.setText("获取用户信息失败：" + message);
                            statusText.setTextColor(0xFFFF4444);
                        }
                    }
                });
            }
        });
    }

    private void redeemCard() {
        String code = cardInput.getText().toString().replace("-", "").trim().toUpperCase();
        if (code.length() != 16) {
            statusText.setText("卡密应为 16 位字符");
            statusText.setTextColor(0xFFFF4444);
            return;
        }

        redeemButton.setEnabled(false);
        statusText.setText("正在兑换...");
        statusText.setTextColor(0xFF00d4ff);

        apiClient.redeemCard(code, new ApiClient.ApiCallback() {
            @Override
            public void onResult(boolean success, String message, JSONObject data) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        redeemButton.setEnabled(true);
                        if (success) {
                            statusText.setText("兑换成功：" + message);
                            statusText.setTextColor(0xFF00CC66);
                            cardInput.setText("");
                            loadUserInfo();
                        } else {
                            statusText.setText(message);
                            statusText.setTextColor(0xFFFF4444);
                        }
                    }
                });
            }
        });
    }

    private void showRenameDialog() {
        final EditText input = new EditText(this);
        input.setHint("请输入新用户名");
        input.setTextColor(0xFF000000);
        input.setText(apiClient.getUsername());
        input.setSelection(input.getText().length());

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("修改用户名")
                .setView(input)
                .setPositiveButton("确定", null)
                .setNegativeButton("取消", null)
                .create();

        dialog.setOnShowListener(new android.content.DialogInterface.OnShowListener() {
            @Override
            public void onShow(android.content.DialogInterface d) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final String newName = input.getText().toString().trim();
                        if (newName.length() < 3) {
                            Toast.makeText(AccountActivity.this, "用户名至少 3 位", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        apiClient.rename(newName, new ApiClient.ApiCallback() {
                            @Override
                            public void onResult(boolean success, String message, JSONObject data) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (success && data != null) {
                                            JSONObject user = data.optJSONObject("user");
                                            String updatedName = user != null ? user.optString("username", newName) : newName;
                                            apiClient.setUsername(updatedName);
                                            usernameText.setText("用户名：" + updatedName);
                                            Toast.makeText(AccountActivity.this, "改名成功", Toast.LENGTH_SHORT).show();
                                            dialog.dismiss();
                                        } else {
                                            Toast.makeText(AccountActivity.this, message, Toast.LENGTH_LONG).show();
                                        }
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });

        dialog.show();
    }

    private void logout() {
        apiClient.clearAuth();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String formatMembership(String type) {
        switch (type) {
            case "vip":
                return "VIP 会员";
            case "svip":
                return "SVIP 会员";
            default:
                return "普通用户";
        }
    }
}
