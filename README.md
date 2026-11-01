# 声译 v0.2.1-debug 测试版

[下载 APK ZIP 与源码](https://github.com/huangxq6775/-/raw/refs/heads/downloads-v0.2.1-debug/LingoLive-v0.2.1-debug.zip)

修复模型无限准备：最多等待 2 分钟，超时显示检查与重试提示，SDK 失败显示错误码。迟到回调不会自动恢复麦克风。SDK 自身后台下载可能仍继续；此修复不代表手机网络问题已解决。请确认加速器覆盖声译与模型下载流量。

解压后 APK 可覆盖安装（与旧版相同调试签名），完整源码及服务端部署说明见 project/。保留现有悬浮窗、印地语和可选 GPT 功能，不更换翻译服务。

18 项 Android 测试通过，Lint 无问题。手机端 Google 模型下载仍需真机验收。非正式发布版。
