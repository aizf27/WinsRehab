# WinsRehab — 医患康复管理

WinsRehab 是一个本地优先的 Android 应用。医生制定康复计划，应用按频率生成训练任务，患者在康复中心查看并执行。当前可在同一设备切换医患账号，演示绑定、建计划、生成任务、完成和跳过。完整进度见 [开发进度](docs/开发进度.md)。

## 快速开始

1. 在 Android Studio 安装 Android SDK 36，打开项目并同步 Gradle。运行 `app` 时使用 API 26 或以上的模拟器或设备。
2. 首次运行可用内置医生账号 `1 / 123456`。在患者端注册测试账号，用医生工号 `1` 发起绑定，再切到医生端确认。
3. 如需使用心理助手，在不入库的 `local.properties` 中配置 `DEEPSEEK_API_KEY=你的Key`。

开发调试时也可显式调用 `TestDataHelper.initTaskGenerationTestData()` 预置患者 `test001 / 123456` 和演示计划。应用启动时只初始化测试医生。

## 测试

在仓库根目录用 PowerShell 执行。

```powershell
./gradlew.bat :app:assembleDebug
./gradlew.bat :app:testDebugUnitTest --rerun-tasks
./gradlew.bat :app:lintDebug
./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.injected.device.serial=emulator-5554"
```

界面测试前启动并唤醒模拟器。按实际设备调整 serial，保留参数外的双引号。已知偶发超时见 [开发进度](docs/开发进度.md#已知问题与风险)。

## 演示边界

医患共享同一安装实例中的 Room 数据库。当前版本限演示使用，使用测试账号与虚构患者数据。上线前的安全与迁移要求见 [开发进度](docs/开发进度.md#已知问题与风险)。

## 文档

- [项目总览](docs/项目总览.md) · 架构、数据模型与页面入口
- [开发计划](docs/开发计划.md) · MVP 里程碑与验收
- [开发进度](docs/开发进度.md) · 已实现能力、验证记录与风险
- [行为契约](docs/行为契约.md) · 回归判据
- [AGENTS.md](AGENTS.md) · Agent 开发规则与按任务读取路线
