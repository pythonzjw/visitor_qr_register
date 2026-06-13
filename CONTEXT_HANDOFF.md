# 当前目标
完成 `code/visitor_qr_register/` 安卓 APK：新增微信聊天页二维码图片自动校准，GitHub tag 触发 CI 自动构建并发布 Release APK，供用户下载安装到真机测试。

# 已完成
- 原生 Java Android APK 已实现：前端可设置轮询间隔、二维码图片点击位置 X/Y、启动/停止、打开无障碍、状态和成功数。
- 授权中心已接入 `visitor_qr_register`：首次机器码自动注册、30 分钟心跳、最大在线机器数量 2、签名验签与固定 debug 签名。
- v1.0.8 已修正微信图片流程：先点击聊天页图片，等待 1s，再长按大图识别二维码。
- v1.0.9 已改 CI：tag 构建后自动把 APK 发布到 GitHub Release。
- 本轮新增自动校准：App 按钮进入校准模式并打开微信，用户点击二维码图片缩略图后，无障碍服务捕获图片控件边界，保存中心点 X/Y，并延迟返回聊天页。

# 已修改文件
- `android_app/app/src/main/java/com/zjw/visitorqrregister/MainActivity.java`：按钮改为“打开微信，点击图片自动校准”；进入校准模式；回到 App 刷新 X/Y。
- `android_app/app/src/main/java/com/zjw/visitorqrregister/VisitorAccessibilityService.java`：监听微信点击事件，识别图片/方形缩略图控件，自动保存 X/Y 并退出校准。
- `android_app/app/src/main/java/com/zjw/visitorqrregister/RuntimeStore.java`：新增 `calibrating` 状态。
- `README.md`：更新自动校准使用说明。

# 关键决策
- 独立 APK，不改现有其他项目。
- 真机测试优先，不再使用 MuMu 作为最终测试目标。
- 自动校准只在用户主动点校准按钮后生效；启动自动化会取消校准状态。
- 校准会点击打开图片，因此成功保存后延迟执行一次返回，尽量回到聊天页。

# 验证情况
- 本地 `./gradlew assembleDebug` 因 Mac 缺 Java Runtime 未能执行。
- 需要通过 GitHub CI 验证本轮改动并发布下一版 APK。
- v1.0.9 Release 可下载：`https://github.com/pythonzjw/visitor_qr_register/releases/download/v1.0.9/visitor-qr-register-v1.0.9.apk`。

# 未完成事项
- 提交本轮自动校准改动，打新 tag 触发 CI 自动发布 Release APK。
- 真机连接 ADB 后做完整二维码流程测试。

# 下一步
- 推送新版本并确认 Release APK 可访问。
- 用户安装新版后：点“打开微信，点击图片自动校准”→ 在聊天页点二维码图片 → 回 App 点“启动”。

# 已知问题
- 本地 `gh auth status` 显示 GitHub token 失效；git push 仍可用，Release 验证优先用公开页面/下载链接。
- 若微信图片点击事件不暴露图片控件边界，校准可能提示继续点击，需要手动 X/Y 兜底。
