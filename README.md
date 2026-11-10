# WinsRehab - 医患双端康复管理

WinsRehab 是一个本地优先的 Android 康复管理应用：医生制定康复计划，系统按频率自动生成训练任务，患者执行并上传视频，医生审核反馈，形成康复闭环。

> 当前为个人项目阶段（v0.5-dev）：单设备、本地 Room 数据库，无后端和跨设备同步。

## 当前进度

约 **45%**（2026-11-09）。代码基线停在 2026-09-07：任务自动生成已完成，患者任务列表、视频录制与审核、放松中心待开发。详细计划见 [docs/开发计划.md](docs/开发计划.md)。

### 已实现

- 医生/患者双端登录注册、医患绑定（申请、确认、拒绝）
- 医生端患者管理、创建/激活康复计划、计划列表
- 康复计划数据层：Room 9 张表（计划、任务模板、任务实例、调整记录等）
- 任务自动生成：`daily` / `weekly` / `interval`，登录与 `onResume` 触发，自动去重
- AI 心理助手（DeepSeek）
- 示范视频列表与播放

### 待实现

- 患者端今日任务列表、完成/跳过
- CameraX 视频录制、任务关联
- 医生待审核列表、播放、通过/驳回、评语
- 反馈、通知、计划调整 UI
- 放松中心（五子棋、Media3 后台音乐）

## 技术栈

- **语言/架构**：Kotlin，MVVM + Repository
- **数据**：Room v7（9 张表），Coroutines + Flow
- **UI**：ViewBinding + Navigation Component + BottomNavigationView
- **SDK**：minSdk 26 / targetSdk 34 / compileSdk 36，Java 11
- **网络**：OkHttp + Gson，DeepSeek Chat API

## 项目结构

```
app/src/main/java/com/example/winsrehab/
├── data/
│   ├── entity/        # 数据实体
│   ├── dao/           # Room DAO
│   ├── repository/    # 业务逻辑
│   └── database/      # AppDatabase + 迁移
├── ui/
│   ├── main/
│   │   ├── doctor/    # 医生端：登录、主页、计划、患者申请
│   │   ├── patient/   # 患者端：登录、主页、绑定、心理助手
│   │   └── selectLog/ # 角色选择
│   └── theme/
├── utils/             # TaskGenerationHelper、TestDataHelper
└── MyApp.kt
```

## 运行

1. 用 Android Studio 打开项目，等待 Gradle 同步
2. 编译并运行 `app`
3. 测试账号：
   - 医生：`1` / `123456`
   - 患者：`test001` / `123456`

首次运行可用 `TestDataHelper.initTaskGenerationTestData()` 初始化演示数据。

> 心理助手需要 DeepSeek API Key。在根目录 `local.properties` 中配置 `DEEPSEEK_API_KEY=你的Key`（该文件不入库），Gradle 会通过 `BuildConfig` 注入。旧 Key 已从源码移除，但仍需在 DeepSeek 平台轮换。

## 文档

- [项目总览](docs/项目总览.md)：定位、架构、数据模型、业务流程
- [开发进度跟踪](docs/开发进度跟踪.md)：实际完成情况、开发日志、已知问题
- [开发计划](docs/开发计划.md)：4 周冲刺计划、验收脚本
- [AGENTS.md](AGENTS.md)：项目记忆与代码规范（Claude Code 通过 `CLAUDE.md` 引用）

## 已知限制

- 本地模式：医生和患者必须在同一台设备/同一个数据库切账号演示
- 密码明文存储、数据库未加密，仅用于演示
- 开发期使用 `fallbackToDestructiveMigration()`，升级会清空数据
- 无自动化测试，仅有默认单元测试样例
