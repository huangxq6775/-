# 声译 v0.2.0-debug 测试版（非正式发布版）

[下载 ZIP 安装包及部署源码](https://github.com/huangxq6775/-/raw/refs/heads/downloads-v0.2.0-debug/LingoLive-v0.2.0-debug.zip)

Windows 下载后右键 ZIP → 全部解压。根目录 APK 支持 Android 8.0+，调试签名与旧版一致。完整应用及 Python 服务端源码位于 project/。

新增可移动、缩放的悬浮窗，可调大小、明暗、字体、双语显示。麦克风由前台服务持有，有通知和停止入口。支持印地语等八种语言。

GPT 校准默认关闭，需先按 project/server/README.md 部署 HTTPS 服务并在服务器配置 OpenAI 密钥；手机只填写服务访问令牌。失败保留本机译文。真实语音、后台行为和真实 GPT 校准需真机及部署后验证。

构建及 10 项 Android 测试、10 项服务端测试通过，Android Lint 无问题。服务端测试使用替身上游，不代表 OpenAI 已上线。

APK SHA-256：aaccdcfacf62cc8647ca936b68cd4f6e1f9be1502a20dfd961ddd8cb756a5bbd

此分支仅供下载，不修改 main。
