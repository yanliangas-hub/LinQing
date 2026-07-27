#!/usr/bin/env python3
"""加密 Web 资源，用于 APK 源码保护。

使用 AES-128-CBC 加密，密钥与 IV 需与 Android 端 CryptoUtils.java 保持一致。
"""

import os
import sys
from pathlib import Path
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
from cryptography.hazmat.backends import default_backend

# 必须与 CryptoUtils.java 中的 KEY 和 IV 完全一致（16 字节）
KEY = b"CryptoQR2026Key!"
IV = b"CryptoQR2026IV!!"


def encrypt_file(input_path: str, output_path: str):
    with open(input_path, "rb") as f:
        plaintext = f.read()

    # PKCS7 填充
    pad_len = 16 - (len(plaintext) % 16)
    plaintext += bytes([pad_len]) * pad_len

    cipher = Cipher(algorithms.AES(KEY), modes.CBC(IV), backend=default_backend())
    encryptor = cipher.encryptor()
    ciphertext = encryptor.update(plaintext) + encryptor.finalize()

    with open(output_path, "wb") as f:
        f.write(ciphertext)


def main(src_dir: str, dst_dir: str):
    src = Path(src_dir)
    dst = Path(dst_dir)
    dst.mkdir(parents=True, exist_ok=True)

    for f in src.iterdir():
        if f.is_file():
            out = dst / (f.name + ".enc")
            encrypt_file(str(f), str(out))
            print(f"Encrypted {f.name} -> {out.name}")


if __name__ == "__main__":
    if len(sys.argv) != 3:
        print(f"Usage: {sys.argv[0]} <src_dir> <dst_dir>")
        sys.exit(1)
    main(sys.argv[1], sys.argv[2])
