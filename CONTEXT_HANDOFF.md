# 当前目标
完成 `code/visitor_qr_register/` 安卓 APK：GitHub tag 触发 CI 自动构建并自动发布 Release APK；后续由用户下载安装到真机测试微信二维码流程。

# 已完成
- 原生 Java Android APK 已实现：前端可设置轮询间隔、二维码图片点击位置 X/Y、启动/停止、打开无障碍、打开微信、状态和成功数。
- 授权中心已接入 `visitor_qr_register`：首次机器码自动注册、30 分钟心跳、最大在线机器数量 2、签名验签与固定 debug 签名。
- v1.0.7 新增 ADB intent 调试入口：`--ez adb_start true`、`--ez adb_stop true`、`--ez adb_open_wechat true`。
- GitHub 仓库已公开：`https://github.com/pythonzjw/visitor_qr_register`。
- v1.0.8 已修正微信图片流程：先点击聊天页图片，等待 1s，再长按大图识别二维码。
- 当前正在改 CI：tag 构建后自动把 APK 发布到 GitHub Release，避免手动下载 artifact 再上传。

# 已修改文件
- `.github/workflows/android-debug-apk.yml`：新增 `contents: write` 权限、整理 release APK 文件名、tag 构建时用 `softprops/action-gh-release@v2` 自动发布 Release APK。
- `android_app/app/src/main/java/com/zjw/visitorqrregister/VisitorAccessibilityService.java`：v1.0.8 已提交，点击图片后长按大图。
- `android_app/app/src/main/java/com/zjw/visitorqrregister/MainActivity.java`：v1.0.8 已提交，配置文案改为聊天页图片点击位置。

# 关键决策
- 独立 APK，不改现有其他项目。
- 真机测试优先，不再使用 MuMu 作为最终测试目标。
- v1.0.6 后固定签名，优先覆盖安装。
- Release APK 后续由 GitHub Actions 在 tag 上自动发布。

# 验证情况
- v1.0.7 Release 可下载：`https://github.com/pythonzjw/visitor_qr_register/releases/download/v1.0.7/visitor-qr-register-v1.0.7.apk`。
- v1.0.8 代码已推送且 CI 成功，但当时工作流未自动发布 Release APK。
- 当前工作流改动尚待提交、推送新 tag 并验证 CI/Release。

# 未完成事项
- 提交并推送 CI 自动发布改动。
- 打新 tag 触发 CI，确认 Release 页面出现新版 APK。
- 真机连接 ADB 后再做完整二维码流程测试。

# 下一步
- 提交当前 CI 改动，打 `v1.0.9` tag，推送并监控 GitHub Actions。
- 成功后给用户新版 Release APK 下载链接。

# 已知问题
- 本地 `gh auth status` 显示 GitHub token 失效；若 git push 或 gh 查询失败，需要用户重新认证或提供可用凭据。
- 若手机 ADB 未出现真机序列号，无法继续远程真机测试。
