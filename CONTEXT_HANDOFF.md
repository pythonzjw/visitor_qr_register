# CONTEXT_HANDOFF.md

## 当前目标

独立安卓 APK：按间隔识别微信访客二维码，随机去重填写姓名手机号并提交，同时接入统一授权中心，并通过 GitHub CI 构建。

## 已完成

- 新建 `android_app/` 原生 Android Java 工程。
- 实现前端：间隔分钟、二维码 X/Y 比例、启动/停止、打开无障碍、打开微信、状态和成功数。
- 实现无障碍服务：打开微信、长按二维码、识别二维码、进入小程序、选择外卖快递、填写两个可编辑字段、提交审核、成功后去重落库。
- 实现本机持久去重姓名/手机号。
- 服务器已新增授权项目 `visitor_qr_register`，租约 2400 秒，最多 2 台在线，`grace_seconds=0`。
- 客户端已写入服务端响应公钥 `visitor_qr_register_v1`。
- 授权客户端已适配新版响应签名：优先校验 `response_signed_payload + response_payload_signature` v2，缺失时回退旧 `response_signature`。
- 修正 AppConfig PEM 字符串为运行时真实换行，避免公钥解析失败。
- 配置脚本已避免重复 upsert 已存在项目，降低旧服务端把 `grace_seconds=0` 重置成默认值的风险。
- 已创建 GitHub 私有仓库并推送：`https://github.com/pythonzjw/visitor_qr_register`。
- GitHub Actions `v1.0.4` 构建通过，artifact：`visitor-qr-register-debug-apk`。

## 已修改文件

- `android_app/app/src/main/java/com/zjw/visitorqrregister/LicenseClient.java`
- `android_app/app/src/main/java/com/zjw/visitorqrregister/AppConfig.java`
- `android_app/scripts/configure_license_center.py`
- `CONTEXT_HANDOFF.md`

## 关键决策

- 独立 APK，不改现有项目。
- 服务端生成响应签名密钥对；客户端只内置公钥。
- 不做客户端请求私钥；服务端启用 rate_limit。
- 授权失败不在客户端展示原因，直接停止运行。

## 验证情况

- `python3 -m py_compile android_app/scripts/configure_license_center.py` 通过。
- XML 解析和 Java 静态检查通过。
- 服务端 `/api/verify` 已确认返回 v2 签名字段。
- 本机缺 Java Runtime，未本地 Gradle 构建。
- GitHub Actions tag `v1.0.4` 构建成功：`https://github.com/pythonzjw/visitor_qr_register/actions/runs/27355386061`。
- APK artifact：`https://api.github.com/repos/pythonzjw/visitor_qr_register/actions/artifacts/7567879309/zip`。

## 下一步

- 下载 Actions artifact 安装真机。
- 开启无障碍服务，校准二维码 X/Y 并测试微信长按二维码和小程序字段填写。

## 已知问题

- `v1.0.0`、`v1.0.1` CI 因 PEM 字符串换行失败；后续又修复 SDK tools 安装问题，`v1.0.3` 已通过；`v1.0.4` 完成授权 v2 适配。
- 如果目标小程序手机号字段不可编辑，本轮会失败。
- 默认二维码位置按视频估算，不同手机/聊天滚动位置需手动调整 X/Y。
