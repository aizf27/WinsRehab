# WinsRehab - 医患双端康复管理

WinsRehab 是一个**本地优先的 Android 康复管理应用**：医生制定康复计划，系统按频率自动生成训练任务，患者执行并录制视频，医生审核反馈，形成“计划 → 执行 → 审核 → 调整”的康复闭环。

> 当前是个人项目 / MVP 阶段：单设备、本地 Room 数据库，无后端、无跨设备同步。

## 现在能跑通什么

- 医生 / 患者双端登录注册、医患绑定（申请、确认、拒绝）
- 医生端患者管理、创建并激活康复计划、查看计划列表
- 任务自动生成：`daily` / `weekly` / `interval`，登录与 `onResume` 触发，自动去重
- 患者端今日任务列表接真实数据；完成任务 / 跳过任务；今日进度、本周柱状图、本周完成卡同源刷新
- 医生端患者总数、患者阶段/进度等页面数字接真实查询
- AI 心理助手（DeepSeek）、示范视频列表与播放
- 数据库 v8（10 张表）；JVM 单测 105 个，界面自动化 27 个

## 还没打通什么

MVP 闭环还差最后三段：

1. **视频录制**：CameraX 采集、预览、保存，并把视频关联到任务
2. **医生审核**：待审核列表、播放、通过 / 驳回、评语
3. **患者结果**：查看审核结果；驳回后重录

反馈、通知、后台 Worker、上传队列、计划调整 UI、放松中心（五子棋 / Media3）不属于当前闭环，放在后续 backlog。详见 [开发计划](docs/开发计划.md)。

## 技术栈

- **语言 / 架构**：Kotlin，MVVM + Repository
- **数据**：Room v8（10 张表），Coroutines + Flow
- **UI**：ViewBinding + Navigation Component + BottomNavigationView，Material Components
- **SDK**：minSdk 26 / targetSdk 34 / compileSdk 36，Java 11
- **网络**：OkHttp + Gson，DeepSeek Chat API
- **测试**：JUnit（JVM 单测）+ Espresso / UiAutomator（界面自动化）

## 运行

1. 用 Android Studio 打开项目，等待 Gradle 同步
2. 编译并运行 `app`
3. 测试账号：
   - 医生：`1` / `123456`
   - 患者：`test001` / `123456`

首次运行可用 `TestDataHelper.initTaskGenerationTestData()` 初始化演示数据。

心理助手需要 DeepSeek API Key：在根目录 `local.properties` 配置 `DEEPSEEK_API_KEY=你的Key`（该文件不入库），Gradle 通过 `BuildConfig` 注入。历史提交中的旧 Key 仍需在 DeepSeek 平台轮换。

## 测试

```bash
# JVM 单测
./gradlew.bat :app:testDebugUnitTest --rerun-tasks

# Lint
./gradlew.bat :app:lintDebug

# 界面自动化（先启动模拟器）
./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.injected.device.serial=emulator-5554"
```

界面测试跑前先唤醒模拟器；机器负载高时偶发“登录后等待 `doctorHomeRoot` 超时”，属等待时间不足，重跑即可。

## 文档

| 文档 | 用途 |
|------|------|
| [项目总览](docs/项目总览.md) | 定位、架构、数据模型、业务流程 |
| [行为契约](docs/行为契约.md) | 已实现且不得无意改坏的行为与验收口径 |
| [开发进度](docs/开发进度.md) | 实际完成情况、最近日志、已知问题 |
| [开发计划](docs/开发计划.md) | MVP 闭环路线图、后续 backlog |
| [AGENTS.md](AGENTS.md) | AI / 协作者动手前须知、代码规范 |

## 已知限制

- 本地模式：医生和患者必须在同一台设备、同一个数据库切账号演示
- 密码明文存储、数据库未加密，仅用于演示，不能直接上线
- 开发期使用 `fallbackToDestructiveMigration()`，升级会清空数据
- 无后端：没有真实权限校验、跨设备同步、云端视频存储
- 视频录制 / 审核 / 重录尚未实现，MVP 闭环未闭合
