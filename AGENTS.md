# WinsRehab Agent 指南

WinsRehab 是本地单设备演示的 Android 医患康复应用，使用 Kotlin、ViewBinding、MVVM 和 Room。当前主线见开发计划。

用中文简洁沟通，优先说明结果、验证情况和阻塞点。

## 按任务读取和更新文档

先读本文件。用 `rg -n '^#{1,3} ' <文件>` 定位相关章节，按任务读取。

| 任务 | 文档及归属 |
|---|---|
| 搭环境、跑项目或测试 | [README.md](README.md) · 快速开始、演示账号、测试命令 |
| 找模块、页面、数据模型 | [项目总览](docs/项目总览.md) · 架构与代码索引 |
| 改任务、进度、UI 或导航行为 | [行为契约](docs/行为契约.md) · 已实现规则与回归判据 |
| 做录制、审核等新能力 | [开发计划](docs/开发计划.md) · 里程碑、待确认项与验收 |
| 查完成情况、测试基线或风险 | [开发进度](docs/开发进度.md) · 实际状态与最近记录 |

`AGENTS.md` 管协作规则，`CLAUDE.md` 保持转指。已实现行为变化时同步改契约，范围调整时改计划，里程碑或验证基线变化时改进度。沿用现有六份项目文档，历史查 Git。

## 开发约束

- ViewModel 经 Repository 访问数据。数据库用 `MyApp.instance.database`。响应式查询用 `Flow`，一次性操作用 `suspend`。
- 任务状态写入统一走 `TaskExecutionRepository`。跨表更新放进 `db.withTransaction`，计划进度由业务动作在同一事务内重算。页面不得直接写 `RehabPlan.progress`。
- 状态用 `data/model/Status.kt` 中的常量。任务调度走 `TaskInstanceRepository` 和 `TaskSchedule`，禁止调用已弃用的 `TaskGenerationHelper.shouldGenerateToday()`。
- 双端沿用 Navigation Component 和 BottomNavigationView。Fragment 用 Bundle / `arguments`，Activity 用 Intent extras。修改导航时核对契约中的参数兜底和缺参行为。
- UI 沿用 `res/values` 的语义资源及现有组件。改动数据页时核对加载、空、错误态和占位规则。移除资源前先确认无代码、布局或清单引用。
- Kotlin 文件使用通俗的单行 `//` 注释，说明职责或原因。沿用 `XxxActivity`、`XxxVM` / `XxxViewModel`、`XxxAdapter` 和邻近 Fragment 的命名。布局按 `activity_`、`fragment_`、`item_` 分类。每个 `@Composable` 提供对应 `@Preview`。
- 网络请求、关键成功或失败分支及导航跳转用 `Log.i` / `Log.e`。日志不得包含凭据、患者身份、诊断详情或完整视频路径。
- 视频文件落盘与数据库关联遵循计划的 M1 约束。Room 只存路径和元数据，操作文件前核对归属。
- 网页浏览使用 `/browse`，禁止使用 `mcp__claude-in-chrome__*`。`.claude/skills/` 为第三方库，禁止修改或删除。

## 验证与提交

- 修改前用 `git status --short` 确认工作区，保留用户改动。直接改、自检、提交，常规审查不派子代理。
- Android 代码改动先跑 `:app:assembleDebug`，用 `rg` 核对入口、资源和关键约束，再跑 `:app:testDebugUnitTest --rerun-tasks`。按改动范围加跑 lint 或界面测试，命令见 README。
- 纯文档改动检查链接、跨文档一致性和 `git diff --check`。
- 有意调整既有行为时，先更新契约与对应测试，再改实现。新能力验收后把实际行为加入契约。纯逻辑优先放 JVM 单测，Room 事务和页面交互用 `androidTest`。
- 每完成一个独立功能点，提交一次中文 commit，只提交当前任务文件。

## 代码入口速查

以 `app/src/main/java/com/example/winsrehab/` 为根，可用 `rg --files` 按类名定位。

- 应用入口 → `SelectLogActivity`、`DtHomeActivity`、`PtMainActivity`
- 生成与进度 → `TaskInstanceRepository`、`TaskSchedule`、`PlanProgress`
- 执行与患者统计 → `TaskExecutionRepository`、`PtRehabViewModel`
- 演示数据 → `TestDataHelper.initTaskGenerationTestData()`
