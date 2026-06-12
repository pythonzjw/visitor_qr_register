# 当前目标
完成 `code/visitor_qr_register/` 独立安卓 APK：服务端授权项目、GitHub 私有仓库、CI 构建 APK，并在安卓真机上完成 ADB-only 全流程调试。当前真机已安装旧 APK，发现授权公钥解析兼容问题，需构建修复版复测。

# 已完成
- 2026-06-12 真机安装后 App 可启动，界面显示正常；日志发现 `LicenseClient: verify failed: InvalidKeySpecException`，服务端直接 verify 对该 machine_id 返回 `ok=true`。
- 新增 `code/visitor_qr_register/android_app/` 原生 Java Android 工程。
- 实现前端：间隔分钟、二维码 X/Y 比例、启动/停止、打开无障碍、打开微信、状态、成功数。
- 实现无障碍自动化：打开微信、长按二维码、识别二维码、等待小程序、选择外卖快递、填写两个可编辑字段、提交审核。
- 提交成功后持久化已用姓名/手机号，确保本机不重复。
- 服务器授权中心已新增 `visitor_qr_register` 项目：`auto_register + heartbeat_online + project_online_limit + rate_limit`，`lease_seconds=2400`，`grace_seconds=0`，`max_online_devices=2`。
- 客户端已内置响应公钥 `visitor_qr_register_v1`，并适配授权中心 v2 响应签名。
- GitHub 私有仓库已创建并推送：`https://github.com/pythonzjw/visitor_qr_register`。
- GitHub Actions `v1.0.4` 已成功构建 debug APK，artifact API：`https://api.github.com/repos/pythonzjw/visitor_qr_register/actions/artifacts/7567879309/zip`。
- 真机 ADB 通道已确认：安卓设备 `313d4194` 状态为 `device`。
- APK 已下载到本地并复制到 Windows：`C:\Users\admin\AppData\Local\Temp\visitor_qr_register_v104.apk`。
- 已将 APK 推到手机：`/sdcard/Download/visitor_qr_register_v104.apk` 与 `/data/local/tmp/visitor_qr_register_v104.apk`。

# 已修改文件
- `android_app/app/src/main/java/com/zjw/visitorqrregister/LicenseClient.java`：新增 Ed25519 SPKI 解析 fallback，修复 Android 真机 `InvalidKeySpecException`。
- `code/visitor_qr_register/` 工程文件
- `CONTEXT_HANDOFF.md`
- `code/visitor_qr_register/CONTEXT_HANDOFF.md`

# 关键决策
- 独立 APK，不改现有 `android_order_grabber`。
- 服务端生成响应签名密钥对，客户端只内置公钥。
- 不做客户端请求私钥；使用授权中心 rate_limit 降低刷请求风险。
- 授权失败不展示任何授权信息，直接停止运行。
- 真机调试只走 ADB/VisionAgent，不操作 Windows 鼠标、键盘、热键、桌面截图或 GUI。
- 用户允许只修改安装所需权限，但不能影响手机其他已调好的功能；临时改过的小米安装保护键已恢复。

# 验证情况
- 2026-06-12 静态检查：`LicenseClient.java` 括号/大括号平衡；本地无 JDK，尚未构建修复版 APK。
- `python3 -m py_compile code/visitor_qr_register/android_app/scripts/configure_license_center.py` 通过。
- XML 解析检查通过。
- Java 包名和括号平衡静态检查通过。
- 服务端 `/api/verify` 已确认返回 v2 签名字段。
- `v1.0.4` CI 成功：`https://github.com/pythonzjw/visitor_qr_register/actions/runs/27355386061`。
- 真机安装验证：`adb install -r ...visitor_qr_register_v104.apk` 和 `pm install -r /data/local/tmp/...apk` 均返回 `INSTALL_FAILED_USER_RESTRICTED: Install canceled by user`。
- Xiaomi/Redmi Android 15：`install_non_market_apps=1`、`verifier_verify_adb_installs=0`、`package_verifier_enable=0`，`com.android.shell REQUEST_INSTALL_PACKAGES=allow`。
- 小米包安装器 UI 可打开 APK，但停在“未查询到此应用的 ICP 备案信息”弹窗；ADB/input/touchscreen tap 均无法触发“继续安装”。底层 `/dev/input/event6` sendevent 无权限。
- 临时测试并已恢复：`secure miui_safe_mode` 从 `1` 临时置 `0` 后恢复为 `1`；`skip_unknown_source_dialog`、`secure_mode_app_install_verify`、`user_close_security_mode_flag` 已删除恢复为 `null`。

# 未完成事项
- 2026-06-12 15:26 复查：`pm list packages com.zjw.visitorqrregister` 仍为空，手机仍停在小米安装器“未查询到此应用的 ICP 备案信息”弹窗；ADB 点击被拦截，需要手机本机手动点击“继续安装”。

# 下一步
- 需要用户允许 commit/push 触发 CI 构建修复版 APK；安装修复版后继续验证授权自动注册/心跳/2 台限制，再进入无障碍和微信流程。

# 已知问题
- 当前阻塞点是小米安装器的 ICP 备案确认页禁止 ADB 模拟点击，不是 APK 构建失败。
- 若小程序手机号字段由微信授权且不可编辑，本轮会失败且不占用随机手机号。
- 默认二维码位置按视频估算，不同手机或聊天滚动后需调整 X/Y。
