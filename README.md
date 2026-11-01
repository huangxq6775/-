# 声译 · LingoLive

原生 Android 语音翻译应用（Java），支持边说边显示识别文字、翻译中间结果与完整句子。

## 功能

- 中文、英语、日语、韩语、法语、德语、西班牙语、**印地语（हिन्दी / hi-IN）**互译。
- 系统 SpeechRecognizer 语音输入，开启中间识别结果；完成一句后自动继续聆听。
- Google ML Kit 端侧翻译：首次使用下载语言模型，下载后翻译不需要联网、API 密钥或自建服务器。
- 系统 TextToSpeech 朗读目标语言，朗读前暂停麦克风，避免重复识别自身播放的语音。
- 一键切换语言、复制译文、本机最近 30 条完整翻译记录及清空功能。
- 切换语言或离开界面时停止采集；过期的异步翻译不会覆盖新语言或新句子的结果。

## 构建

Android Studio 打开本目录，安装 Android SDK 35 / Build Tools 35.0.0，使用 JDK 17 或 21。

```bash
./gradlew assembleDebug testDebugUnitTest lintDebug
```

如 SDK 未自动发现，在本机的 `local.properties` 中设置 `sdk.dir`（此文件不提交）。

调试安装包：`app/build/outputs/apk/debug/app-debug.apk`。
连接手机并开启 USB 调试后：

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

应用兼容 Android 8.0（API 26）及以上。当前为调试版本，正式上架前需配置发布签名。

## 真机使用与验收

1. 在系统设置中启用语音识别服务，选择源语言与目标语言，点击「开始实时翻译」，允许麦克风权限。
2. 首次下载翻译模型时保持联网。中文 → 印地语可说「你好，请问车站在哪里」，确认源文与天城文译文出现。
3. 等待一句结束，再继续说下一句，检查聆听自动恢复且记录新增。
4. 选择印地语 → 中文，说「नमस्ते, आप कैसे हैं?」，验证印地语输入；点击朗读验证中文语音输出。
5. 点击停止，确认不再采集；切换语言后确认旧译文不会回写；离开应用确认麦克风关闭。
6. 测试拒绝麦克风权限、断网下载模型、缺少印地语识别/朗读语音包等情况，确认有可操作的错误提示。
7. 点击清空，重启应用，确认本机历史已删除。

## 运行条件与限制

- 语音识别由手机的系统服务提供，不同品牌的支持语言、联网要求、中间结果频率和断句速度不同。若服务不提供中间结果，将按句更新译文。连续模式在句间会短暂重启识别，并非专业会议用的无间断音频流。
- 手机须有支持印地语的识别服务与朗读语音包。应用会报告不支持或语言数据缺失，不会用假数据替代结果。
- ML Kit 非英语语言之间的翻译可能经英语中转；精度、延迟与专业云翻译不同，医疗等高精度场景应人工复核。
- 音频交由系统识别服务处理，具体隐私政策取决于手机所用服务。本应用不保存原始音频；仅把完成的文本翻译保存在应用私有存储，禁用 Android 自动备份。
- 本项目不包含后台录音、悬浮窗翻译、屏幕翻译或应用商店发布流程。

## 验证范围

自动检查覆盖构建、Android Lint 及异步结果版本控制。语音识别、模型下载、译文质量和朗读需按以上步骤在有语音服务的真机上验证。

## 当前 Codex 云环境

Android SDK 位于 `/workspace/android-tools/sdk`，完整 JDK 位于 `/workspace/android-tools/jdk`。
在仓库 `/workspace/-` 中执行以下命令可使用环境的 HTTPS 代理及系统 CA 构建：

```bash
JAVA_HOME=/workspace/android-tools/jdk \
JAVA_TOOL_OPTIONS='-Dhttps.proxyHost=proxy -Dhttps.proxyPort=8080 -Dhttp.proxyHost=proxy -Dhttp.proxyPort=8080 -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts' \
./gradlew --no-daemon assembleDebug testDebugUnitTest lintDebug
```

`local.properties` 已在当前实例配置 SDK 路径，不包含密钥，不纳入版本控制。
云环境未连接 Android 真机，也没有可用于验证真实语音的麦克风或识别服务。
