#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""配置访客登记助手授权中心项目，并把响应验签公钥写入 AppConfig.java。

用法：
  LICENSE_CENTER_ADMIN_KEY=xxx python3 android_app/scripts/configure_license_center.py
可选：
  LICENSE_CENTER_BASE_URL=http://47.116.98.81:5003
"""
from __future__ import annotations

import json
import os
import re
import sys
import urllib.error
import urllib.request
from pathlib import Path

PROJECT_KEY = "visitor_qr_register"
DEFAULT_BASE_URL = "http://47.116.98.81:5003"
ROOT = Path(__file__).resolve().parents[1]
APP_CONFIG = ROOT / "app/src/main/java/com/zjw/visitorqrregister/AppConfig.java"
MODES = ["auto_register", "heartbeat_online", "project_online_limit", "rate_limit"]
LEASE_SECONDS = 2400
MAX_ONLINE_DEVICES = 2


def request_json(base_url: str, path: str, admin_key: str, payload: dict | None = None) -> dict:
    data = None if payload is None else json.dumps(payload, ensure_ascii=False).encode("utf-8")
    req = urllib.request.Request(base_url.rstrip("/") + path, data=data)
    req.add_header("X-Admin-Token", admin_key)
    req.add_header("X-Admin-Key", admin_key)
    if data is not None:
        req.add_header("Content-Type", "application/json; charset=utf-8")
        req.method = "POST"
    with urllib.request.urlopen(req, timeout=15) as resp:
        return json.loads(resp.read().decode("utf-8"))


def java_string(value: str) -> str:
    return json.dumps(value, ensure_ascii=False)


def patch_app_config(key_id: str, public_key_pem: str) -> None:
    src = APP_CONFIG.read_text(encoding="utf-8")
    replacement = (
        "private static Map<String, String> buildResponsePublicKeys() {\n"
        "        Map<String, String> keys = new HashMap<>();\n"
        f"        keys.put({java_string(key_id)}, {java_string(public_key_pem)});\n"
        "        return Collections.unmodifiableMap(keys);\n"
        "    }"
    )
    patched = re.sub(
        r"private static Map<String, String> buildResponsePublicKeys\(\) \{.*?\n    \}",
        replacement,
        src,
        flags=re.S,
    )
    if patched == src:
        raise RuntimeError("未找到 AppConfig.java 中的 buildResponsePublicKeys 方法")
    APP_CONFIG.write_text(patched, encoding="utf-8")


def upsert_project(base_url: str, admin_key: str) -> dict:
    payload = {
        "project_key": PROJECT_KEY,
        "enabled": True,
        "note": "访客登记助手：自动注册，30分钟心跳，最多2台在线",
        "modes": MODES,
        "lease_seconds": LEASE_SECONDS,
        "max_online_devices": MAX_ONLINE_DEVICES,
        "signing_secret": "",
    }
    try:
        return request_json(base_url, "/api/admin/projects", admin_key, payload)
    except urllib.error.HTTPError as e:
        if e.code not in (404, 405):
            raise
    legacy = {
        "project_key": PROJECT_KEY,
        "name": "Visitor QR Register",
        "enabled": True,
        "default_modes": MODES,
        "lease_seconds": LEASE_SECONDS,
        "grace_seconds": 0,
        "max_online_devices": MAX_ONLINE_DEVICES,
        "signing_secret": "",
    }
    return request_json(base_url, "/api/admin/project/upsert", admin_key, legacy)


def main() -> int:
    admin_key = os.environ.get("LICENSE_CENTER_ADMIN_KEY", "").strip()
    if not admin_key:
        print("缺少环境变量 LICENSE_CENTER_ADMIN_KEY", file=sys.stderr)
        return 2
    base_url = os.environ.get("LICENSE_CENTER_BASE_URL", DEFAULT_BASE_URL).strip().rstrip("/")

    project = upsert_project(base_url, admin_key)
    if project.get("ok") is False:
        raise RuntimeError(f"项目配置失败：{project}")

    key = request_json(base_url, f"/api/admin/response_keys/{PROJECT_KEY}/ensure", admin_key, {})
    if key.get("ok") is False or not key.get("key_id") or not key.get("public_key_pem"):
        raise RuntimeError(f"响应签名 key 获取失败：{key}")
    patch_app_config(key["key_id"], key["public_key_pem"])
    print(f"已配置 {PROJECT_KEY}：lease_seconds={LEASE_SECONDS}, max_online_devices={MAX_ONLINE_DEVICES}, key_id={key['key_id']}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
