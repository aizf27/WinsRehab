# 架构审查报告 — ui-foundation-and-page-wiring

- 审查时点：提案阶段（Phase 3 架构审查），实施前
- 审查对象：`openspec/changes/ui-foundation-and-page-wiring/` 下 clarify.md / proposal.md / specs（ui-foundation、ui-page-wiring、patient-rehab delta）/ design.md / tasks.md + 项目 AGENTS.md + config.yaml + `openspec/specs/` 主规格 + 仓库真实代码
- 结论：**不通过（NEEDS-WORK）** — 14 条 issue，其中 Critical 1 条（按 tasks 实施会编译失败）、High 3 条、Medium 6 条、Low 4 条

## 结构校验（openspec validate）

命令：`openspec validate ui-foundation-and-page-wiring --strict`

实际输出：

```
Change 'ui-foundation-and-page-wiring' is valid
```

结构校验通过；下列问题均为架构与事实层面问题，validate 不覆盖。

## 总体判断

架构方向本身成立：取值表（语义色 + 4dp 间距 + 有限字号/圆角/阴影）作为唯一取值来源、双端主题共用基座只换主题色、8 类组件沉淀为 `widget_*.xml` + 样式 + `ui/common` 映射器、跨页参数收敛为单一解析链、按任务组分阶段——与仓库既有分层（UI → ViewModel → Repository → DAO）与既有实现脉络一致，未引入与既有能力重叠的新数据层，零 DDL、零写入路径改动、零新增查询（`getPendingTasksByDoctor` / `getPendingTaskCount` / `getPatientsByDoctorFlow` / `getWeekInstancesWithTask` / `getCompletedCountInWindow` / `getPlansByDoctor` 均已存在），与 `openspec/specs/` 既有主规格（patient-rehab 6 条、task-execution 7 条纯业务规则）无冲突、无重复定义。

问题集中在三处：**清理 Compose 的收口没有闭环到 MainActivity（Critical）**、**状态映射引入与既有 `TaskStatusStyle` 平行的第二套实现（High）**、**若干文档事实与仓库不符导致任务完成标准无法按文档核验（Medium/Low）**。

---

## issue 清单

### 1. Critical — MainActivity（Compose 模板页）未纳入任何任务，移除 Compose 构建开关后必然编译失败

- **severity**：Critical
- **file**：`design.md`（D7 死资源清理、Open Questions ①）、`tasks.md`（2.3、10.5）、`proposal.md`（What Changes「双端主题与依赖收敛」、契约变更点「依赖增删」）
- **problem**：`app/src/main/java/com/example/winsrehab/MainActivity.kt:16-31` 是 Compose 模板页（`ComponentActivity` + `setContent` + `androidx.compose.material3.Scaffold/Text` + `androidx.compose.ui.tooling.preview.Preview` + `ui.theme.WinsRehabTheme`），配套 `app/src/main/java/com/example/winsrehab/ui/theme/{Color,Type,Theme}.kt` 三个 Compose 主题文件（Type.kt 依赖 material3 Typography，Theme.kt 依赖 material3 MaterialTheme/lightColorScheme/dynamicColorScheme）。tasks.md 2.3 要求「移除 Compose 开关与 material3」并删 `libs.androidx.material3`，proposal 契约变更点也列了依赖增删，但**没有任何任务删除 MainActivity 与 ui/theme 三文件**：一旦 `compose = true` / `composeOptions` / compose 依赖被移除，MainActivity.kt 的 `setContent`/`@Composable` 便无法编译，工程直接红。
  同时这造成交付物自相矛盾：specs/ui-foundation/spec.md 需求⑧要求「界面资产不得保留零引用资源与未使用框架开关」，而 MainActivity 是除清单声明外零引用的 Compose 页面（启动入口是 SelectLogActivity）；specs/ui-page-wiring/spec.md 需求④要求「每个已实现页面必须至少有一个可达入口」，MainActivity 无任何入口。design.md 只把 `DtInfoActivity` 写进 D7 死资源清单，却把 MainActivity 以「归档时确认保留或删除」挂在 Open Questions ①（并注明「本轮死资源清理未列入（clarify 清点未覆盖）」），等于把必然的编译中断留到实施阶段才暴露。
- **suggested fix**：①design.md Context 记入「MainActivity + ui/theme 是 Compose 模板残留、除清单声明外零引用」；②D7 把它（类文件 1 个 + ui/theme 3 个 + AndroidManifest.xml:19 的声明）列为删除项，与 compose 构建项移除**同组闭环**；③tasks.md 2.3 增补「删除 MainActivity、ui/theme 三文件与清单声明」，完成标准加一条检索断言「`grep -r "androidx.compose|setContent|@Composable" app/src` 无匹配」；④proposal 契约变更点「依赖增删」与「BREAKING（内部）」处同步说明会删掉该页面；⑤Open Questions ① 删除（改为明确删除结论）。

### 2. High — 状态徽标新增 `StatusBadgeMapper` 与既有 `TaskStatusStyle` 平行并存，两套策略相反且无迁移安排

- **severity**：High
- **file**：`design.md`（D3 组件落点表「状态徽标」）、`tasks.md`（3.4、3.5）、`specs/ui-foundation/spec.md`（需求④卡片与状态徽标）
- **problem**：仓库已有状态映射实现 `app/src/main/java/com/example/winsrehab/ui/main/patient/main/TaskStatusStyle.kt`（30 行）：`TaskVisual` 6 值（含 `UNKNOWN`）、`visualOf(status)` 用 `else -> TaskVisual.UNKNOWN` 兜底、`labelOf` 给「待完成/已完成/已跳过/待审核/已驳回/未知」，且已被生产路径使用（`ui/main/patient/adapter/TaskInstanceAdapter.kt:54,61,65`），并有配套单测 `app/src/test/java/com/example/winsrehab/ui/main/patient/main/TaskStatusStyleTest.kt`（含 UNKNOWN 兜底断言）。design D3 / tasks 3.4 另建 `ui/common/StatusBadgeMapper.kt` + `widget_status_badge.xml`，并把策略反转为「缺分支时抛错而非兜底」，却未提复用、扩展、迁移或退役既有实现。
  后果有两层：①同一语义（任务状态 → 文案 + 语义色）出现两套权威实现，specs/ui-foundation 需求④「任一处不一致视为未完成」正是要求单一权威；②两套策略相反，若 TaskInstanceAdapter 继续用兜底、新徽标用抛错，则「未知状态」在两处行为不同，且既有单测断言（UNKNOWN 兜底）与新增策略直接冲突。
- **suggested fix**：在 design D3 与 tasks 3.4 中明确二选一，并写清迁移动作：①**扩展既有实现为唯一权威**——把 `TaskStatusStyle` 上收到 `ui/common`（或保留原位并让 `StatusBadgeMapper` 委托给它），计划状态并入同一映射表，`TaskInstanceAdapter` 改为调用同一出口，`TaskStatusStyleTest` 迁移到新位置并改写「未知状态不再静默兜底」的断言；②或明确删除 `TaskStatusStyle.kt` + `TaskStatusStyleTest.kt`，把 3 处调用点（TaskInstanceAdapter.kt:54,61,65）改到新映射器。无论哪种，tasks 3.5 的单测必须覆盖「任务 5 态 + 计划 4 态 + 未知/新增状态」三组，并说明旧测试文件的去向（迁移或删除），避免 test 目录留下断言相反的两份测试。

### 3. High — 主题基座由强制浅色改为 DayNight，深色行为语义变更未声明、无深色策略

- **severity**：High
- **file**：`design.md`（D2 双端主题、Risks、Trade-offs）、`tasks.md`（2.1、2.2）、`specs/ui-foundation/spec.md`（需求②双端主题）
- **problem**：现状 `app/src/main/AndroidManifest.xml` 有 12 处 `android:theme` 显式指定 `Theme.AppCompat.Light.*`（11 处 `Light.NoActionBar` + PatientApplicationActivity 处 `Theme.AppCompat.Light`），即**所有界面被强制为浅色**。本变更删除这些覆盖后统一继承 `Theme.WinsRehab`（`app/src/main/res/values/themes.xml:5` 的 parent 是 `Theme.MaterialComponents.DayNight.NoActionBar`），于是全部 18 个界面开始跟随系统深色模式——这是一次**既有的可见行为语义变更**，design D2 只写「基础主题 + 双端 overlay」，未声明该变化，Risks / Trade-offs 也未记录。
  同时仓库只有单套 `res/values/`（无 `values-night`），全项目检索 `setDefaultNightMode|forceDarkAllowed|uiMode|MODE_NIGHT` 无任何匹配，说明项目从未有过深色策略；取值表 D1 定义的是固定浅色值（如 surface 白、on_surface #1F2937、divider 浅灰），而 Material 组件在深色下会切换到深色主题属性，两者混用会出现「浅色卡片 + 深色文字/深色背景 + 浅色文字」的错配，specs/ui-foundation 需求②「界面不得自行覆盖主题」的场景也不覆盖这一冲突。
- **suggested fix**：design D2 明确择一并写入文档：①**锁浅色（最小变更，建议）**——基础主题改 `Theme.MaterialComponents.Light.NoActionBar`（或 `DayNight` + `AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_NO)` 在 Application 层），使深色行为与现状一致，D1 固定取值才有意义；②**支持深色**——补 `res/values-night/` 全量语义色（24 个色的深色值）并把双端 overlay 一并覆盖 night 变体，同时在 tasks 增补 night 取值任务与 Espresso/截图核验。无论哪种，都要在 Risks 记录「删除 12 处显式浅色主题后，深色行为由基座决定」这一语义变化，并在 spec 需求②的场景里补「深色模式下取值仍来自取值表，不得出现组件默认色」的断言。

### 4. High — 验收路径的前置数据无法由指定测试数据构造器产出，两条 Espresso 路径不可复现

- **severity**：High
- **file**：`design.md`（D8 测试与验收）、`specs/patient-rehab/spec.md`（delta 场景「完成 1 跳过 1 → 2/4」）、`tasks.md`（9.7 路径 4、7.3 路径 3）
- **problem**：design D8 指定测试数据来自 `TestDataHelper.initTaskGenerationTestData()`（`app/src/main/java/com/example/winsrehab/utils/TestDataHelper.kt:129`）。该构造器只建 1 个 active 计划 + **恰好 3 个任务模板**（daily / weekly `"1,3,5"` / interval `"2"`），当天生成的 TaskInstance 数在 1–3 之间且随星期与日期波动，**不可能是 4 条**；而 specs/patient-rehab delta 的场景与路径 4 的断言写死「今日 4 条待完成任务 → 完成 1、跳过 1 → 2/4」（spec 行 49-52）。同理，路径 3「确认患者申请后该条消失、计数减 1」需要一条 pending 状态的绑定申请，而该构造器建出的 test001 患者 `bindingStatus="active"`，**不产生任何待确认申请**；该构造器重复调用还会重复插入计划（无幂等检查），会影响「计数」类断言的稳定性。
  结果：tasks 9.7 / 7.3 的验收任务按文档无法通过，实施者只能临场自造数据，与「断言只看界面不读库」的原则形成缺口（数据准备无据可依）。
- **suggested fix**：任选其一并落到文档：①design D8 逐路径列出前置数据构造步骤（新增或扩展测试数据 helper：路径 4 需固定 N 条今日任务，可按「插入 N 个 daily 模板」或「连续 N 天 interval=1」构造；路径 3 需插入一条 `pending` 绑定申请 + 对应 DoctorTask），并在 tasks 增补「测试数据构造器扩展」子任务（含幂等清理）；②把 spec 场景与路径 4 的断言改为相对断言（「今日任务数为 N → 完成 1 跳过 1 → 首页显示 (1)/(N)」，N 由界面读取而非硬编码 4）；③两者结合：保留绝对断言的路径 4，但要求专用测试数据。同时明确构造器的幂等约定（重复调用先清后建），否则计数类断言不可复现。

### 5. Medium — AppCompat 主题覆盖计为 11 处，实际 12 处（漏计 SelectLogActivity），完成标准按错数验收

- **severity**：Medium
- **file**：`clarify.md`（Related-Surface Survey「主题挂载点」）、`proposal.md`（What Changes、契约变更点「仓内契约」）、`tasks.md`（2.2）
- **problem**：三处均表述为「11 个界面各自的 AppCompat 覆盖 / 11 处」。仓库事实是 `app/src/main/AndroidManifest.xml` 中 `android:theme="@style/Theme.AppCompat.*"` 共 **12 处**：DtLogActivity:25、PtLogActivity:30、DtHomeActivity:35、PtMainActivity:40、DtInfoActivity:45、PsychologyActivity:50、TrainingVideoActivity:55、VideoPlayerActivity:60、WaitingBindingActivity:65、PatientApplicationActivity:70（`Theme.AppCompat.Light`，唯一带 ActionBar）、CreateRehabPlanActivity:75、SelectLogActivity:82。tasks 2.2 的完成标准若照「11 个」核验，会漏掉 SelectLogActivity:82（虽然 tasks 5.1 另要求启动页挂基础主题，但 2.2 的核验口径本身是错的，后续若 5.1 被裁减或顺序调整即留下未覆盖项）。
- **suggested fix**：clarify / proposal / tasks 2.2 统一改为 **12 处**，并把 tasks 2.2 的完成标准写成可核验形式：「`AndroidManifest.xml` 中引用 `Theme.AppCompat.*` 的 activity 数为 0」＋列出 12 个 activity 名称清单；proposal 契约变更点同步改为 12 处。

### 6. Medium — 「计划三态」与仓库事实不符（PlanStatus 有 4 个合法值），按文档实施会在 draft/cancelled 抛错

- **severity**：Medium
- **file**：`proposal.md`（What Changes「8 类组件 / 状态徽标（任务五态＋计划三态）」、方案摘要）、`tasks.md`（3.4、3.5）
- **problem**：`app/src/main/java/com/example/winsrehab/data/model/Status.kt:20-31` 的 `PlanStatus` 有 **4 个**合法值：`draft` / `active` / `completed` / `cancelled`。proposal 两处写「计划三态」，而 tasks 3.4 要求「任务全状态 + 计划全状态 → 文案与语义色一一对应，缺分支时抛错而非兜底」——按「三态」实施必然漏掉两个合法值之一（通常是 draft 或 cancelled），计划列表页（4.4 / 8.4 要挂计划状态徽标）一旦出现草稿或已取消计划即抛错崩界面。design D3 与 tasks 3.4 用的是「计划全状态」口径（正确），说明是 proposal 侧的事实错误，但其重复出现会误导实施者按 3 个分支实现。
- **suggested fix**：proposal 两处改为「计划四态（草稿 / 进行中 / 已完成 / 已取消）」；tasks 3.5 的单测明确覆盖「任务 5 态 + 计划 4 态 + 未知状态反向用例」；specs/ui-foundation 需求④的场景补一条「草稿与已取消计划也要有徽标文案与语义色」。

### 7. Medium — 患者申请页失去唯一的标题/返回容器，方案未补顶部栏（该页将无标题、无返回）

- **severity**：Medium
- **file**：`design.md`（D4 18 页改造清单中 patient application 行）、`tasks.md`（5.4）
- **problem**：`app/src/main/res/layout/activity_patient_application.xml`（30 行）是 LinearLayout + RecyclerView + tvEmpty，**自身没有任何 Toolbar/顶部栏**，其标题与返回完全依赖 `AndroidManifest.xml:70` 的 `Theme.AppCompat.Light`（12 处覆盖中唯一带 ActionBar 的一处）。本次把它改为 NoActionBar 的双端主题后，该页将没有标题栏也没有返回按钮——违反 specs/ui-foundation 需求⑤（顶部栏与底部导航的统一形态）与 specs/ui-page-wiring 需求⑤（页面入口的去向完整性），用户进入患者申请列表后无法返回医生首页。design D4 该行与 tasks 5.4 只提到「卡片 + 徽标 + 空态 + 适配器改 ViewBinding」，未含顶部栏。
- **suggested fix**：design D4 该页行与 tasks 5.4 增补「加 `widget_top_bar`（标题『患者申请』+ 返回），返回行为与 ActionBar 现状一致（finish 回宿主）」；并在 spec 需求⑤的适用页清单中显式列出该页，避免后续遗漏；tasks 5.7 的 Espresso 路径 1 或路径 3 增加「进入患者申请页后可点返回回到医生首页」的断言。

### 8. Medium — 「同一数字同源」的共享机制与判定挂载点未定，按文档实施易落成「两处各自查询」

- **severity**：Medium
- **file**：`design.md`（D5 假数据落点表、D6 导航与参数）、`tasks.md`（9.1）、`specs/patient-rehab/spec.md`（delta 需求「患者首页与康复中心的数字同源同口径」）
- **problem**：现状康复页 `app/src/main/java/com/example/winsrehab/ui/main/patient/main/pt_rehabFragment.kt` 用 fragment 级 `PtRehabViewModel by viewModels()`（:18 附近），`repeatOnLifecycle(STARTED)` 收集 `uiState`；`TaskExecutionRepository.observeRehabWeek(patientId)`（`data/repository/TaskExecutionRepository.kt:47-54`）**每次订阅都会重新取 today 并重新查一次周数据**。design D5 只写患者首页「接康复页同一份周数据」、D6 只写导航与参数收敛，**没有指定共享机制**（activityViewModels 共享同一 VM 实例，还是 Repository 层单一缓存/单 Flow）。若实施者按最直觉方式在首页再建一个 fragment 级 VM 各自 collect，首页与康复中心就是两次独立查询，恰好落入 delta spec 明确要拦截的「两处各自计算」场景，也让「来自同一次周数据查询」的验收无法成立（跨周、跨天边界会更明显）。
  这是规则边界推演缺口：spec 侧给出了应当拦截的行为，design 侧未给出判定挂载点（挂哪一层、由谁保证单一订阅），tasks 9.1 也没有可核验的完成标准。
- **suggested fix**：design D6 明确机制并写明理由，例如「患者首页与康复中心共用宿主 Activity 作用域的同一 VM 实例（`by activityViewModels()`，与 `pt_homeFragment` 现有 `PtInfoVM` 用法一致），由首屏触发一次 `load()`，两页只 collect 同一 `uiState`；跨天/跨周刷新由宿主 `onResume` 统一触发一次」；tasks 9.1 完成标准写成可核验形式（「两页使用同一 VM 类型与 activityViewModels 作用域」「`observeRehabWeek` 在该路径上只订阅一次」，并说明如何验证）；spec 侧措辞与之一致（「同一 VM 实例 / 单一订阅」而非笼统的「同源」）。

### 9. Medium — 跨页参数解析链覆盖不全，遗漏 RehabPlanListFragment 等取参点，与「只保留一条解析链」冲突

- **severity**：Medium
- **file**：`design.md`（D6 账号与工号单一解析工具消费方）、`tasks.md`（5.6、8.4、9.6）、`specs/ui-page-wiring/spec.md`（需求⑥跨页面参数只保留一条解析链）
- **problem**：design D6 把 `AccountArgs` 的消费方列为 `pt_rehabFragment`、`pt_infoFragment`、`dt_pt_manageFragment` 三处，但仓库中 fragment 取参点至少还有：`ui/main/doctor/plan/RehabPlanListFragment.kt:27`（`doctorCode = arguments?.getString("doctorCode") ?: ""`，且本变更 6.1 才给它接第一个入口，属于本次变更范围内新增可达的页面）、`ui/main/doctor/home/dt_homeFragment.kt:22`、`ui/main/doctor/home/dt_info_Fragment.kt:35`、`ui/main/doctor/info/DtInfoEditFragment.kt:29`、`ui/main/patient/info/PtInfoEditFragment.kt:28`。spec 需求⑥要求「只保留一条解析链」，若只改三处，`RehabPlanListFragment` 会保留第二条独立链（且它正是本次新接入的页面），交付后仍存在「同一工号两种取法」。
  另外 tasks 9.6 只写「患者端两页账号解析改统一工具」，未覆盖医生端 fragment 的 navArgs 链，验收口径与 spec 不一致。
- **suggested fix**：design D6 把消费方清单补全（至少含 `RehabPlanListFragment`、`dt_homeFragment`、`dt_info_Fragment` 与两个编辑页），并说明导航目的地参数（`navArgs` 委托）与 `AccountArgs` 的分工：导航目的地由安全参数读取后交给 `AccountArgs` 统一裁决，或明确「`navArgs` 视为导航层豁免、但必须回落到同一出口」。tasks 8.4（计划列表页）增补「`doctorCode` 改用统一解析，参数缺失进明确空态（而非 `?: ""` 后发空查询）」，9.6 增补医生端 fragment；spec 需求⑥补一条「新增入口页面不得引入第二条解析链」的验收场景。

### 10. Medium — 全项目唯一占位口径落在患者端包内，医生端需横跨引用；与同批新增的 `ui/common` 收口方向不一致

- **severity**：Medium
- **file**：`design.md`（D1 取值/落点、D5 占位口径）、`tasks.md`（4.5、9.x）、`specs/ui-foundation/spec.md`（需求⑥统一占位口径）
- **problem**：spec 需求⑥要求「全项目 MUST 只有这一种占位写法」，design D5 指定唯一实现为 `RehabFormatter.placeholderText()`——但它位于 `app/src/main/java/com/example/winsrehab/ui/main/patient/main/RehabFormatter.kt:26`（患者端业务包），而本次要被统一到占位的使用方包括医生端：医生首页待审核数、医生我的「本月完成 / 满意度」（design D5）。于是医生端页面必须 import 患者端包里的 formatter，形成「医生 UI → 患者 UI 包」的横向耦合，与同一变更里新增的 `ui/common/StatusBadgeMapper.kt`、`ui/common/AccountArgs.kt` 的收口方向相反。这不是纯风格问题：占位口径是横向共性能力（跨端、跨页、新增页面必须复用），落在业务包内会让下一个新增页面「就近再造一个 `--`」，spec 的"唯一写法"约定不可长期维持。
- **suggested fix**：design D1/D5 把占位与三态文案（`--`、空态/错误态文案）上收到 `ui/common`（如 `ui/common/UiTexts.kt` 或 `Placeholder`），`RehabFormatter` 只保留康复页专用格式化（进度、柱高、星期标签），并明确「医生端不得引用患者端包」；tasks 4.5 的单测改为针对 `ui/common` 的文案映射，tasks 增补「把既有 `RehabFormatter.placeholderText()` 迁移到 `ui/common` 并删除旧入口（含 `RehabFormatterTest` 中相关断言迁移）」；同时在 spec 需求⑥的场景中补「医生端页面引用统一占位来源」的断言。

### 11. Low — strings.xml 清点数量与仓库事实不符（clarify 写 7 条，实际 5 条）

- **severity**：Low
- **file**：`clarify.md`（Related-Surface Survey 中 strings.xml 行）
- **problem**：clarify 记「7 条」，proposal 写「实际 5 条」，仓库 `app/src/main/res/values/strings.xml` 实为 **5 条**：`app_name`、`title_dotlog`、`dotlog_Inspirational_words`、`dotlog_theme`、`hello_blank_fragment`。clarify 与 proposal 同一事实两个数字，清点基线不可信会影响 tasks 5.5「抽中文文案删模板字符串与两条英文」的核验（容易被误当成还有 2 条英文待处理，实际英文只有 2 条——`dotlog_Inspirational_words`、`dotlog_theme`，与 5 条总数吻合）。
- **suggested fix**：clarify Related-Surface Survey 该行改为「5 条（`app_name`、`title_dotlog`、`dotlog_Inspirational_words`、`dotlog_theme`、`hello_blank_fragment`）」，并注明其中英文 2 条由 tasks 5.5 处理。

### 12. Low — 排除项计数前后不一致（排除表 9 行 vs User Answers「8 项」）

- **severity**：Low
- **file**：`clarify.md`（Related-Surface Survey 排除表、User Answers 第 11 条、Architecture Direction 方案摘要）
- **problem**：排除表逐行列出 9 项（摄像头录制、视频审核页、反馈页、放松中心、计划调整界面、环形进度自绘控件、患者表冗余字段回写、数据库表结构变更、业务规则与状态机），但 User Answers 第 11 条答「A–G 七组全部纳入，8 项排除项保持排除」，方案摘要又写「排除 9 项」。用户确认过的排除口径出现两个数字，后续 review 无法判断以哪个为准。
- **suggested fix**：统一为 9 项（以排除表为准），把 User Answers 第 11 条改为「9 项排除项保持排除」并在其后括注 9 项名称；或若其中某项实际并入范围，则删除该行并把三处数字统一为 8。

### 13. Low — design 声称存在 `DtInfoActivity` 及其专属布局，仓库里两者都不存在

- **severity**：Low
- **file**：`design.md`（D7 死资源清理）、`tasks.md`（10.5）
- **problem**：design D7 写「零引用独立活动页 `DtInfoActivity`（仅清单声明、全项目无跳转）及其专属布局资源」，tasks 10.5 写「删零引用独立活动页（含清单声明与专属资源）」。仓库事实：`app/src/main/java/com/example/winsrehab/ui/main/doctor/info/` 下只有 `DtInfoEditFragment.kt` 与 `DtInfoVM.kt`，**不存在 DtInfoActivity.kt**；`res/layout` 下也没有 `activity_dt_info*.xml`；全项目 `DtInfoActivity` 仅 1 处匹配 = `AndroidManifest.xml:42` 的清单声明（指向不存在的类）。因此任务 10.5 的「专属资源」无对象可删，实施者会先去找不存在的类与布局，或误删 `activity_dt_info_edit.xml` 之类仍在使用的布局。
- **suggested fix**：design D7 与 tasks 10.5 改述为「删除 `AndroidManifest.xml` 中 `DtInfoActivity` 的 activity 声明（该类与专属布局在仓库中均不存在，清单声明指向空类）」；把该现状写进 design Context（既有清单残留，属技术债）；tasks 10.5 的完成标准改为「清单中 activity 声明数 = 12 且无指向不存在类的条目」。

### 14. Low — 依赖收敛口径不完整且完成标准不可核验（Compose 目录条目、Material 版本）

- **severity**：Low
- **file**：`design.md`（D2「Material 依赖用 libs.material 版本沿用传递版本」）、`tasks.md`（2.3）
- **problem**：tasks 2.3 只写「移除 Compose 开关与 material3」，未覆盖 `gradle/libs.versions.toml` 中 9 条 Compose 相关条目（activityCompose 1.8.0、composeBom 2024.09.00、compose 1.7.0、material3 1.3.0、androidx-ui 系列）与 `app/build.gradle.kts` 的 `activity.compose`、`ui`、`ui.graphics`、`ui.tooling.preview`、`debugImplementation(ui.tooling)`、`debugImplementation(ui.test.manifest)`、`androidTestImplementation(ui.test.junit4)`——只删其中一部分会留死配置（与本次「不得保留未使用框架开关」的目标相悖，属新的技术债）。另一处：design D2 说 Material 用 `libs.material` 且「版本沿用传递版本」，但版本目录 `gradle/libs.versions.toml` 中**没有 material 条目**、`app/build.gradle.kts` 也未声明（当前靠传递依赖），本机 Gradle 缓存里 `com.google.android.material` 同时存在 1.10.0 与 1.12.0 两个版本——实施者无法得知该固定哪个版本，"沿用传递版本"也没有可核验对象。
- **suggested fix**：tasks 2.3 逐条列出待删清单（版本目录 9 条 + build.gradle.kts 7 项依赖 + `compose`/`composeOptions` 两个块），完成标准加检索断言「`grep -r "androidx.compose\|androidx-ui\|material3" gradle app/build.gradle.kts` 无匹配」「`./gradlew :app:assembleDebug` 通过」；design D2 与 proposal 契约变更点明确 material 的固定版本号（建议与项目 targetSdk/compileSdk 匹配的显式版本，如 `material = "1.12.0"`），并把「新增 `libs.material` 条目」写成具体动作。

---

## 已加载 skill 清单

按能力类别（架构 / 技术债 / 架构演进）匹配当前可用 skill 清单，命中即加载：

- `architecture-evolution` — 架构演进审查（架构维度）：架构脉络契合度、长期迭代与扩展性、规则/校验边界误伤、既有契约与行为语义兼容、重构贴合历史逻辑
- `technical-debt` — 技术债识别（架构维度）：过时技术选型、迁移债、平行实现与迁移缺失、临时方案固化、契约不一致与模型漂移

（`ddd-architecture` / `db-conventions` / `api-conventions` 等类别未命中：本变更零 DDL、零 API、无分层结构调整，仅界面层改造，domain/持久层契约不变，故未加载；非降级。）

## 主规格交叉核对结论

- `openspec/specs/patient-rehab/spec.md`（6 条既有需求：今日任务列表、完成与跳过三选对话框、今日进度、本周柱状图周区间、底部统计卡、刷新链路）：delta 仅 **ADDED** 一条「患者首页与康复中心的数字同源同口径」，不重定义既有语义，且引用的「跳过不计入完成」口径来自 task-execution 主规格，无冲突、无重复。
- `openspec/specs/task-execution/spec.md`（7 条需求全部为业务规则/状态流转/排期口径，无界面展示需求）：本变更不改写入路径、不改状态机，无冲突。
- 新增能力 `ui-foundation`、`ui-page-wiring` 在主规格中不存在，属纯新增，不与既有能力重叠。

## 执行边界

```yaml
task_groups:
  "1":
    allowed_directories:
      - "app/src/main/res/values"
      - "app/src/main/res/layout"
      - "app/src/main/res/color"
      - "app/src/main/res/drawable"
    allowed_packages:
      - "app/src/main/java/com/example/winsrehab/ui"
      - "app/src/test/java/com/example/winsrehab/ui"
    notes: "旧 40 个色名同时被 layout、drawable/shape、color/selector 引用，替换与删名必须同一次提交，否则 R.color 断链；values 目录当前仅有 strings/colors/themes 三个文件，文本外观新建 styles.xml；根包与 data/utils 层不应出现 R.color 改动，若出现说明越界。1.4 的检索断言需覆盖 app/src/main 全量（含 drawable 内的 #RRGGBB）。"
  "2":
    allowed_directories:
      - "app/src/main"
      - "app/src/main/res/values"
      - "app/src/main/res/color"
      - "gradle"
    allowed_packages:
      - "app/src/main/java/com/example/winsrehab"
      - "app/src/main/java/com/example/winsrehab/ui"
      - "app/src/test/java/com/example/winsrehab"
    notes: "AndroidManifest.xml 有 12 处 android:theme 显式覆盖（11 处 Light.NoActionBar + PatientApplicationActivity 处 Light），逐条改/删并以『引用 Theme.AppCompat.* 的 activity 数 = 0』核验；MainActivity.kt 与 ui/theme 三文件的删除必须与 compose 构建项移除同组完成，否则本组提交即编译失败；gradle/libs.versions.toml 需新增 material 固定版本条目并删 compose 相关条目，app/build.gradle.kts 同步删依赖。基础主题若锁浅色，改 parent 为 Theme.MaterialComponents.Light.NoActionBar。"
  "3":
    allowed_directories:
      - "app/src/main/res/layout"
      - "app/src/main/res/values"
      - "app/src/main/res/drawable"
    allowed_packages:
      - "app/src/main/java/com/example/winsrehab/ui/common"
      - "app/src/test/java/com/example/winsrehab/ui/common"
    notes: "状态徽标必须以既有 TaskStatusStyle 为基线扩展或整体迁移，禁止另起第二套映射（两套策略不能一兜底一抛错并存）；若迁移既有文件，需同步改 TaskInstanceAdapter 的 3 处调用点并处理 TaskStatusStyleTest；组件布局命名 widget_*.xml，样式落 values/styles.xml，语义色只引取值表。"
  "4":
    allowed_directories:
      - "app/src/main/res/layout"
      - "app/src/main/res/values"
      - "app/src/main/res/drawable"
    allowed_packages:
      - "app/src/main/java/com/example/winsrehab/ui/common"
      - "app/src/test/java/com/example/winsrehab/ui/common"
    notes: "三态与占位文案的单一实现应落 ui/common（勿放患者端包 ui/main/patient/main），医生端也要复用；对话框样式不改 Material 默认行为之外的内容；顶部栏组件需同时支持标题与返回，供患者申请页使用。"
  "5":
    allowed_directories:
      - "app/src/main/res/layout"
      - "app/src/main/res/values"
      - "app/src/main/res/navigation"
      - "app/src/androidTest"
    allowed_packages:
      - "app/src/main/java/com/example/winsrehab/ui/main/selectLog"
      - "app/src/main/java/com/example/winsrehab/ui/main/doctor/login"
      - "app/src/main/java/com/example/winsrehab/ui/main/patient/login"
      - "app/src/main/java/com/example/winsrehab/ui/main/patient/binding"
      - "app/src/main/java/com/example/winsrehab/ui/main/doctor/application"
      - "app/src/main/java/com/example/winsrehab/ui/common"
      - "app/src/test/java/com/example/winsrehab/ui/common"
    notes: "患者申请页改造必须补 widget_top_bar（标题+返回），该页原先只靠 Theme.AppCompat.Light 的 ActionBar 提供标题与返回；ApplicationAdapter 已是 ViewBinding，无需重写；strings.xml 只删两条英文与模板字符串，不要整包搬 303 处硬编码文案；账号解析工具放 ui/common，参数缺失返回 null 并由调用方进明确空态，禁止 ?: \"\" 后发空查询。"
  "6":
    allowed_directories:
      - "app/src/main/res/navigation"
      - "app/src/main/res/layout"
      - "app/src/main/res/values"
      - "app/src/androidTest"
    allowed_packages:
      - "app/src/main/java/com/example/winsrehab/ui/main/doctor/home"
      - "app/src/main/java/com/example/winsrehab/ui/main/doctor/plan"
      - "app/src/main/java/com/example/winsrehab/ui/main/doctor/adapter"
      - "app/src/main/java/com/example/winsrehab/ui/common"
    notes: "dt_info_Fragment 的 totalCount 导航参数退役后，DtHomeActivity 里『只为数条数』建的 PatientAdapter 及 submitList 一并删除，count 必须改由 doctor 我的页自行 observe（不得再靠 Intent/Bundle 传值）；DtHomeVM 与 DtHomeViewModel 合并时保留承载 patients 观察的那一个，导航 startArgs 与三处 navigate 的 Bundle 同步收敛；待办数与通知横幅必须同一数据来源（getPendingTaskCount(doctorCode, \"patient_binding\")），签名以现有 DAO 为准。"
  "7":
    allowed_directories:
      - "app/src/main/res/layout"
      - "app/src/main/res/values"
      - "app/src/androidTest"
    allowed_packages:
      - "app/src/main/java/com/example/winsrehab/ui/main/doctor/home"
      - "app/src/main/java/com/example/winsrehab/ui/main/doctor/adapter"
      - "app/src/main/java/com/example/winsrehab/ui/main/patient/info"
      - "app/src/main/java/com/example/winsrehab/ui/common"
    notes: "患者卡接 Patient.rehabStage / overallProgress，空值走统一占位；移除 AI 评估行前确认布局中没有其它引用；PatientAdapter 改造时顺带删除被注释且引用不存在字段的 4 行赋值（属死代码，一并清理）；患者管理页与医生首页的患者数断言需同一来源（getPatientsByDoctorFlow 条数）。"
  "8":
    allowed_directories:
      - "app/src/main/res/layout"
      - "app/src/main/res/navigation"
      - "app/src/main/res/values"
      - "app/src/androidTest"
    allowed_packages:
      - "app/src/main/java/com/example/winsrehab/ui/main/doctor/home"
      - "app/src/main/java/com/example/winsrehab/ui/main/doctor/info"
      - "app/src/main/java/com/example/winsrehab/ui/main/doctor/plan"
      - "app/src/main/java/com/example/winsrehab/ui/main/doctor/adapter"
      - "app/src/main/java/com/example/winsrehab/ui/common"
    notes: "计划列表页的 doctorCode 必须改走统一解析链并进明确空态（现为 arguments?.getString ?: \"\"），否则留下第二条取参链；CreateRehabPlanActivity 在 patientId 为空时直接结束的行为不改，列表页因此不放『新建计划』入口；计划状态徽标覆盖 draft/active/completed/cancelled 四态；RehabPlanListFragment 之前零入口，本组建 action 后要一并纳入可达性冒烟。"
  "9":
    allowed_directories:
      - "app/src/main/res/layout"
      - "app/src/main/res/navigation"
      - "app/src/main/res/values"
      - "app/src/androidTest"
    allowed_packages:
      - "app/src/main/java/com/example/winsrehab/ui/main/patient/main"
      - "app/src/main/java/com/example/winsrehab/ui/main/patient/adapter"
      - "app/src/main/java/com/example/winsrehab/ui/main/patient/info"
      - "app/src/main/java/com/example/winsrehab/ui/common"
    notes: "患者首页与康复中心必须共用宿主作用域的同一 VM（activityViewModels）且只订阅一次 observeRehabWeek，否则落入『两处各自计算』的应当拦截场景；pt_homeFragment 现有未使用的 navArgs 声明与 PtMainActivity 传参链要一并收敛到统一解析；康复页现有 Loading/Content/Error 三态与七根柱逻辑保留，只换组件与取值；置灰入口须移除点击监听（点击无副作用），不是仅改颜色。"
  "10":
    allowed_directories:
      - "app/src/main"
      - "app/src/main/res"
      - "app/src/main/res/layout"
      - "app/src/main/res/drawable"
      - "app/src/main/res/values"
    allowed_packages:
      - "app/src/main/java/com/example/winsrehab/ui/main/patient/psychology"
      - "app/src/main/java/com/example/winsrehab/ui/main/patient/Video"
      - "app/src/main/java/com/example/winsrehab"
    notes: "死资源删除须以 Lint UnusedResources 逐条确认为准，删除前检索布局/adapter 引用，避免误删仍在用的 drawable；删除 item_core_function.xml、item_today_task.xml、hello_blank_fragment 前确认无 inflate 引用；DtInfoActivity 只有 AndroidManifest.xml:42 的清单声明（类与专属布局都不存在），本组只删该声明；工作区必须保持 git status 干净，探针式改动一律还原。"
```
