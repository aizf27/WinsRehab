# WinsRehab 项目记忆

**用于 Codex / Claude Code 的项目记忆，唯一事实源。`CLAUDE.md` 只做转指，不再维护副本。**

---

## 现在该做什么

- **当前主线**：MVP 闭环还差视频录制、医生审核、患者结果与重录，按 [docs/开发计划.md](docs/开发计划.md) 的 M1 → M4 推进
- 已打通：登录绑定、医生建计划、任务生成、患者今日任务、完成 / 跳过、计划进度回写
- 未实现：CameraX 录制、视频入库与任务关联、医生待审核 / 播放 / 通过驳回、患者结果 / 重录
- 界面自动化已在 Pixel_4_XL 模拟器跑通：27 个用例全绿，耗时约 116 秒
- 遗留风险：机器负载高时偶发「登录后等 `doctorHomeRoot` 超时」，属等待时间不够而非逻辑错误，重跑即可；跑前先唤醒模拟器
- 复跑：`./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.injected.device.serial=emulator-5554"`（PowerShell 必须给参数加引号，否则会被拆开）
- 流程：直接改 → 自检（L0 编译 / L1 检索断言 / L2 单测 `--rerun-tasks` / 按需 lint）→ 一次中文 commit
- 不引入额外编排流程，不派子代理做常规审查；需要时先读 [行为契约](docs/行为契约.md)，避免改坏已实现行为

---

## 项目核心

WinsRehab 是**医患双端康复管理系统**：医生制定康复计划，系统按频率自动生成训练任务，患者执行并上传视频，医生审核反馈，形成康复闭环。

**技术栈**：Kotlin + MVVM + Room（版本 8，10 张表）

**当前阶段**：本地优先、单设备演示，无后端、无跨设备同步。

---

## 架构关键点

### 分层架构

```
UI（Activity/Fragment + ViewModel）
  ↓ LiveData/Flow
Repository（业务逻辑、事务边界）
  ↓ Coroutines
DAO（数据库访问）
  ↓
Entity（10 张表）
```

- ViewModel 不直接访问 DAO，必须经过 Repository
- 任务状态变更只有一个业务入口：`TaskExecutionRepository`
- 计划进度只有一个写入口：完成 / 跳过 / 审核通过的事务内重算

### 核心数据模型

- **用户**：Doctor、Patient（bindingStatus: unbound→pending→active/rejected）
- **康复计划**：RehabPlan（draft→active→completed/cancelled）
- **任务系统**：TrainingTask（模板，频率规则）→ TaskInstance（实例，患者执行）
- **调整追踪**：PlanAdjustment（记录医生调整历史）
- **轻量反馈**：Feedback（患者留言、医生回复）
- **辅助**：PtRehabVideo、DemoVideo、DoctorTask
- `ChatMessage` 是心理助手的聊天数据类，不是 Room 表

### 关键业务流程

1. 患者登录 → 输入医生工号绑定 → 医生确认
2. 医生创建康复计划 → 添加训练任务（daily / weekly / interval）
3. 系统自动生成任务：患者登录或 `onResume` 时生成当天 TaskInstance
4. 患者完成任务、录制视频 → 任务进入待审核
5. 医生审核视频 → 患者查看结果，驳回可重录

> 第 4–5 步的录制 / 审核 / 结果尚未实现；当前完成 / 跳过已可用，但不会进入审核。

---

## 代码规范

### 注释

- 所有 Kotlin 文件必须有通俗易懂的单行注释 `//`
- 禁止长篇多行注释块；注释说明“是什么、为什么”，不重复代码逻辑

### 命名约定

- Activity：`XxxActivity.kt`
- Fragment：`xxx_fragment.kt`（小写下划线，历史遗留）
- ViewModel：`XxxVM.kt` 或 `XxxViewModel.kt`
- Adapter：`XxxAdapter.kt`
- 布局：`activity_xxx.xml`、`fragment_xxx.xml`、`item_xxx.xml`

### Commit

- 每完成一个小功能点就 commit
- message 用中文，清晰描述做了什么
- 示例：`M1 录制页接入 CameraX 预览`

### Log

- 关键节点加 `Log.i` / `Log.e`
- 格式：`Log.i("ClassName", "description: $value")`
- 用于网络请求、成功/失败分支、导航跳转

### 数据库操作

- Flow 用于响应式查询（自动更新 UI）
- suspend 用于一次性操作（插入、删除）
- 获取数据库实例：`MyApp.instance.database`
- ViewModel 不直接访问 DAO，必须经过 Repository
- 跨表状态变更放 `db.withTransaction { ... }`，不要在 Fragment 里连续调用多个 DAO

### 导航

- 医生端/患者端使用 Navigation Component + BottomNavigationView
- Fragment 间传参通过 Bundle + arguments
- Activity 间传参通过 Intent extras

### 状态与进度

- 状态常量在 `data/model/Status.kt`，不要在各页面写字符串
- 只有 `pending` 可完成 / 跳过；`waiting_review` / `rejected` 的业务链路尚未接
- 计划进度 = 已完成 ÷ 截至今天应完成数，向下取整 0–100；分母按计划日期 + 频率推算，不按已生成条数
- 今日进度是文字 + 横向 `ProgressBar`，**不是环形图**；不要为“好看”改口径

### 视频与文件（M1 起）

- 数据库只存路径，不存 blob
- 先写临时文件，事务成功后再保留；失败删临时文件
- 删除视频记录前先确认路径，避免误删其它任务文件

---

## 当前状态（2026-11-18）

### 已完成

- ✅ **UI 基建与页面接线**（2026-11-18，10 组完成）
  - 语义色 / 间距 / 圆角 / 阴影 / 文本外观取值表，双端主题（医生蓝、患者绿），统一按钮、输入框、卡片、状态徽标、信息行、顶部栏、三态与占位
  - 双端导航图重构、跨页面参数统一解析、18 个界面可达性接线、死布局 / 图片 / 字符串 / 样式清理
  - 患者首页与康复中心共用同一 `PtRehabViewModel` 周数据，今日完成数与完成率同源
  - 质量门：编译通过、单元测试 105 用例全绿、Lint 0 error；界面自动化 27 用例在模拟器全绿
- ✅ 用户系统：登录注册、医患绑定、信息管理
- ✅ 主界面框架：双端底部导航
- ✅ 康复计划数据层：4 个 Entity + DAO + Repository，数据库 v8（10 张表）
- ✅ **状态与数据规则**（2026-11-10）
  - `data/model/Status.kt`：TaskStatus / PlanStatus / VideoReviewStatus / UploadStatus / BindingStatus
  - `TaskInstance` 唯一索引 `(taskId, patientId, scheduledDate)`
  - `PtRehabVideo` 补任务关联、本地 / 远端路径、审核、上传、时长与大小字段
  - `Feedback` 表；数据库升 v8（10 张表，继续 `fallbackToDestructiveMigration()`）
  - `utils/TaskSchedule.kt` 纯函数 + `TaskScheduleTest`（10 个用例全绿）
- ✅ 医生端康复计划 UI：创建、任务管理、计划列表
- ✅ **任务自动生成**：`generateDailyTasks()`
  - daily / weekly / interval
  - 登录触发 + `onResume` 刷新
  - `isInstanceExists` 去重 + 批量插入 IGNORE
  - 频率判断已换 `TaskSchedule`，不再依赖 `lastScheduledDate`
- ✅ **患者任务执行**（2026-11-18）
  - 今日任务列表接真实 `TaskInstance`
  - 完成 / 跳过弹窗，事务更新状态、时间戳、计划进度
  - 今日进度横向条、本周柱状图、本周完成卡同源
- ✅ AI 心理助手（DeepSeek）
- ✅ 示范视频库（列表、播放）

### 待实现

- 🚧 视频录制（CameraX）、预览、保存、任务关联
- 🚧 医生审核（待审核列表、播放、通过 / 驳回、评语）
- 🚧 患者结果展示、驳回后重录
- 🚧 反馈 UI、通知、计划调整 UI
- 🚧 放松中心（五子棋、Media3 音乐）

### 重要说明

- `TaskSchedule` 纯函数、数据库 v8 已于 2026-11-10 完成；CameraX、Media3 **还没实现**
- interval 生成现在只依赖 `(task, planStartDate, date)`；旧的 `TaskGenerationHelper.shouldGenerateToday()` 已标 `@Deprecated`，不要再用
- 视频审核 / 上传 / 驳回重录的写入链路尚未实现，`PtRehabVideo` 只有字段和数据层
- 当前统计：主代码 Kotlin 101、布局 43、JVM 单测 105、界面用例 27、Lint 0 error / 284 warning

---

## 任务生成机制（现状）

### 频率规则

- **daily**：每天生成
- **weekly**：`frequencyDetail` 存 `"1,3,5"`，表示周一、三、五（1=周一，7=周日）
- **interval**：`frequencyDetail` 存 `"2"`，表示间隔 2 天

### 生成流程

```kotlin
患者登录（PtLogVM.loginPatient）
  ↓
TaskInstanceRepository.generateDailyTasks(patientId, today)
  ↓
1. 获取患者所有 active 计划
2. 遍历计划的任务模板（TrainingTask）
3. 判断频率（TaskSchedule.shouldGenerate(task, plan.startDate, date)，只依赖任务/计划开始日/日期）
4. 检查去重（isInstanceExists）
5. 批量插入 TaskInstance（OnConflictStrategy.IGNORE + 唯一索引兜底）
```

### 触发点

- 患者登录成功后（`PtLogVM`）
- 应用恢复时（`PtMainActivity.onResume`）

### 去重保护

- 数据库级别：`TaskInstance` 唯一索引 `(taskId, patientId, scheduledDate)` + `OnConflictStrategy.IGNORE`
- 业务级别：`isInstanceExists` 查询

### 已知局限

- `TaskSchedule`、`PlanProgress`、状态映射有 JVM 单测；任务执行事务有 androidTest
- 视频 / 审核尚未实现，没有对应测试
- 生成仍由登录 / `onResume` 触发，后台 Worker 在 backlog
- 只生成当天任务，不补生成历史（漏了就是漏了）

---

## 常见问题

### 1. ViewModel 中获取数据库

❌ 错误：`AppDatabase.getDatabase()`
✅ 正确：`MyApp.instance.database`

### 2. Repository 构造函数

`TaskInstanceRepository` 需要 3 个 DAO：

```kotlin
TaskInstanceRepository(
    taskInstanceDao,
    rehabPlanDao,
    trainingTaskDao
)
```

`TaskExecutionRepository` 需要 `AppDatabase`（事务边界），其余 DAO 默认从 `db` 取，测试可注入装饰器：

```kotlin
TaskExecutionRepository(
    db = MyApp.instance.database
)
```

### 3. 数据库迁移

当前开发阶段使用 `fallbackToDestructiveMigration()`，发布前需编写 Migration 脚本。

### 4. 冗余字段更新

Patient 表中的 `todayTasksCompleted`、`weeklyCompletionRate` 等统计字段需要在相关操作后手动更新。

### 5. 安全债（发布前必须处理）

- 密码当前**明文**存储，未哈希
- DeepSeek API Key 已改从 `local.properties` / 环境变量经 `BuildConfig` 注入；历史提交中的旧 Key 仍需轮换
- Room 数据库未加密

### 6. 第三方技能库

`.claude/skills/` 是独立的第三方技能库，不属于项目代码，不要修改或删除。

### 7. 不要绕过业务入口

- 不要在 Fragment / ViewModel 里直接 `copy(status = ...)` 改任务状态
- 不要直接调用 DAO 改 `RehabPlan.progress`
- 完成 / 跳过 / 审核通过统一走 `TaskExecutionRepository`，保证事务与进度口径一致

---

## 文档索引

| 文档 | 用途 |
|------|------|
| [README.md](README.md) | 仓库首页、快速开始 |
| [docs/项目总览.md](docs/项目总览.md) | 定位、架构、数据模型、业务流程 |
| [docs/行为契约.md](docs/行为契约.md) | 已实现行为与回归判据 |
| [docs/开发进度.md](docs/开发进度.md) | 实际完成情况、最近 3 条日志、已知问题 |
| [docs/开发计划.md](docs/开发计划.md) | MVP 闭环路线图与 backlog |

**文档规则**：只维护以上 5 份项目文档 + 本文件，不新增按日期或阶段命名的临时文档；计划调整改 `docs/开发计划.md`，已实现行为变化同步改 `docs/行为契约.md`。

---

## 启动入口

`SelectLogActivity` → 医生/患者登录 → 各自主界面

- 医生主界面：`DtHomeActivity`（首页、患者管理、个人信息）
- 患者主界面：`PtMainActivity`（首页、康复训练、个人信息）

---

## 测试账号

**测试患者**：test001 / 123456
**测试医生**：1 / 123456

使用 `TestDataHelper.initTaskGenerationTestData()` 可初始化任务生成测试数据。

---

**最后更新**：2026-11-18
