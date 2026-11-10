# 任务清单：W1D2 查询与事务 + W1D3 患者康复页

> 各组可独立实施，组内全部完成后编译通过、可启动、测试通过。
> 序号即实施顺序：1→2→3 为数据与事务层，4 为共享日期口径，5→6 为患者康复页消费层。
> 跨组重复出现的源文件及互不重叠的改动区域（组间严格按序号实施，不并行）：
> - `TaskInstanceRepository.kt`：1.4（`generateDailyTasks` 生成区段）、3.2（状态管理区段的完成/跳过）、4.2（统计/辅助区段的周统计与私有日期助手）。
> - `TaskInstanceDao.kt`：2.2（新增按计划窗口统计已完成数）、5.2（新增本周区间联表查询）。
> - `TaskExecutionRepository.kt`：3.1（完成/跳过事务）、5.3（康复页查询）。
> 质量门命令统一为 `.\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest`（androidTest 用例在模拟器可用时追加 `.\gradlew.bat :app:connectedDebugAndroidTest`）。

## 1. 计划起止日期规则与每日生成时间窗

覆盖关联场景：任务生成入口 A（患者登录 `PtLogVM.kt:59`）与入口 B（`PtMainActivity.kt:88` 的 `onResume`）共用同一生成函数，改一处两入口同时受约束；计划保存入口（草稿与直接激活共用 `savePlan`）。

- [x] 1.1 新增 `utils/PlanDates.kt` 纯函数：`isStartDateValid(startDate, today)`（解析失败视为非法）、`calculateEndDate(startDate, durationDays)`（开始日 + 周期天数 - 1，解析失败返回 null）、`isWithinPlanWindow(startDate, endDate, date)`（解析失败返回 false），并加单行注释说明「为什么 -1」 [spec:task-execution#康复计划起止日期规则]
- [x] 1.2 `CreateRehabPlanViewModel.savePlan` 接入日期规则：开始日早于今天即 `Result.failure("开始日期不能早于今天")` 并返回；私有 `calculateEndDate` 改调 `PlanDates`，null 时返回「开始日期格式非法」；草稿与激活两条保存路径共用同一分支，加 `Log.i` 记录拦截与保存结果 [spec:task-execution#康复计划起止日期规则]
- [x] 1.3 `CreateRehabPlanActivity` 日期选择器把 `minDate` 设为今天（即时反馈的第二道，判定仍以保存校验为准），并确保选中后被校验拒绝时提示可见 [spec:task-execution#康复计划起止日期规则]
- [x] 1.4 `TaskInstanceRepository.generateDailyTasks` 增加计划级时间窗：解析 `plan.endDate` 后，目标日期早于开始日或晚于结束日则记录日志并 `continue` 跳过该计划；时间窗判断前置于任务模板查询与频率判断；两个触发入口不改调用点 [spec:task-execution#每日任务生成的时间窗规则]
- [x] 1.5 新增 JVM 单测 `PlanDatesTest`（周期天数 1/7、开始日等于今天/明天/昨天、非法日期、窗口含两端/出窗）与 `GenerateWindowTest`（结束日当天在窗、到期出窗、未开始出窗、结束日非法返回 false）；「同日期重复生成不重复入库」依赖唯一索引，放到 3.3 的 Room 测试 [spec:task-execution#每日任务生成的时间窗规则]
- [x] 1.6 `TestDataHelper.initTaskGenerationTestData()` 的种子计划改为相对今天（`startDate = 今天`、`endDate = 今天 + 29`），保证加时间窗后 `test001` 当天仍能生成任务 [spec:task-execution#每日任务生成的时间窗规则]
- [x] 1.7 质量门：编译 + 单测全绿，确认 `TaskScheduleTest` 原有 10 个用例未受影响 [infra]

## 2. 计划进度应完成数口径

覆盖关联场景：应完成数只由计划起止日期与任务频率推算，是「进度不因漏生成而虚高」的口径基础。

- [x] 2.1 新增 `utils/PlanProgress.kt` 纯函数：`computeDueCount(tasks, planStartDate, planEndDate, today)` 在 `[开始日, min(今天, 结束日)]` 上逐日复用 `TaskSchedule.shouldGenerate` 累加，窗口为空返回 0，窗口超过 3660 天按 3660 截断；`computePercent(completed, due)` 向下取整并 `coerceIn(0, 100)`、`due <= 0` 返回 0 [spec:task-execution#应完成数按计划排期推算]
- [x] 2.2 `TaskInstanceDao` 新增按计划窗口统计已完成数的只读查询：`task_instance INNER JOIN training_task ON taskId`，条件 `planId` + `patientId` + `status = 'completed'`，且 `scheduledDate BETWEEN :startDate AND :endDate`（窗口 = 计划开始日 ~ min(今天, 结束日)，与分母同窗口），命名与注释说明分子口径 [spec:task-execution#计划完成进度计算口径]
- [x] 2.3 新增 JVM 单测 `PlanProgressTest`：daily 单日计数、weekly 命中星期、interval 从计划开始日锚定、今天早于开始日为 0、今天晚于结束日按结束日截断、漏生成时分母不缩小、`computePercent(0, due) == 0`、百分比向下取整与 0/100 上下界、3660 截断 [spec:task-execution#应完成数按计划排期推算]
- [x] 2.4 质量门：编译 + 单测全绿（`PlanProgressTest` 与 `TaskScheduleTest` 同时通过） [infra]

## 3. 任务完成/跳过事务与计划进度回写

覆盖关联场景：现有 `completeInstance`/`skipInstance` 无外部调用方，迁移到新执行链路；医生计划列表 `RehabPlanAdapter.kt:60` 只读 `rehab_plan.progress`，自动受益。

- [x] 3.1 新增 `data/repository/TaskExecutionRepository.kt`（构造 `(db, instanceDao, trainingTaskDao, planDao)`，DAO 默认从 `db` 取、测试可注入；事务用 `db.withTransaction`）：`completeTask(instanceId, today = LocalDate.now())` / `skipTask(...)` 执行「读实例 → **非 `pending` 早退** → `updateInstanceStatus` 更新 `status` 与 `completedAt` → 经 `taskId` 查 `TrainingTask` 得 `planId` → 取同计划任务模板与窗口内已完成数 → `PlanProgress` 重算 → `RehabPlanDao.updatePlanProgress`」，失败整体回滚；`TrainingTask`/`RehabPlan` 缺失或日期非法时 `Log.e` + 跳过重算、保留状态更新；关键节点加 `Log.i`/`Log.e` [spec:task-execution#计划进度重算挂载点]
- [x] 3.2 删除 `TaskInstanceRepository` 的 `completeInstance`/`skipInstance` 及 5 个无调用方的今日查询方法（`getTodayInstances`、`getTodayInstancesOnce`、`getInstancesByDate`、`getTodayTaskCount`、`getTodayCompletedCount`），并删除随之失去唯一调用方的 `TaskInstanceDao` 查询（`getInstancesByDate`、`getInstancesByDateOnce`、`getTodayTaskCount`、`getTodayCompletedCount`）；删除 `RehabPlanRepository.updateProgress` 这个无调用方包装，使计划进度只有 `TaskExecutionRepository` 一个写入口；`TaskInstanceDao.updateInstanceStatus` 保留并由 3.1 复用 [spec:task-execution#任务完成与跳过状态流转]
- [x] 3.3 新增 Room 内存库 instrumented 测试（`app/src/androidTest`）：完成一项 → `status = completed`、`completedAt` 非空、`rehab_plan.progress = 50`；跳过一项 → `status = skipped` 且 `completedAt` 非空（操作时间戳）、进度不变（今天全跳过为 `0/N`）；分别构造 `waiting_review`/`rejected` 实例调用 `completeTask`/`skipTask`，断言 `status`、`completedAt`、`progress` 全部不变；对不存在的 `instanceId` 调用时状态与进度都不变；用构造传入的抛异常 `instanceDao` 装饰器触发中途失败，断言状态与进度同回滚；重复调用两次幂等；同日期重复生成不重复入库（唯一索引） [spec:task-execution#任务执行到计划进度的业务链路]
- [x] 3.4 新增 instrumented 测试：`waiting_review`/`rejected` 状态任务不计入分子；视频提交路径（关联视频改变状态）后 `progress` 保持不变；并断言医生计划列表数据源读到的 `progress` 字段值与写入值一致（适配器无需改动） [spec:task-execution#计划进度重算挂载点]
- [x] 3.5 质量门：编译 + 单测全绿 + androidTest 源集编译通过（模拟器可用时执行 `connectedDebugAndroidTest`） [infra]

## 4. 本周统计区间修正

覆盖关联场景：`TaskInstanceRepository.getWeekDateRange`（L325）被 `getWeekInstances`(L104)、`getWeekInstancesOnce`(L112)、`getWeekTaskCount`(L260)、`getWeekCompletedCount`(L268) 四处共用，本组一次改完，不按入口拆分。

- [x] 4.1 新增 `utils/DateRanges.kt`：`weekRange(today: LocalDate)` 用 `previousOrSame(DayOfWeek.MONDAY)` 起始、`plusDays(6)` 结束；提供字符串重载（内部 `TaskSchedule.parseDate`，失败回退今天）并统一格式化为 `yyyy-MM-dd`，注释说明「周一至周日、不随设备区域变化」 [spec:patient-rehab#本周完成柱状图与周统计区间]
- [x] 4.2 删除 `TaskInstanceRepository` 的 5 个无调用方周统计方法（`getWeekInstances`、`getWeekInstancesOnce`、`getWeekTaskCount`、`getWeekCompletedCount`、`getWeekCompletionRate`）与私有 `getWeekDateRange()`/`getTodayDate`，并删除随之失去唯一调用方的 `TaskInstanceDao` 查询（`getInstancesByDateRange`、`getInstancesByDateRangeOnce`、`getWeekTaskCount`、`getWeekCompletedCount`），清除 `Calendar`/`SimpleDateFormat` 相关 import；康复页周统计改由 5.3 基于 `DateRanges.weekRange` 与本周区间联表查询计算 [spec:patient-rehab#本周完成柱状图与周统计区间]
- [x] 4.3 新增 JVM 单测 `DateRangesTest`：周一当天、周日当天（本周最后一天而非下周）、跨月、跨年、含时分或空串的非法输入回退今天 [spec:patient-rehab#本周完成柱状图与周统计区间]
- [x] 4.4 质量门：编译 + 单测全绿，并全仓检索确认无 `Calendar`/`SimpleDateFormat` 周区间残留调用 [infra]

## 5. 康复页今日任务数据与状态基础

覆盖关联场景：今日任务需带任务名称/时长/优先级，用联表查询模型避免 N+1；康复页状态由 ViewModel 统一输出。

- [x] 5.1 新增 `data/model/TaskInstanceWithTask.kt`：`@Embedded` 的 `TaskInstance` + 投影字段 `taskName`/`suggestedDuration`/`priority`（只投影列表真实消费的字段，不引入无读者的 `taskType`/`planName`/`planId`），注释说明不落库 [spec:patient-rehab#今日任务列表展示真实任务]
- [x] 5.2 `TaskInstanceDao` 新增本周区间联表查询（`task_instance` + `training_task`，不 JOIN `rehab_plan`，因为不投影 `planName`/`planId`；`patientId` + `scheduledDate BETWEEN 周区间` 条件，`CASE` 让 `priority = 'important'` 在前、`createdAt` 升序），返回 `Flow<List<TaskInstanceWithTask>>`；今日列表取其中今天的子集，柱状图按日期分 7 桶，两者同源 [spec:patient-rehab#今日任务列表展示真实任务]
- [x] 5.3 `TaskExecutionRepository` 新增康复页查询：暴露 5.2 的本周区间联表查询；`today` 与 `weekRange(today)` 在每次订阅时求值（`flow { emitAll(...) }` 或 `onStart`），不缓存；在此基础上今日列表取今天子集、今日进度由该子集计数、本周每日统计按 `scheduledDate` 分 7 桶（每桶含总数与已完成数，无任务为 `0/0`）、本周汇总为 7 桶之和，四处同源 [spec:patient-rehab#本周完成柱状图与周统计区间]
- [x] 5.4 新增 `ui/main/patient/main/PtRehabViewModel.kt`（`ViewModel()` 无构造参数，内部用 `MyApp.instance.database` 组装执行仓库；暴露 `load(patientId)`）与 `RehabUiState`（`Loading`/`Content`/`Error`，`Error` 由 Flow `catch` 产出）：`patientId` 由 Fragment 按 `pt_infoFragment.resolveAccount()` 同款兜底链解析（`args.account` 非空优先，否则 `requireActivity().intent.getStringExtra("account")`），为空直接进 Error 态、不调 `load()`；`today`/`weekRange` 在每次订阅时求值，`repeatOnLifecycle` 重新收集与 `retry()` 都重算；由本周区间联表 Flow 派生 `StateFlow<RehabUiState>`（今日子集/今日进度/7 桶/周汇总同源），并提供动作入口 `completeTask(instanceId)` / `skipTask(instanceId)` / `retry()`（`retry()` 重新订阅查询 Flow；`viewModelScope` 调执行仓库）；ViewModel 只经 Repository 访问数据（遵守 AGENTS.md 分层） [spec:patient-rehab#康复页数据刷新链路]
- [x] 5.5 新增 `ui/main/patient/main/TaskStatusStyle.kt` 纯映射（任务状态 → 视觉类型 + 是否可操作，`pending` 才可操作）与 `ui/main/patient/adapter/TaskInstanceAdapter.kt`、`res/layout/item_task_instance.xml`：渲染任务名称、建议时长、优先级、状态视觉与「开始」入口显隐，点击回调带出实例 id [spec:patient-rehab#今日任务完成与跳过交互]
- [x] 5.6 新增 JVM 单测 `TaskStatusStyleTest`（五种状态→视觉与可操作性、未知状态兜底）与 instrumented 测试（联表查询返回字段齐全且 `important` 排序在前、本周 7 桶统计含某天 `0/0`、周汇总等于 7 桶之和、日期变化后重新订阅 7 桶区间随之变化） [spec:patient-rehab#本周完成柱状图与周统计区间]
- [x] 5.7 质量门：编译 + 单测全绿 + androidTest 源集编译通过 [infra]

## 6. 康复页渲染与完成/跳过交互

覆盖关联场景：今日列表、今日进度、本周柱状图、底部统计卡四处同一份数据源；环形图区域保持占位。

- [x] 6.1 `fragment_pt_rehab.xml`：删除 4 个 `include item_exercise_task`（L280-283）并删除无使用方的 `res/layout/item_exercise_task.xml`；改为 `RecyclerView`（`rvTodayTasks`，`layout_height="wrap_content"` + `android:nestedScrollingEnabled="false"`，父容器是 `NestedScrollView`）；补空态 `tvEmpty`、加载 `loadingView`、错误 `errorView` + `btnRetry` 视图；保留 `tvProgressCount`/`tvProgressPercent`/`progressBarToday`/`weekBar1..7`/`statCard1..3` 的 id 与结构，进度卡右上角图标区作为环形进度占位不改动 [spec:patient-rehab#今日进度显示]
- [x] 6.2 重写 `pt_rehabFragment` 的数据装配：`repeatOnLifecycle(STARTED)` 收集 `RehabUiState`，渲染今日列表、今日进度（`x/y`、百分比、横向条 progress）、本周柱状图 7 天（柱高按完成比例、今天桶 `tvDay` 文案为「今天」并可高亮）与统计卡（「本周完成」真实值、「累计训练」「连续打卡」显示占位 `--`），删除全部写死模拟数据 [spec:patient-rehab#底部统计卡数据来源]
- [x] 6.3 完成/跳过交互：仅 `pending` 任务显示「开始」，点击弹 `AlertDialog`（完成 / 跳过 / 取消），选择后回调 `PtRehabViewModel.completeTask(instanceId)` / `skipTask(instanceId)`（Fragment 不直接碰 Repository），取消不做任何变更；非 `pending` 任务不再弹框（含快速连点） [spec:patient-rehab#今日任务完成与跳过交互]
- [x] 6.4 空态与异常态：当天无任务显示 `tvEmpty` 空态与 `0/0`、0%；加载中显示 `loadingView`；出错显示 `errorView` 且 `btnRetry` 触发 `PtRehabViewModel.retry()`；页面不崩溃 [spec:patient-rehab#今日任务列表展示真实任务]
- [x] 6.5 新增 `ui/main/patient/main/RehabFormatter.kt` 纯函数（进度文案、柱高比例且总数为 0 为 0、占位符、空态文案、今天标记）并补 JVM 单测，百分比取整复用 `PlanProgress.computePercent`；状态→视觉与可操作性归 `TaskStatusStyle`（5.5），此处不重复实现；Fragment 只做赋值 [spec:patient-rehab#本周完成柱状图与周统计区间]
- [x] 6.6 质量门：`.\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest` 全绿，并全仓检索确认康复页已无写死任务/进度/统计数值残留、本周柱状图有且仅有一个「今天」标记 [infra]
