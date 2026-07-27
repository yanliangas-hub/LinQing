package com.example.cryptoqr;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.Toast;

/**
 * 主页面：加载解密后的 Web 二维码生成器，并提供账户入口。
 */
public class MainActivity extends Activity implements View.OnClickListener {

    private WebView webView;
    private ImageButton accountButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webview);
        accountButton = findViewById(R.id.account_button);

        setupWebView();
        loadProtectedWebApp();

        accountButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.account_button) {
            startActivity(new Intent(this, AccountActivity.class));
        }
    }

    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
    }

    private void loadProtectedWebApp() {
        try {
            String indexPath = CryptoUtils.prepareWebFiles(this);
            webView.loadUrl(indexPath);
        } catch (Exception e) {
            Toast.makeText(this, "加载应用失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
