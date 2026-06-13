# visitor_qr_register

独立安卓 APK：按设定间隔长按微信聊天里的访客二维码，识别进入小程序后随机生成不重复姓名和手机号并提交审核。

## 功能

- 前端设置轮询间隔，单位分钟。
- 前端支持点击二维码图片缩略图自动校准位置；也可手动设置 X/Y。
- 本机持久去重姓名和手机号：提交成功后才写入已用记录。
- 授权中心无感授权：首次启动自动注册机器码，30 分钟心跳，服务端租约 40 分钟，项目最多 2 台在线。
- 客户端不展示机器码、授权状态和授权失败原因；授权失败时自动停止。

## 授权中心配置

先配置项目并写入响应验签公钥：

```bash
cd /Users/zhengjiawei/Desktop/code/visitor_qr_register
LICENSE_CENTER_ADMIN_KEY='你的管理密钥' \
python3 android_app/scripts/configure_license_center.py
```

可选：

```bash
LICENSE_CENTER_BASE_URL='http://47.116.98.81:5003'
```

脚本会配置：

- `project_key=visitor_qr_register`
- `modes=auto_register + heartbeat_online + project_online_limit + rate_limit`
- `lease_seconds=2400`
- `max_online_devices=2`
- 生成/读取服务端 Ed25519 响应签名密钥，并把公钥写入 `AppConfig.java`

## 构建

```bash
cd /Users/zhengjiawei/Desktop/code/visitor_qr_register/android_app
./gradlew assembleDebug
```

输出：

```text
android_app/app/build/outputs/apk/debug/app-debug.apk
```

## 使用

1. 安装 APK。
2. 打开 App，进入“打开无障碍设置”，开启“访客登记助手服务”。
3. 点“打开微信，点击图片自动校准”，在微信聊天页点击二维码图片缩略图，App 会自动保存 X/Y 并返回聊天页。
4. 默认位置不准时，也可以回 App 手动调整二维码 X/Y 比例后保存。
5. 设置间隔分钟，点击“启动”。

## 注意

- 若小程序手机号字段由微信授权且不可编辑，本轮会失败，不会占用随机手机号。
- 若微信长按菜单文本变化，需要在 `AppConfig.QR_MENU_TEXTS` 增加对应文本。
