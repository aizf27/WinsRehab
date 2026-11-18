# WinsRehab 项目记忆

**用于 Codex / Claude Code 的项目记忆，唯一事实源。`CLAUDE.md` 只做转指，不再维护副本。**

---

## 进行中的变更（新会话先看这里）

- **无进行中变更**。`ui-foundation-and-page-wiring`（UI 基建 + 页面接线）已归档，归档件在 `openspec/changes/archive/2026-11-18-ui-foundation-and-page-wiring/`，主 spec 已同步（新建 `ui-foundation`、`ui-page-wiring`）
- 分支 `change/ui-foundation-and-page-wiring` 与 worktree `.worktree/ui-foundation-and-page-wiring/ws` 已删除；组 1–10 全部 `--no-ff` 合回 `dev_week1`
- 下一步按 [docs/开发计划.md](docs/开发计划.md) 推进：视频录制（CameraX）+ 任务关联、医生审核（播放/通过/驳回/评语）、反馈与通知、计划调整 UI、放松中心（五子棋/Media3）
- 界面自动化已在 Pixel_4_XL 模拟器跑通：27 个用例（17 界面 + 10 非界面）全绿，耗时约 116 秒
- 遗留风险：机器负载高时偶发「登录后等 `doctorHomeRoot` 超时」，属等待时间不够而非逻辑错误，重跑即可；跑前先唤醒模拟器
- 复跑：`./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.injected.device.serial=emulator-5554"`（PowerShell 必须给参数加引号，否则会被拆开）
- 流程：**不走 openspec 编排**，不再派子代理审查。每个功能点 = 直接改 → 自检（L0 编译 / L1 检索断言 / L2 单测 `--rerun-tasks` / 按需 lint）→ 一次中文 commit

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
Repository（业务逻辑）
  ↓ Coroutines
DAO（数据库访问）
  ↓
Entity（10 张表）
```

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
- 示例：`W1D3 患者今日任务列表接真实数据`

### Log
- 关键节点加 `Log.i` / `Log.e`
- 格式：`Log.i("ClassName", "description: $value")`
- 用于网络请求、成功/失败分支、导航跳转

### 数据库操作
- Flow 用于响应式查询（自动更新 UI）
- suspend 用于一次性操作（插入、删除）
- 获取数据库实例：`MyApp.instance.database`
- ViewModel 不直接访问 DAO，必须经过 Repository

### 导航
- 医生端/患者端使用 Navigation Component + BottomNavigationView
- Fragment 间传参通过 Bundle + arguments
- Activity 间传参通过 Intent extras

---

## 当前状态（2026-11-18）

### 已完成（约 60%）
- ✅ **UI 基建与页面接线**（2026-11-18，10 组全部完成并归档）
  - 语义色/间距/圆角/阴影/文本外观取值表，双端主题（医生蓝、患者绿），统一按钮、输入框、卡片、状态徽标、信息行、顶部栏、三态与占位
  - 双端导航图重构、跨页面参数统一解析、18 个界面可达性接线、死布局/图片/字符串/样式清理
  - 患者首页与康复中心共用同一 `PtRehabViewModel` 周数据，今日完成数与完成率同源
  - 质量门：编译通过、单元测试 105 用例全绿、Lint 0 error；界面自动化 27 用例在模拟器全绿（收尾4 修正匹配器后）
- ✅ 用户系统：登录注册、医患绑定、信息管理
- ✅ 主界面框架：双端底部导航
- ✅ 康复计划数据层：4 个 Entity + DAO + Repository，数据库 v7（W1D1 升 v8）
- ✅ **W1D1 数据与规则**（2026-11-10）
  - `data/model/Status.kt`：TaskStatus / PlanStatus / VideoReviewStatus / UploadStatus / BindingStatus
  - `TaskInstance` 唯一索引 `(taskId, patientId, scheduledDate)`
  - `PtRehabVideo` 补任务关联、本地/远端路径、审核、上传、时长与大小字段
  - `Feedback` 表；数据库升 v8（10 张表，继续 `fallbackToDestructiveMigration()`）
  - `utils/TaskSchedule.kt` 纯函数 + `TaskScheduleTest`（10 个用例全绿）
- ✅ 医生端康复计划 UI：创建、任务管理、计划列表
- ✅ **任务自动生成**：`generateDailyTasks()`
  - daily / weekly / interval
  - 登录触发 + `onResume` 刷新
  - `isInstanceExists` 去重 + 批量插入 IGNORE
  - 频率判断已换 `TaskSchedule`，不再依赖 `lastScheduledDate`
- ✅ AI 心理助手（DeepSeek）
- ✅ 示范视频库（列表、播放）

### 待实现（约 40%）
- 🚧 患者端任务完成/跳过交互与状态流转（今日任务列表已接真实数据，录制入口暂置灰）
- 🚧 视频录制（CameraX）、任务关联
- 🚧 医生审核（播放、通过/驳回、评语）
- 🚧 反馈、通知、计划调整 UI
- 🚧 放松中心（五子棋、Media3 音乐）

### 重要说明
- 代码基线停在 **2026-09-07**，2026-11-09 重启，执行 [docs/开发计划.md](docs/开发计划.md)
- `TaskSchedule` 纯函数、数据库 v8 已于 W1D1 完成；CameraX、Media3 **还没实现**
- interval 生成现在只依赖 `(task, planStartDate, date)`；旧的 `TaskGenerationHelper.shouldGenerateToday()` 已标 `@Deprecated`，不要再用

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
3. 判断频率（`TaskSchedule.shouldGenerate(task, plan.startDate, date)`，只依赖任务/计划开始日/日期）
4. 检查去重（`isInstanceExists`）
5. 批量插入 TaskInstance（OnConflictStrategy.IGNORE + 唯一索引兜底）
```

### 触发点
- 患者登录成功后（`PtLogVM`）
- 应用恢复时（`PtMainActivity.onResume`）

### 去重保护
- 数据库级别：`TaskInstance` 唯一索引 `(taskId, patientId, scheduledDate)` + `OnConflictStrategy.IGNORE`
- 业务级别：`isInstanceExists` 查询

### 已知局限
- `TaskSchedule` 已有 JVM 单测；状态流转、计划进度、去重仍靠手测
- 生成仍由登录 / `onResume` 触发，后台 Worker 在 W3
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

---

## 文档索引

| 文档 | 用途 |
|------|------|
| [README.md](README.md) | 仓库首页、快速开始 |
| [docs/项目总览.md](docs/项目总览.md) | 定位、架构、数据模型、业务流程 |
| [docs/开发进度跟踪.md](docs/开发进度跟踪.md) | 实际完成情况、历史日志、已知问题 |
| [docs/开发计划.md](docs/开发计划.md) | 唯一的 4 周开发计划与验收脚本 |

**文档规则**：只维护以上 4 个文档，不再新增按日期或阶段命名的临时文档；计划调整直接改 `docs/开发计划.md`。

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
