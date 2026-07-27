#!/usr/bin/env python3
"""激活码生成器

算法说明：
- 激活码格式：XXXX-XXXX-XXXX-XXXX（16 位大写字母数字）
- 前 12 位为随机载荷（payload）
- 后 4 位为校验码（checksum）
- checksum = SHA256(payload + SECRET) 的前 4 位十六进制字符

示例：ABCD-1234-EFGH-5678
      ^^^^^^^^^^^^ payload
                  ^^^^ checksum
"""

import hashlib
import random
import string
import sys

# 与 Android 端保持一致的密钥（仅用于本地验证示例）
SECRET = "CryptoQR-2026-Secret-Key-v1"


def generate_code():
    """生成一个有效激活码。"""
    payload = ''.join(random.choices(string.ascii_uppercase + string.digits, k=12))
    checksum = hashlib.sha256((payload + SECRET).encode()).hexdigest()[:4].upper()
    code = payload + checksum
    return '-'.join([code[i:i+4] for i in range(0, 16, 4)])


def validate_code(code: str) -> bool:
    """验证激活码是否有效。"""
    code = code.replace('-', '').upper().strip()
    if len(code) != 16:
        return False
    if not all(c in string.ascii_uppercase + string.digits for c in code):
        return False
    payload = code[:12]
    checksum = code[12:16]
    expected = hashlib.sha256((payload + SECRET).encode()).hexdigest()[:4].upper()
    return checksum == expected


if __name__ == "__main__":
    count = int(sys.argv[1]) if len(sys.argv) > 1 else 5
    print(f"生成 {count} 个激活码（密钥: {SECRET}）：\n")
    for i in range(count):
        code = generate_code()
        print(f"{i+1}. {code}  {'有效' if validate_code(code) else '无效'}")
