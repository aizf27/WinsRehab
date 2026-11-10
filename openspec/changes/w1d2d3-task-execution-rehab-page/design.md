# 设计：W1D2 查询与事务 + W1D3 患者康复页

## Context

动机见 `proposal.md` 的 Why 与 What Changes；行为契约见 `specs/task-execution/spec.md` 与 `specs/patient-rehab/spec.md`。本节只记录解释技术方案所必需的现状与约束。

现状（代码事实）：

- 患者康复页 `ui/main/patient/main/pt_rehabFragment.kt` 完全由写死数据驱动：今日进度固定 `2/4` / `50%`（L41-L43），今日列表由 4 个 `include item_exercise_task` 拼成（`fragment_pt_rehab.xml:280-283`），本周柱状图 7 个写死 `WeekData`（L112-L120），统计卡固定「28 天 / 19-28 / 7 天」（L159-L165）。进度卡片内只有文字 + `ProgressBar`（横向）+ 一个图标占位，没有环形进度控件（全仓检索 `MultiTaskRingView` 无命中）。
- 任务生成 `data/repository/TaskInstanceRepository.kt:132` 只判断「计划开始日 ≤ 目标日」（经 `TaskSchedule.shouldGenerate` 内的 `date.isBefore(planStartDate)`），**不看计划结束日**，因此已到期计划仍会持续生成任务。
- 本周区间 `TaskInstanceRepository.kt:325` 用 `java.util.Calendar`：`calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)` 在默认周起始为周日的区域设置下，会把「本周周一」推到下一周，周日当天取到的区间整体偏移到下周，周日被排除在本周之外；该区间被 `getWeekInstances`(L104)、`getWeekInstancesOnce`(L112)、`getWeekTaskCount`(L260)、`getWeekCompletedCount`(L268) 四处消费。
- 计划进度字段 `rehab_plan.progress`（`data/entity/RehabPlan.kt:35`，Int，默认 0）从未被业务写入：`RehabPlanDao.updatePlanProgress`(L100) 只被 `RehabPlanRepository.updateProgress`(L115，自身无调用方) 包装一次，业务层没有写入路径；`RehabPlanAdapter.kt:60` 只读该字段显示「进度 X%」。
- 现有 `completeInstance`(L287)/`skipInstance`(L294) 只做单条状态更新，无事务、无进度回写；`getTodayInstances`(L80) 只返回 `TaskInstance`，没有任务名称/时长/优先级。
- 任务状态由 `data/model/Status.kt` 的 `TaskStatus` 常量约束：`pending/completed/skipped/waiting_review/rejected`。
- 工程约束：`minSdk = 26`，`java.time` 可直接用（`utils/TaskSchedule.kt` 已在用 `LocalDate` 且已有 JVM 单测 `app/src/test/.../TaskScheduleTest.kt`）；Room 已依赖 `androidx.room:room-ktx`（`app/build.gradle.kts:100`），可用 `withTransaction`；数据库版本 8，`fallbackToDestructiveMigration()`。

## Goals / Non-Goals

**Goals:**

- 让康复页的今日列表、今日进度、本周柱状图、本周完成统计卡全部来自数据库当天/本周真实数据，且四者口径互相自洽（同一份数据算出来）。
- 计划进度形成闭环：任务完成/跳过 → 同一事务内重算并写回 `rehab_plan.progress` → 医生计划列表自动显示真实值。
- 把「应完成数」与「本周区间」抽成不碰数据库、可 JVM 单测的纯计算，避免规则散落在仓库层。
- 修正三处口径缺陷：生成缺计划结束日窗口、周区间周日偏移、计划结束日 off-by-one。

**Non-Goals:**

- 不做分段自绘环形进度控件（本轮占位），不做「累计训练」「连续打卡」的真实统计（streak 逻辑延后）。
- 不做视频录制/上传/审核，不改 `waiting_review`/`rejected` 的产生路径，也不在那条路径上挂进度重算。
- 不做后台 Worker 补生成历史任务；「漏了就是漏了」的现状不变。
- 不改数据库版本、不加表加列、不写迁移脚本；不做数据回填脚本（历史计划的 `progress` 在下一次完成/跳过时自然刷新）。
- 不做多患者/跨设备一致性与后端同步。
- 不更新 `Patient` 表的冗余统计字段（`rehabDays`/`overallProgress`/`weeklyCompletionRate`/`todayTasksCompleted`/`todayTasksTotal`）：当前全仓无读取方，写入没有收益；因此本轮不遵守 `AGENTS.md` 常见问题 4 的「相关操作后手动更新」约定，若 Day 4 患者进度页需要，由那个 change 决定读取现算还是回写。

## Decisions

### D1 新增独立的任务执行仓库，而不是继续堆在现有仓库上

选择：新建 `data/repository/TaskExecutionRepository`，构造签名为 `(db: AppDatabase, instanceDao: TaskInstanceDao = db.taskInstanceDao(), trainingTaskDao: TrainingTaskDao = db.trainingTaskDao(), planDao: RehabPlanDao = db.rehabPlanDao())`——`db` 提供 `withTransaction` 事务边界，三个 DAO 默认从 `db` 取、测试可注入装饰器。承载「完成/跳过 + 计划进度重算 + 康复页查询」。`TaskInstanceRepository` 保留生成职责，删除其中已无调用方的 `completeInstance`/`skipInstance` 及 10 个今日/本周查询方法（`getTodayInstances`、`getTodayInstancesOnce`、`getInstancesByDate`、`getWeekInstances`、`getWeekInstancesOnce`、`getTodayTaskCount`、`getTodayCompletedCount`、`getWeekTaskCount`、`getWeekCompletedCount`、`getWeekCompletionRate`），并删除随之失去唯一调用方的 8 条 `TaskInstanceDao` 查询（`getInstancesByDate`、`getInstancesByDateOnce`、`getInstancesByDateRange`、`getInstancesByDateRangeOnce`、`getTodayTaskCount`、`getTodayCompletedCount`、`getWeekTaskCount`、`getWeekCompletedCount`）与私有 `getWeekDateRange`/`getTodayDate`。计划进度在全项目只有 `TaskExecutionRepository` 一个写入口；`RehabPlanRepository.updateProgress` 这个无调用方包装随本轮删除。

理由：现有仓库已 342 行，混了生成、查询、统计、状态四类职责；执行动作需要跨三张表且在事务内，塞进去会让生成路径也被迫感知进度规则。`clarify.md` 确认这些方法在 `app/src/main` 内无外部调用方，替换不会产生回归。需要 `AppDatabase` 是因为 `androidx.room.withTransaction` 是 `RoomDatabase` 的扩展，单靠 DAO 开不了事务；现有仓库构造签名都是纯 DAO，本仓库是首个需要事务边界的。

备选：①在 `TaskInstanceRepository` 上加方法（弃：职责膨胀，且生成与执行共享一个实例会让事务边界含糊）；②新建 Repository 但保留旧方法做转发（弃：无调用方的死代码，AGENTS.md 明确拒绝，本轮净留死方法）。

### D2 「应完成数」做成纯函数，逐日遍历而非频率公式

选择：新增 `utils/PlanProgress`（object，与 `TaskSchedule` 同风格），暴露 `computeDueCount(tasks: List<TrainingTask>, planStartDate: LocalDate, planEndDate: LocalDate, today: LocalDate): Int`，内部对每个任务在 `[planStartDate, min(today, planEndDate)]` 上逐日调用 `TaskSchedule.shouldGenerate`，计数累加；窗口为空（today < startDate）返回 0。另提供 `computePercent(completed: Int, due: Int): Int`：`due <= 0 → 0`，否则 `(completed * 100 / due)` 向下取整并 `coerceIn(0, 100)`。

理由：逐日遍历复用了已被 10 个单测覆盖的频率规则，不会与生成口径产生第二套实现；weekly/interval 的解析、非法频率的兜底、interval 以计划开始日为锚等细节全部继承。演示级计划窗口（≤ 数周）下耗时可忽略。

备选：①按频率用数学公式直接算天数（弃：weekly 的星期集合与 interval 的锚点要重写一遍，是 off-by-one 的温床，且会和 `TaskSchedule` 漂移）；②在 SQL 里按实例条数统计（弃：正是 clarify 否掉的「已生成实例数」口径，漏登录会让分母缩水）。

守卫：窗口天数超过 3660 天（约 10 年，只可能来自历史脏数据）时按 3660 天截断计算，避免极端数据把逐日循环拖长。

### D3 完成/跳过走一个事务：改状态 → 找计划 → 重算 → 写回

选择：`TaskExecutionRepository.completeTask(instanceId, today: LocalDate = LocalDate.now())` / `skipTask(instanceId, today: LocalDate = LocalDate.now())` 在 `db.withTransaction { }` 内依次：读实例（`getInstanceByIdOnce`）→ **状态不是 `pending` 则直接返回**（`completed`/`skipped`/`waiting_review`/`rejected` 一律不改状态、不重算；`waiting_review`/`rejected` 由视频流程产生，不能被完成/跳过越权改写）→ 用 `TaskInstanceDao.updateInstanceStatus` 更新状态与 `completedAt`（跳过也写操作时间戳，与现状一致）→ 经 `taskId` 查 `TrainingTask` 得 `planId` → 读 `RehabPlan` 得窗口 → 用 `trainingTaskDao.getTasksByPlanOnce(planId)` 取同计划全部任务模板 + `TaskInstanceDao` 新增的「按计划窗口统计已完成数」查询得出 completed → `PlanProgress.computeDueCount` / `computePercent` → `RehabPlanDao.updatePlanProgress(planId, percent)`。`today` 通过参数注入（默认 `LocalDate.now()`），使进度计算可脱离设备时钟做确定性测试。

理由：spec 要求状态与进度同事务（不允许「任务完成了但进度没动」）；spec 也要求只有「待完成」任务可被执行，故守卫必须拦在业务动作层，而不是只靠界面隐藏「开始」按钮。`withTransaction` 是 room-ktx 提供的挂起事务，Room 挂起查询本身不在主线程执行，事务内多次查询安全。

新增的 DAO 只读查询（`TaskInstanceDao`）：

```sql
-- 某计划下某患者、在计划窗口内已完成的任务数（用于进度分子；与分母同窗口）
SELECT COUNT(*) FROM task_instance ti
INNER JOIN training_task tt ON tt.taskId = ti.taskId
WHERE tt.planId = :planId AND ti.patientId = :patientId AND ti.status = 'completed'
  AND ti.scheduledDate BETWEEN :startDate AND :endDate
```

备选：①先改状态再异步重算（弃：中间态可见，且异常时进度永久偏差）；②在 `updateInstanceStatus` 这个 DAO 方法上挂钩重算（弃：违反规则 D 挂载点，会波及视频提交等所有状态变更）。

可测性：构造允许注入 DAO（见 D1），测试侧传 `TaskExecutionRepository(db, decoratedInstanceDao)` 即可用只存在于 `androidTest` 的抛异常装饰器构造中途失败，验证状态与进度同回滚；另用「不存在的 `instanceId`」验证早退不落库。

脏数据分支：事务链路中若经 `taskId` 查不到 `TrainingTask`、或查不到 `RehabPlan`、或计划的 `startDate`/`endDate` 解析失败，一律记录 `Log.e` 并**跳过进度重算、保留已更新的任务状态**（与 D8 生成路径的保守跳过口径一致），不抛异常、不让页面崩溃；完成/跳过动作本身仍然成功。

### D4 今日列表与本周柱状图共用一条本周区间联表查询

选择：新增 `data/model/TaskInstanceWithTask.kt`：`@Embedded val instance: TaskInstance` + `taskName: String`、`suggestedDuration: Int`、`priority: String`（只投影 spec 今日列表真实消费的三个字段，不引入无读者的 `taskType`/`planName`/`planId`）。`TaskInstanceDao` 新增一条按**本周区间**取数并带展示字段的联表查询：

```sql
SELECT ti.*, tt.taskName AS taskName,
       tt.suggestedDuration AS suggestedDuration, tt.priority AS priority
FROM task_instance ti
INNER JOIN training_task tt ON tt.taskId = ti.taskId
WHERE ti.patientId = :patientId AND ti.scheduledDate BETWEEN :startDate AND :endDate
ORDER BY CASE tt.priority WHEN 'important' THEN 0 ELSE 1 END ASC, ti.createdAt ASC
```

返回 `Flow<List<TaskInstanceWithTask>>`。今日列表取其中 `scheduledDate = 今天` 的子集；今日进度、本周柱状图 7 桶、本周汇总全部由这同一份结果按 `scheduledDate` 分组计数得到，杜绝今日进度与柱状图各用一套数据源。排序用显式 `CASE` 让 `important` 在前（`priority` 存 `normal`/`important`，字典序 `important < normal`，若用 `DESC` 会与语义相反）。

理由：今日列表需要名称/时长/优先级，逐条再查是 N+1；`@Embedded` + 投影字段的 Room 支持成熟。把今天与本周合并成一条本周区间查询，可以让「今日进度 x/y」「柱状图七个桶」「本周完成卡」严格同源。

备选：①今日单独 JOIN 查询 + 本周无 JOIN 统计（弃：同页两套数据源，脏数据下今日进度与柱状图可能互相矛盾）；②`@Relation` 嵌套对象（弃：只支持一对一/一对多，两层嵌套要引入 `TaskWithPlan` 中间模型，且仍要再查一次）；③在 UI 层用映射表拼装（弃：N+1）。

### D5 周区间用 `java.time` 重算，收敛为唯一纯函数

选择：新增 `utils/DateRanges`（object）：`weekRange(today: LocalDate): Pair<LocalDate, LocalDate>`，起始日 = `today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))`，结束日 = 起始日 `plusDays(6)`；另提供字符串重载（内部用 `TaskSchedule.parseDate` 解析，失败时回退到今天）。`TaskInstanceRepository` 的 5 个无调用方周统计方法（`getWeekInstances`、`getWeekInstancesOnce`、`getWeekTaskCount`、`getWeekCompletedCount`、`getWeekCompletionRate`）与私有 `getWeekDateRange()`/`getTodayDate` 连同其引用的 DAO 查询一并删除；本 change 内唯一使用周区间的是新增的本周区间联表查询（D4）与康复页统计（D6）。柱状图所需的「每天完成/总数」由该查询结果在内存按 `scheduledDate` 分组得到（7 个桶），不新增聚合 SQL 与投影模型。

理由：`previousOrSame(MONDAY)` 的语义就是「本周周一」，不依赖区域设置（避开原 bug 的根因）；纯函数可直接构造 `LocalDate` 做边界单测（周一、周日、跨月、跨年）。旧 4 个消费方已确认无调用方，删除比「改写后继续留作死代码」更符合 D1 对死代码的处置；新的唯一消费方统一走 `DateRanges`，不存在两套周区间口径。

备选：①继续用 `Calendar` 只把 `set` 改成 `add`（弃：区域设置差异仍在，且难以脱离设备时区做单测）；②`WeekFields.of(Locale.getDefault())`（弃：项目语义已明确为「周一至周日」，不应随设备区域变化）；③保留 4 个旧方法并改为调用 `DateRanges`（弃：4 个方法无调用方，改完仍是死代码，且留下新旧两套入口）。

### D6 康复页引入 ViewModel + StateFlow，动作经 ViewModel，保留现有控件 id

选择：新增 `ui/main/patient/main/PtRehabViewModel : ViewModel()`——**无构造参数**，与项目现状一致（全仓无自定义 `ViewModelProvider.Factory`，所有 VM 都用 `by viewModels()` 无参创建）；内部用 `MyApp.instance.database` 组装 `TaskExecutionRepository`，暴露 `load(patientId: String)` 与 `StateFlow<RehabUiState>`，以及动作入口 `completeTask(instanceId)` / `skipTask(instanceId)` / `retry()`。**patientId 由 Fragment 解析后传入**：沿用 `pt_infoFragment.resolveAccount()` 的同一模式——`args.account` 非空优先，否则回退 `requireActivity().intent.getStringExtra("account")`（BottomNav 切页不带参数）；解析为空时不调 `load()`，直接进入 `Error` 态，避免用空 patientId 查出一个「合法空态」把取参失败掩盖掉。查询侧的 `today` 与 `weekRange` **在每次订阅时求值**（`flow { emitAll(repo.observeWeekTasks(patientId, DateRanges.weekRange(LocalDate.now()))) }` 或 `onStart`/`flatMapLatest` 包裹），因此 `repeatOnLifecycle` 重新收集与 `retry()` 都会按新的日期/周区间重算，跨零点或跨周恢复不会沿用旧区间。`retry()` 重新订阅查询 Flow；内部 `viewModelScope` 调执行仓库，完成后状态流自然重发。`RehabUiState` 为 sealed class：`Loading` / `Content(todayTasks, todayCompleted, todayTotal, weekBars: List<WeekBarData>, weekCompleted, weekTotal)` / `Error(message)`，`Error` 由查询 Flow 的 `catch` 产出。数据源只取 D4 那一条本周区间联表查询：今日列表取 `scheduledDate = 今天` 的子集，`todayCompleted/todayTotal` 由该子集计数，`weekBars` 由整份结果按日期分 7 桶，`weekCompleted/weekTotal` 为 7 桶之和——四处同源，杜绝同一页两个数值口径。Fragment 只弹框并把选择回调给 ViewModel 动作，不直接碰 Repository。`pt_rehabFragment` 改为 `viewLifecycleOwner.lifecycleScope` 收集状态 + `repeatOnLifecycle(STARTED)`。

布局改造：`fragment_pt_rehab.xml` 删除 4 个 `include item_exercise_task`（L280-283），换成 `RecyclerView`（id `rvTodayTasks`，`layout_height="wrap_content"` + `android:nestedScrollingEnabled="false"`，因为父容器是 `NestedScrollView`），新增 `item_task_instance.xml` 与 `ui/main/patient/adapter/TaskInstanceAdapter`；同时补空态/加载/错误三个视图（`tvEmpty`、`loadingView`、`errorView`、`btnRetry`）。状态 → 视觉类型与「是否可操作」抽成 `ui/main/patient/main/TaskStatusStyle` 纯函数（只有 `pending` 可操作），适配器只做赋值，因此该映射可 JVM 单测（`RehabFormatter` 不重复实现状态→视觉）。保留 `tvProgressCount`、`tvProgressPercent`、`progressBarToday`、`weekBar1..7`、`statCard1..3` 的现有 id 与结构，减少回归面。进度卡右上角图标区即为环形进度占位（不新增自绘控件），真实数值只由文字 + 横向进度条承载。

理由：现有 Fragment 直接 `findViewById` + 写死数据，没有 ViewModel；AGENTS.md 规定 ViewModel 不直接访问 DAO、必须经 Repository。需要说明的是，**本工程此前没有 StateFlow / repeatOnLifecycle**（既有是 `viewModelScope + collectLatest` / LiveData），这是首次引入，代价是新增一种范式；引入理由是康复页需要 Loading/Error 态且要合并多源。动作入口放 ViewModel 而非 Fragment，保证「UI→ViewModel→Repository」单向依赖，也让终态防连点集中在一处。

备选：①继续在 Fragment 里 `launch { repository... }`（弃：无 loading/error 态，状态散在视图里，无法按 spec 的刷新链路保证四处同步）；②用 `LiveData`（弃：项目新代码已在用 Flow，且 combine 多源更方便）；③Fragment 直连执行仓库触发完成/跳过（弃：绕过 ViewModel，破坏分层，且刷新链路要重做）。

### D7 计划起止日期：校验在保存动作，结束日 `+天数-1`

选择：把日期规则抽成纯函数 `utils/PlanDates`（object）：`isStartDateValid(startDate: String, today: LocalDate): Boolean`（解析失败视为非法）、`calculateEndDate(startDate: String, durationDays: Int): String?`（`LocalDate.plusDays(durationDays - 1L)`，解析失败返回 null）。校验加在 `CreateRehabPlanViewModel.savePlan` 的既有校验链（L91-L113 那段）中：开始日期非法时 `Result.failure(Exception("开始日期不能早于今天"))` 并 `return@launch`；由于草稿（`isDraft=true`）与激活（`isDraft=false`）共用 `savePlan`，两条路径自动同时生效。`calculateEndDate` 私有方法改为调用 `PlanDates.calculateEndDate`，返回 null 时 `Result.failure(Exception("开始日期格式非法"))`（不静默取今天）。同时在 `CreateRehabPlanActivity` 的日期选择器上把 `minDate` 设为今天，作为即时反馈的第二道，但权威判定在 ViewModel。

理由：规则 C 要求两条创建路径都受同一校验，挂在共用的保存入口一次覆盖；UI 层拦截只做体验优化，不作为唯一判定点。日期规则抽纯函数后可以脱离 Android 依赖做 JVM 单测（`AndroidViewModel` 本身跑不进 JVM 单测，现有工程也没有 Robolectric）——这是把「规则」而非「视图模型」作为测试对象的分层收益。

备选：①只在日期选择器上限制（弃：属于「只堵一条路径」，且选择器控件可被绕过/替换）；②在 Repository 里校验（弃：与现有校验集中在 VM 的惯例不一致，且保存失败要同时回显两条提示，分层反而更绕；「时钟依赖」不是理由——执行仓库本身就按设计注入了 `today`）；③日期规则直接写在 VM 里（弃：JVM 单测覆盖不到，只能手测）。

### D8 生成时间窗放在生成循环的计划级判断，先于频率规则

选择：`TaskInstanceRepository.generateDailyTasks` 在解析出 `planStartDate` 之后、取任务模板之前，增加结束日解析与窗口判断：`planEndDate == null || targetDate.isBefore(planStartDate) || targetDate.isAfter(planEndDate)` → `continue`（记 `Log.i` 跳过原因）。窗口判断抽为 `PlanDates.isWithinPlanWindow(startDate, endDate, date)` 纯函数，生成路径调用它，JVM 单测直接测该纯函数；「同日期重复生成不重复入库」依赖 Room 唯一索引，放到 Room instrumented 测试，不在 JVM 单测里假称覆盖。开始日的判断保留在 `TaskSchedule.shouldGenerate` 内（它本身已含 `date.isBefore(planStartDate)`），窗口判断只是把结束日补上并提前到计划级，减少无谓的任务模板查询。

理由：spec 要求窗口判断先于频率判断，且 `clarify.md` 说明两个生成入口共用同一函数——改这一处即同时覆盖登录与 `onResume`。结束日非法（脏数据）时保守跳过该计划，不生成。

备选：把结束日判断塞进 `TaskSchedule.shouldGenerate`（弃：`shouldGenerate` 是全项目频率规则的唯一权威，签名里加 endDate 会让 10 个既有单测全改；且结束日属于「计划窗口」而非「频率」语义）。

### D9 环形进度占位、今天标记与统计卡占位的呈现方式

选择：进度卡右上角区域保留现有图标占位（`ic_trending_up`），不新增自绘控件；文案区展示真实 `x/y 项已完成` 与 `完成率: p%`，横向 `ProgressBar` 的 `progress` 设为百分比。「累计训练」「连续打卡」两张卡显示占位符 `--`，并在卡片标签下保留原标签文字，不显示无来源数值。本周柱状图的「今天标记」：`WeekBarData` 增加 `isToday` 字段，`RehabFormatter` 对当天桶输出标签「今天」（其余为「周一」…「周日」），并返回是否用高亮色；Fragment 只把 `tvDay` 文案与颜色赋值，`item_week_bar.xml` 不新增控件。展示口径（进度文案、柱高比例且总数为 0 为 0、占位符、今天标记）抽成 `ui/main/patient/main/RehabFormatter`（纯函数，无 Android 依赖），便于 JVM 单测，Fragment 只做赋值；状态→视觉与可操作性唯一归 `TaskStatusStyle`（见 D6），`RehabFormatter` 不重复实现。所有百分比取整统一复用 `PlanProgress.computePercent`（向下取整 + clamp），不在 Formatter 里另写一套。

理由：spec 明确真实数值不得只由环形图承载、不得出现伪造值、柱状图要标记今天；不新增控件而用既有 `tvDay` 文案与颜色承载「今天」，改动面最小。把文案与比例计算抽纯函数，则「柱高 = 完成/总数、总数为 0 时 0」「今天标记唯一」这类易错点能被单测锁住，不必引入 Espresso；百分比复用同一函数避免一处 floor 一处 round 导致同页 66% vs 67%。

备选：删掉环形图区域（弃：布局留白、后续返工）；画一个静态示意环（弃：仍是无数据来源的伪值）；为今天标记新增高亮控件或自定义 View（弃：`item_week_bar.xml` 已有 `tvDay`，改文案/颜色足够）；把计算留在 Fragment 内（弃：不可单测）。

## Data Model

**本变更不涉及持久化结构变更：无 DDL、无迁移脚本、数据库版本保持 8。**

沿用现有 10 张表中的 3 张（均为既有表，本变更只读写既有列）：

| 表 | 本变更使用的列 | 读写方式 |
|---|---|---|
| `task_instance` | `instanceId`（Long，主键自增）、`taskId`（Long，非空）、`patientId`（String，非空）、`scheduledDate`（String `yyyy-MM-dd`，非空）、`status`（String，默认 `pending`）、`completedAt`（Long?，可空）、`createdAt`（Long，非空） | 读：今日联表查询、本周区间查询、按计划统计已完成数；写：`status` + `completedAt`（完成/跳过） |
| `training_task` | `taskId`（Long，主键自增）、`planId`（Long，非空）、`taskName`（String）、`taskType`（String）、`suggestedDuration`（Int，分钟）、`priority`（String，默认 `normal`）、`frequency`（String）、`frequencyDetail`（String） | 只读（今日列表投影字段、进度分母的频率依据） |
| `rehab_plan` | `planId`（Long，主键自增）、`patientId`（String）、`planName`（String）、`startDate`（String `yyyy-MM-dd`）、`endDate`（String `yyyy-MM-dd`）、`status`（String，默认 `draft`）、`progress`（Int，非空，默认 0，取值 0-100）、`updatedAt`（Long） | 读：窗口判断、进度重算；写：`progress` + `updatedAt`（经既有 `updatePlanProgress`） |

既有约束沿用（不新增、不修改）：

- `task_instance` 唯一索引 `(taskId, patientId, scheduledDate)` —— 重复生成由数据库兜底，本变更依赖它保证生成链路的幂等。
- `task_instance` 索引 `(patientId, scheduledDate)`、`(status)`；`training_task` 索引 `(planId)`；`rehab_plan` 索引 `(patientId)`、`(status)`。
- 审计字段现状：三张表只有 `createdAt`/`updatedAt` 时间戳，无操作人与逻辑删除列，本变更不引入。

新增的是**查询模型与查询语句**（非表结构）：

- `data/model/TaskInstanceWithTask`：`@Embedded` 的 `TaskInstance` + 投影字段 `taskName`/`suggestedDuration`/`priority`，不落库。
- `TaskInstanceDao` 两条只读查询：今日任务联表查询（见 D4）、按计划统计已完成数（见 D3）。
- `TaskInstanceDao.updateInstanceStatus` 由新的执行链路复用（`TaskExecutionRepository` 用它更新状态与 `completedAt`），不再是死方法。

## Rule Boundary Attachment

四条规则的判定挂载点与「不会被其它路径误触」的论证（对应 `proposal.md` 的规则边界表与两个 spec 的拦截/放行/等价路径场景）：

| 规则 | 挂载点（动作/层） | 为什么不会被同一业务意图的其它路径误触 |
|---|---|---|
| A 完成口径（只有 `completed` 计入分子） | 计划进度重算的分子统计——`TaskExecutionRepository` 计算进度时调用「按计划窗口统计 `status='completed'` 的任务数」这一条只读查询，SQL 里写死状态字面量与窗口条件 | 分子由单条 SQL 定义，任何调用重算的路径都得到同一分子；`skipped`/`waiting_review`/`rejected` 不在 SQL 条件内，因此跳过触发重算也不会改变分子（今天全跳过仍是 `0/N`）。分子与分母使用同一 `[startDate, min(today, endDate)]` 窗口，窗口外历史实例不会把进度虚高到 100%。计划列表只读 `progress` 字段，没有第二条计算进度的路径 |
| E 执行前置状态（只有 `pending` 可执行） | `TaskExecutionRepository` 的 `completeTask`/`skipTask` 业务动作内，读实例后先判 `status == pending`，否则早退 | 守卫挂在执行动作而非界面：即使界面被绕过（其它入口直接调仓库），`waiting_review`/`rejected`/终态任务也不会被改写状态或污染分子；视频提交走 `updateInstanceVideo`，不经过该动作，保持独立 |
| B 生成时间窗（`startDate ≤ date ≤ endDate`） | 每日任务生成函数的**计划级**循环内，在取任务模板与频率判断之前；两个触发入口（登录 `PtLogVM.kt:59`、`onResume` `PtMainActivity.kt:88`）都只调用这一个函数，不各自判断 | 生成只此一个函数出口（全仓 `generateDailyTasks` 仅定义 1 处、调用 2 处），窗口判断在函数内部，任何新触发点天然继承；窗口判断不改 `TaskSchedule`，既有 10 个频率单测不受影响。结束日非法的脏数据走保守跳过分支，不会被当成「窗口无限长」 |
| C 开始日期校验（不得早于今天） | 计划**保存动作**（`CreateRehabPlanViewModel.savePlan` 的校验链），草稿与激活共用同一分支；UI 日期选择器 `minDate` 只做即时反馈的第二道，不作为唯一判定 | 界面上不存在第二条写入计划的路径（`planRepository.createPlan` 全仓仅此 1 处调用；计划列表/详情无编辑入口），因此不会出现「从另一入口绕过校验写入早于今天的计划」。历史数据的读取、展示、生成判断都不经过该校验，所以旧计划不会报错 |
| D 进度重算挂载点 | 「任务完成/跳过」这一**业务动作**的事务内（`withTransaction` 包裹「改状态 + 重算 + 写回」），不挂在 `TaskInstanceDao.updateInstanceStatus` 这种底层单条状态更新上 | 底层状态更新被视频路径（`attachVideo` 走的是 `updateInstanceVideo`）与将来的审核路径复用；把重算挂在业务动作上，则这些路径不会触发重算。同时事务边界保证不会出现中间态，重算自带幂等短路（状态已是终态直接返回），重复点击不会叠加 |

同一业务意图的其它执行路径结论（已在两个 spec 中以「等价执行路径」场景落测）：

- 生成今天的任务：登录触发 / 回到主界面触发 → 同一函数、同一窗口判断，均正确拦截窗口外日期。
- 保存康复计划：存草稿 / 直接激活 → 同一 `savePlan` 校验链，均拦截早于今天的开始日期。
- 推进任务状态：完成 / 跳过 → 同一事务与同一重算，幂等；仅分子差异。
- 取本周区间：4 个旧消费方法无调用方，连同私有 `getWeekDateRange` 一并删除；新增的本周区间联表查询是唯一消费方，统一走 `DateRanges.weekRange`，无残留口径。
- 查看计划进度：医生计划列表 → 只读字段，无独立写入路径，自动受益。

## Risks / Trade-offs

- [进度百分比随计划定义变化而回溯变化：医生事后给计划加任务或改频率，同一批历史完成记录的百分比会随分母重算而下降] → 本轮接受（演示阶段无进度快照需求）；在 `Design Decisions` 与 spec 中明确口径为「按当前计划与任务定义实时推算」，不做快照；若后续需要审计口径，另提 change 引入进度快照表。
- [分母是纯推算而非实际生成量，患者漏登录时进度会显得偏低] → 刻意选择（clarify 已确认），因为它避免了「漏生成反而虚高」；风险留档为已知行为，不做补生成（后台 Worker 在 W3）。
- [逐日遍历在超长窗口/超多任务下变慢] → 窗口截断 3660 天；计划窗口通常 ≤ 90 天、单计划任务数 ≤ 数十，实测可忽略；该计算在挂起上下文执行，不阻塞主线程。
- [周区间口径从「设备区域设置相关」改为「固定周一到周日」] → 是有意修正；4 个旧消费方法已确认无调用方并随本轮删除，新增的本周区间联表查询是唯一消费方，不存在依赖旧口径的外部逻辑。
- [康复页布局从 4 个 include 换成 RecyclerView，可能影响同页的录制卡片区块] → 录制卡片当前是纯静态布局（`layoutStateIdle`/`layoutStateRecording`/`btnStartRecord`/`btnStopRecord` 只在 XML 中，全仓 Kotlin 无引用、无状态切换逻辑）；本轮只替换今日列表区域，不触碰该区块及其 id，后续接线录制时另提 change。
- [本周区间联表查询依赖 `training_task` 与 `rehab_plan` 都存在对应行；若任务模板被删而实例残留，INNER JOIN 会让这些实例从今日列表、今日进度、柱状图同时消失（口径一致但少统计）] → 已知取舍：项目内删除任务模板时会连带删除其实例（`deleteTaskInstances`），残留实例属于脏数据；因为四处同源，不会出现「列表少一条但进度不少」的同页矛盾；如需保留展示可改 LEFT JOIN，但会引入空名称条目，本轮不加。
- [本工程首次引入 `StateFlow` + `repeatOnLifecycle`（此前只有 `viewModelScope + collectLatest` / LiveData）] → 新增范式会带来一段风格统一成本；本轮只在康复页使用，后续若推广再统一收敛，并在代码注释里标出「首次引入」的上下文。
- [完成/跳过都写 `completedAt`（同一列承载两种语义）] → 与现状一致（`skipInstance` 原本就写），该列对跳过而言是「操作时间戳」，仅供审计、不计入任何完成统计；spec 已按此口径写明（完成时间戳、跳过操作时间戳），实现与测试按同一口径断言。
- [JVM 单测无法覆盖事务与 Room 查询] → 纯函数（`PlanDates`/`PlanProgress`/`DateRanges`/`RehabFormatter`）用 JUnit 覆盖边界；事务与写回、联表查询用 Room 内存库的 instrumented 测试覆盖（`app/src/androidTest`，已有 `androidx.test` 依赖），避免只靠手测。
- [instrumented 用例需要模拟器/真机，质量门在无设备时会卡住] → 质量门默认执行 `assembleDebug` + `assembleDebugAndroidTest`（保证 androidTest 源集编译通过）+ `testDebugUnitTest`；模拟器可用时追加 `connectedDebugAndroidTest`。设备相关的人工回归（录制卡片两态、空态视觉）留档于 Migration Plan，不进入 tasks.md。

## Migration Plan

- 部署：无 DDL、无数据脚本，直接覆盖安装（`versionCode`/`versionName` 按仓库现有约定递增）。数据库版本保持 8，`fallbackToDestructiveMigration()` 不变，因此不存在升级失败路径。
- 数据兼容：历史计划 `progress` 仍为 0，直到该计划下一次有任务被完成/跳过时自然刷新；不需要回填。历史 `startDate` 早于今天的计划不会被新校验拦下（校验只作用于保存动作）。
- 测试数据：`TestDataHelper.initTaskGenerationTestData()` 的种子计划写死 `startDate=2026-09-01`、`endDate=2026-09-30`，加时间窗后对今天整体出窗、生成 0 条任务；本 change 第 1 组把种子起止日改为相对今天（start=今天、end=今天+29），保证 `test001` 当天能生成任务。
- 回滚：代码回退即可，无 schema 变更需要回退；旧版本读取 `rehab_plan.progress` 会显示本 change 写入的真实值（字段本来就存在），不会崩。
- 人工回归留档（按 `openspec/config.yaml` 规则不进入 tasks.md）：①患者登录 → 康复页今日列表与数据库当天实例一致；②完成/跳过各一次 → 列表、进度、柱状图、统计卡四处同步；③医生计划列表显示真实进度百分比；④周日当天打开康复页，柱状图区间为本周一至当天（周日边界）；⑤康复页录制卡片区块结构未改动（仅替换今日列表区域），无需两态回归。

## Open Questions

- 「累计训练」与「连续打卡」的统计口径与实现周次延后确认（需 streak 定义与跨计划口径），不影响本 change 的 specs、方案与任务拆分。
