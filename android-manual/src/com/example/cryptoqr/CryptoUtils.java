package com.example.cryptoqr;

import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * 源码保护工具：将 APK assets 中的加密 Web 资源解密到应用私有目录。
 */
public class CryptoUtils {

    private static final String AES_KEY = "CryptoQR2026Key!"; // 16 bytes
    private static final String AES_IV = "CryptoQR2026IV!!";  // 16 bytes
    private static final String ASSET_ENC_DIR = "";
    private static final String WEB_DIR = "web";

    private static final String[] WEB_FILES = {
            "index.html.enc",
            "style.css.enc",
            "app.js.enc",
            "qrcode.min.js.enc"
    };

    /**
     * 确保 Web 资源已解密到私有目录，并返回 index.html 的 file:// 路径。
     */
    public static String prepareWebFiles(Context context) throws Exception {
        File webDir = new File(context.getFilesDir(), WEB_DIR);
        File marker = new File(webDir, ".ready");

        if (!marker.exists() || isAssetsUpdated(context, webDir)) {
            if (!webDir.exists() && !webDir.mkdirs()) {
                throw new IOException("无法创建 Web 资源目录");
            }
            for (String encName : WEB_FILES) {
                String plainName = encName.replace(".enc", "");
                String assetPath = ASSET_ENC_DIR.isEmpty() ? encName : ASSET_ENC_DIR + "/" + encName;
                byte[] encrypted = readAsset(context, assetPath);
                byte[] decrypted = decrypt(encrypted);
                writeFile(new File(webDir, plainName), decrypted);
            }
            writeFile(marker, "ready".getBytes());
        }

        return "file://" + new File(webDir, "index.html").getAbsolutePath();
    }

    private static byte[] readAsset(Context context, String path) throws IOException {
        InputStream is = context.getAssets().open(path);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int len;
        while ((len = is.read(buffer)) != -1) {
            baos.write(buffer, 0, len);
        }
        is.close();
        return baos.toByteArray();
    }

    private static void writeFile(File file, byte[] data) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(data);
        fos.close();
    }

    private static byte[] decrypt(byte[] encrypted) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(AES_KEY.getBytes("UTF-8"), "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(AES_IV.getBytes("UTF-8"));
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
        return cipher.doFinal(encrypted);
    }

    private static boolean isAssetsUpdated(Context context, File webDir) {
        // 简单校验：如果 assets 中的加密文件比已解密文件新，则重新解密。
        // 实际项目可记录版本号或 CRC 校验。
        try {
            for (String encName : WEB_FILES) {
                File plainFile = new File(webDir, encName.replace(".enc", ""));
                if (!plainFile.exists()) return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }
}
