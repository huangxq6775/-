# 声译 v0.2.2-debug 测试诊断版

[下载 ZIP 安装包及源码](https://github.com/huangxq6775/-/raw/refs/heads/downloads-v0.2.2-debug/LingoLive-v0.2.2-debug.zip)

SDK 初始化与下载接口移到后台线程，避免它们同步阻塞界面计时器。显示每秒等待时间与准备阶段，增加可复制下载诊断；网络能力、SDK 返回不代表已经发出了下载请求。保持原有翻译服务、悬浮窗及印地语等功能。

Windows 解压后取得 APK，可覆盖安装。开始后约30秒仍未就绪时，点“下载诊断 / 复制记录”，将记录用于定位。不必盲等；仍需真机验证模型下载是否恢复。

20 项 Android 测试通过，覆盖同步初始化阻塞；Lint 无问题。完整源码与服务端位于 project/。这是测试版本，非正式发布版。

APK SHA-256：c3ebd907307d637ec8a22a8f08266f7c74beedc3fbcd56321239219c8fcbac1c
