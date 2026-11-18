# 澄清记录：界面基建与页面业务对接

- 变更：`ui-foundation-and-page-wiring`
- 日期：2026-11-10
- 模式：explore 模式发起，按 spec-driven-standard 的 clarify 决策树访谈方法论执行

## Open Questions

访谈共 5 轮、19 个问题，按前置依赖顺序推进。以下为浮出的疑点。

### 第 1 轮（范围与路线）

1. 本次「全面优化 UI」覆盖到哪一层：只做基建+现有页面、还是连第 2/3 周的新页面一起做、还是只修业务对接不动视觉？
2. 医生端蓝灰与患者端暖绿是两套视觉语言，要统一成一套品牌，还是保留双端差异化但共享取值与组件？
3. UI 技术路线：继续 XML + ViewBinding，还是局部/全面迁 Compose（Compose 与 Material3 依赖已引入，但只有模板生成的 `MainActivity` 一页在用）？

### 第 2 轮（假数据口径与组件边界）

4. 医生/患者首页、患者管理卡片上的假数字怎么处理：能真则真其余退占位、全部接真数据（含新增查询与聚合）、一律退占位、还是接患者表冗余字段并补回写？
5. 要不要显式引入 `com.google.android.material`（主题已是 MaterialComponents、已用 MaterialAlertDialogBuilder，但依赖未声明，靠传递依赖才能编译）？
6. 组件库做到哪些？（多选）
7. UI 改动怎么验收：编译+单测+Espresso+人工截图走查、只编译+单测、还是加截图基准测试？

### 第 3 轮（主题架构与断头入口）

8. 主题这一层怎么收：统一到 Material 主题+双端 overlay、放弃 Material 全走 AppCompat、还是只改用到 Material 组件的两个页面？
9. 康复页「开始录制/停止录制」两个无监听按钮与环形进度占位区怎么处理？
10. 导航断头（计划列表页不在任何导航图里）与三套并存的参数传递要不要本轮一并修？
11. 关联场景清点的划界是否确认（7 组纳入 + 9 项排除）？

### 第 4 轮（文案、死入口、令牌档位）

12. 启动页/登录页全英文而其他文案全中文，语言与 303 处硬编码文案怎么处理？
13. 医生首页四宫格/今日任务、患者首页两张无 id 死卡，具体怎么处置？
14. 设计令牌收敛到什么程度（现状：字号 11 档、圆角 7 档、阴影 5 档、布局内 389 处硬编码色值）？
15. Espresso 覆盖到什么程度（现有 `app/src/androidTest` 3 个文件：`ExampleInstrumentedTest` 是模板包名断言，`RehabQueryTest`、`TaskExecutionRepositoryTest` 是真实数据层用例，没有任何界面交互用例）？

### 第 5 轮（展示数字归属与拆分）

16. 患者首页三个数字（第 N 天 / 本周完成率 / 今日完成数）怎么定？
17. 医生首页「待审核视频数」「患者反馈数」能算但恒为 0（审核/反馈功能尚未实现），显示 0 还是退占位？
18. 这个工作拆成几个 OpenSpec change？

## User Answers

1. **范围**：基建 + 现有 18 个页面。第 2/3 周才出现的新页面（视频审核、反馈、放松中心）留给各自的功能 change。
2. **视觉**：双端差异化，共享同一套取值与组件，只换主题色。
3. **技术路线**：继续 XML + ViewBinding；清掉用不上的 Compose 依赖，避免双轨。
4. **假数据**：能真则真，其余退占位。明确不接患者表冗余统计字段（`rehabDays` / `todayTasksCompleted` / `weeklyCompletionRate` / `hasNewFeedback` / `lastActiveTime` 全项目无写入点，接上只会显示 0）。
5. **Material**：显式引入 `com.google.android.material`，卡片/输入/按钮/对话框/底部弹层统一走 Material；只保留 3D 渐变卡、欢迎卡这类特殊形状的自绘图形。
6. **组件**：全选 —— 按钮（四态+尺寸+禁用）、输入框（标签+错误+辅助文本）、卡片（标准/可点击/状态条）、状态徽标（任务五态+计划四态；映射以既有患者端状态样式文件为基座迁入共享包，不新建第二套）、加载/空态/错误态、分区标题+信息行、对话框（确认/三选一/底部弹层）、顶部栏+底部导航、死资源清理。
7. **验收**：编译 + JVM 单测 + Espresso 5 条关键路径 + 人工截图走查（走查清单留档设计文档，不进任务列表）。
8. **主题**：统一到 Material 主题，派生医生端/患者端两个双端 overlay，处置 Manifest 里 12 处 AppCompat 声明（删 1 处指向不存在类的幽灵声明、11 个在用界面改挂）。
9. **康复页空壳**：「开始录制/停止录制」退成「即将开放」占位；环形进度区保持占位，本轮不做自绘环。
10. **导航与参数**：一并修 —— 计划列表挂进医生端导航并加入口；账号/工号解析收敛成一套；合并医生首页两个同义数据控制器，去掉宿主页里只为数患者人数而创建的适配器。
11. **清点划界**：A–G 七组全部纳入，9 项排除保持排除。
12. **文案**：语言统一中文；303 处硬编码文案只抽「跨页面复用 + 需要状态映射」的部分（状态徽标、空/错/加载文案、通用按钮词、底部导航），页面独有静态文案本轮保留字面量。
13. **死入口**：能接就接、接不上就标「即将开放」并置灰。医生首页保留 2 个可用入口（管理患者、制定计划），其余 2 个置灰；医生首页「今日任务」改成「最新患者申请」（走已有待办数据）；患者首页「病友社区」「医生反馈」置灰禁用。
14. **令牌**：字号 6 档（11/12/14/16/20/24）；间距 4dp 基准，主用 4/8/12/16/24/32；圆角 3 档（8/12/16）；阴影 2 档（0/1）；颜色收敛到约 24 个语义色。
15. **Espresso**：5 条关键路径 —— 启动→医生登录→首页；医生患者管理→创建计划；医生确认患者申请；患者登录→康复页完成/跳过；底部导航三 tab 切换 + 计划列表可达。
16. **患者首页数字**：「第 N 天」退占位（无数据来源）；「本周完成率」与「今日完成数」接真实聚合，与康复页共用同一周区间与完成口径。
17. **恒为 0 的数字**：「待审核视频数」「患者反馈数」退占位，与「累计训练/连续打卡」同一处理。
18. **拆分**：一个 change，内部按任务组分阶段推进。

## Related-Surface Survey

### 纳入本 change

| 关联面 | 代码证据 | 数量 | 划界结论 |
|---|---|---|---|
| 共享颜色取值 | `app/src/main/res/values/colors.xml`（40 个颜色名）；布局内硬编码 `#RRGGBB` 共 389 处 | 40 + 389 | 纳入：全部页面的取值来源，改这里才谈得上对齐 |
| 共享主题与样式 | `app/src/main/res/values/themes.xml`（`Theme.WinsRehab`、`MyRoundButton`、`SectionTitle`、`InfoCardStyle`、`Divider`） | 5 个 style | 纳入：新增双端 overlay 与组件 style 的落点 |
| 共享文案 | `app/src/main/res/values/strings.xml`（5 条：`app_name`、`title_dotlog`、`dotlog_Inspirational_words`、`dotlog_theme`、`hello_blank_fragment`，后三条是模板与英文垃圾） | 5 | 纳入：语言统一与状态类文案抽离的落点 |
| 主题挂载点 | `app/src/main/AndroidManifest.xml`：`<application android:theme="@style/Theme.WinsRehab">`，但 12 条 Activity 声明覆盖为 `@style/Theme.AppCompat.Light.NoActionBar`（11 条）/`@style/Theme.AppCompat.Light`（1 条）；其中 `DtInfoActivity` 指向不存在的类；`MainActivity` 挂 Material 主题但非启动入口 | 12 处 | 纳入：「Material 组件用不上」的根因，与新增双端主题强绑 |
| 依赖声明 | `app/build.gradle.kts`：`buildFeatures { compose = true }`、`implementation(libs.androidx.material3)`，但 Compose 代码只有模板生成的 `MainActivity` 一页；`com.google.android.material` 未声明，经 `navigation-ui-ktx` 传递进入（Gradle 缓存内有 1.10.0 / 1.12.0） | 1 个构建文件 | 纳入：显式引入 material、清理 Compose 依赖 |
| 底部导航选中态 | `app/src/main/res/color/{dt,pt}_bottom_nav_{icon,text}_selector.xml`：医生端选中色 `#38A9F0`、未选中 `#666666`；患者端选中 `#00BC8C` | 4 个 selector | 纳入：双端主题色的实际消费方 |
| 页面：入口与身份 | `activity_select_log.xml` + `SelectLogActivity`（英文文案）、`activity_dt_log.xml` + `DtLogActivity`、`activity_pt_log.xml` + `PtLogActivity`、`activity_waiting_binding.xml` + `WaitingBindingActivity`、`activity_patient_application.xml` + `PatientApplicationActivity` | 5 | 纳入 |
| 页面：医生端 | `fragment_dt_home.xml` + `dt_homeFragment`、`fragment_dt_pt_manage.xml` + `dt_pt_manageFragment`、`fragment_dt_info_.xml` + `dt_info_Fragment`、`fragment_dt_info_edit.xml` + `DtInfoEditFragment`、`activity_create_rehab_plan.xml` + `CreateRehabPlanActivity`、`fragment_rehab_plan_list.xml` + `RehabPlanListFragment` | 6 | 纳入 |
| 页面：患者端 | `fragment_pt_home.xml` + `pt_homeFragment`、`fragment_pt_rehab.xml` + `pt_rehabFragment`、`fragment_pt_info.xml` + `pt_infoFragment`、`fragment_pt_info_edit.xml` + `PtInfoEditFragment` | 4 | 纳入 |
| 页面：独立功能页 | `activity_psychology.xml` + `PsychologyActivity`、`activity_training_video.xml` + `TrainingVideoActivity`、`activity_video_player.xml` + `VideoPlayerActivity` | 3 | 纳入 |
| 列表适配器 | `PatientAdapter`（`findViewById` 风格）、`PatientForPlanAdapter`（`findViewById`）、`PatientPlanGroupAdapter`（`findViewById`）、`RehabPlanAdapter`（`findViewById`）、`TrainingTaskAdapter`（`findViewById`）、`TaskInstanceAdapter`（`findViewById`）、`ChatAdapter`（`findViewById`）、`ApplicationAdapter`（ViewBinding）、`DemoVideoAdapter`（ViewBinding） | 9（6+3 两套混用） | 纳入：卡片与徽标组件的落地位置 |
| 导航图与跳转 | `res/navigation/dt_bot_nav.xml`（5 个 fragment）、`res/navigation/pt_bot_nav.xml`（4 个 fragment）；14 处有效 `startActivity` 跳转（另有 1 处已注释）；`RehabPlanListFragment` 未出现在任何导航图 | 2 + 14 | 纳入：计划列表是孤儿页、参数三套并存，属「与业务对接」的直接断点 |
| 只读数据来源 | `data/dao/PatientDao.kt`、`data/dao/TaskInstanceDao.kt`、`data/dao/TrainingTaskDao.kt`、`data/dao/RehabPlanDao.kt`、`data/dao/DoctorTaskDao.kt`、`data/repository/TaskExecutionRepository.observeRehabWeek`、`data/repository/PatientRepository.getPatientsByDoctor` | 5 + 2 | 纳入：仅复用现有只读查询，预期零新增；不改写路径 |
| 死资源 | 约 30 个未被任何布局或代码引用的 drawable（如 `img_px_*` 类图片与若干 `shape_*` 占位图；具体清单以 Lint UnusedResources 结果为准——举例不等于白名单，`ic_check_circle.xml` 就被 `TaskInstanceAdapter.kt:56` 引用着，不能删）、`item_core_function.xml`、`item_today_task.xml`（无任何引用）、`strings.xml` 的 `hello_blank_fragment` 与两条英文（删字符串须同时删掉 `activity_dt_log.xml:57`、`:84` 两处引用它们的 TextView，否则引用悬空）、`DtInfoActivity` 的幽灵清单声明（类与布局都不存在）、模板 Compose 页 `MainActivity` 与 `ui/theme` 三文件、Compose 依赖 | — | 纳入：属「修复」的清理项，留着会持续误导后续开发（幽灵声明被跳转即崩溃） |

### 排除（各附理由）

| 排除项 | 代码证据 | 理由 |
|---|---|---|
| 摄像头录制 | 无 CameraX 依赖、无录制实现；`fragment_pt_rehab.xml` 的 `btnStartRecord`/`btnStopRecord` 无任何监听 | 属开发计划第 2 周的功能 change |
| 视频审核页 | 不存在对应 layout/Activity/Fragment；`PtRehabVideoDao` 无按审核状态查询 | 属第 2 周功能 change |
| 反馈页 | `AppDatabase` 注册了 `Feedback::class` 实体但**无 `feedbackDao()` 访问器** | 属第 3 周功能 change |
| 放松中心（五子棋/后台音乐） | 无对应代码 | 属第 4 周 |
| 计划调整界面 | `PlanAdjustment` 仅有实体与 DAO/Repository，无界面 | 开发计划明确「第二个月做」 |
| 环形进度自绘控件 | 无 `ui/widget` 包；`fragment_pt_rehab.xml` 现用 `ProgressBar` | 属第 1 周 P1 可选功能，本轮用横向进度条代替 |
| 患者表冗余统计字段回写 | `Patient.kt` 中 `rehabDays`/`todayTasksCompleted`/`todayTasksTotal`/`weeklyCompletionRate`/`hasNewFeedback`/`lastActiveTime`/`rehabStartDate` 全项目无写入点 | 用户已定不接冗余字段 |
| 数据库表结构变更 | `data/database/AppDatabase.kt` 与 `DatabaseMigrations.kt` | 本轮零 DDL |
| 业务规则与状态机 | `openspec/specs/task-execution/spec.md`、`openspec/specs/patient-rehab/spec.md` | 已由两份主 spec 锁定，本轮不改 |

### 一致性分支（同一数据的多条路径）

- **账号/工号传递存在三条路径**：导航参数（`dt_homeFragment`、`dt_info_Fragment` 用 `navArgs`）、页面参数（`dt_pt_manageFragment`、`RehabPlanListFragment` 用 `arguments?.getString(...) ?: ""`）、宿主页面 Intent 兜底（`pt_rehabFragment.resolveAccount`、`pt_infoFragment.resolveAccount`），以及两个信息编辑页（`DtInfoEditFragment`、`PtInfoEditFragment`）同样用 `navArgs`——合计 9 处声明、8 处真正读取（`pt_homeFragment` 的声明从未被读）。结论：本轮收敛为「导航参数优先、宿主页面兜底」单一入口（`AccountArgs`），全部消费方在同一 change 内适配，含本轮新建入口的计划列表页。
- **同一个数字的多处展示**：患者首页本周完成率与康复页本周完成、医生首页管理患者数与患者管理页患者总数、医生我的患者数（`dt_info_Fragment` 走导航参数 `totalCount`，由 `DtHomeActivity` 观测填充）。结论：同一份查询、同一区间、同一算法；`dt_info_Fragment` 不再依赖导航传入的计数，改为自行观察。

## Architecture Direction

### 选定方向

1. 引入 `com.google.android.material` 并把 12 处主题声明统一处置（11 个在用界面改挂到 Material 基础主题 + 医生端/患者端两个 overlay，1 处幽灵声明删除）。
2. 建立一套界面取值表（颜色/字号/间距/圆角/阴影），页面不再写死色值与尺寸。
3. 建立 8 类可复用组件，全部页面改造到这套组件上。
4. 假数据按「能算就算、算不出就占位」逐处替换；占位文案与样式与康复页现有「累计训练/连续打卡」完全一致。
5. 死入口按「能接就接、接不上标即将开放并置灰」处理，且置灰为可逆的临时状态。
6. 继续 XML + ViewBinding，清理未使用的 Compose 依赖。
7. 一个 change，内部按任务组分阶段推进。

### 备选方案与取舍

| 备选 | 未选原因 |
|---|---|
| 放弃 Material，全走 AppCompat | 不用处置 12 处主题声明，但项目已在用 Material 对话框与底部弹层、Material 依赖本来就在，回头改会变成「依赖在但不敢用」的长期别扭 |
| 全面迁 Compose | 18 页重写超出本轮；XML 主题与 Compose 主题互通还要额外搭一层 |
| 统一成一套品牌色 | 改动面更大，患者端「温暖康复」定位会被削掉 |
| 全部接真数据 | 要新增一批统计查询与聚合，而「累计训练/连续打卡」本身无数据来源，会把本轮拖成数据工程 |
| 接患者表冗余字段并补回写 | 回写点分散、要养一套新的一致性难题，项目记忆已把该隐患列出 |
| 拆成三个 change | 取值表一改就波到全部 18 个界面，拆开会让共享样式两边割裂维护 |

### 方案摘要（已由用户文字确认）

用户于 2026-11-10 以「确认」二字直接文字回复确认。以下为摘要全文。

#### 1. 这次变更做什么

**一句话**：给项目立一套双端共用的界面取值表与可复用组件，把现有 18 个界面全部改造到这套标准上，并把这些界面上「写着假数字」和「点不动」的地方换成真数据或明确的占位。

变更内容逐条：

- 新增一套界面取值表，把**颜色、字号、间距、圆角、阴影**五类统一归档，页面里不再直接写色值与尺寸（现在布局里有 389 处硬编码色值、305 处字号、229 处写死高度）。
- 取值收敛目标：字号从 11 档收到 **6 档**（11/12/14/16/20/24）；间距以 **4dp 为基准**，主用 4/8/12/16/24/32；圆角收到 **3 档**（8/12/16）；阴影收到 **2 档**（0/1）；颜色收到约 **24 个语义色**。
- **双端各一套主题**：医生端以蓝为主（主色 #2563EB），患者端以绿为主（主色 #16A34A，配 #22C55E），共用同一套取值与组件，只换主题色。患者端现在散着 5 个绿，本轮收成一个主绿 + 一个辅助绿。
- 把 11 个在用界面统一挂到**真正启用 Material 组件**的主题上，去掉现在每个界面各自覆盖成 AppCompat 的做法（另删掉一处指向不存在类的幽灵声明）。
- 新增 **8 类可复用组件**：按钮（主/次/危险/文字四态，含尺寸与禁用）、输入框（标签＋必填标记＋错误文案＋辅助文本）、卡片（标准/可点击/带状态条）、状态徽标（任务五态＋计划四态）、加载/空态/错误态、分区标题＋信息行、对话框（确认/三选一/底部弹层）、顶部栏＋底部导航。
- **18 个界面逐个改造**：换组件、换取值、统一顶部栏、补齐加载/空/错三态（现在只有康复中心有完整一套）。
- **医生首页**从整页假数据改成：真实管理患者数 ＋ 真实待办（最新患者申请，来自已有数据）＋ 四宫格里 2 个可用入口（管理患者、制定计划），其余 2 个（审核视频、患者反馈）标「即将开放」并置灰。三条假任务卡和「今天有 3 个待审核视频，2 条患者反馈」全部撤掉。
- **患者首页**：「今日完成数」和「本周完成率」接真实聚合，与康复页用同一个「本周一至本周日」区间和同一套完成口径；「第 N 天」退占位；「病友社区」「医生反馈」两张卡标「即将开放」并禁用点击。
- **患者管理列表**：每张患者卡接真实康复阶段与进度（字段本来就有，现在赋值被注释掉，布局默认值让每位患者都显示「瘀滞期 / 60% / 握力↑」）。
- **医生我的页面**：本月完成、患者满意度退占位。
- **康复页**：「开始录制 / 停止录制」退成「即将开放」占位；环形进度区保持占位。
- **计划列表页接回导航**：挂进医生端导航并加入口。
- **账号/工号传递收敛成一套**：导航参数优先、宿主页面兜底。
- **合并医生首页两个同义的数据控制器**，去掉宿主页里只为数患者人数而创建的适配器。
- **登录三页英文改中文**。
- **死资源清理**：约 30 张未引用图片、2 个死布局、3 条模板与英文占位字符串、一处幽灵 Activity 清单声明、模板 Compose 页与 `ui/theme` 三文件、用不上的 Compose 依赖与构建开关。
- **补齐验证**：5 条关键路径的界面自动化测试，现有单元测试保持全绿。

#### 2. 是否涉及契约

- **数据库**：不改表结构、不加字段、不加索引、不改迁移。预期**不新增任何查询方法**（本周完成率复用康复页已有的周区间查询，医生待办复用已有待办查询）。若实现中确需新增，仅限只读查询，不触碰任何写入路径。
- **对外接口**：本项目无后端、无端点，不涉及。
- **明确不做**：不接患者表冗余统计字段；不补反馈表的数据访问入口。

#### 3. 选定方案与取舍

选定 Material 主题 + 双端 overlay、继续 XML + ViewBinding、8 类组件统一收敛；取舍见上表。

#### 4. 实施逻辑（人话）

1. 先立取值表与主题：五类取值归档，派生医生端/患者端两套主题；统一处置 12 处主题声明。此步完成后项目会短暂处于「取值表已就位、页面仍用旧写法」的中间态，属预期。
2. 再做组件：按「按钮 → 输入框 → 卡片 → 徽标 → 三态 → 标题与信息行 → 对话框 → 顶部栏与底部导航」顺序建，每建一类立刻拿一个现成页面当第一个使用方验证。
3. 逐页改造分四组：①入口与身份页（启动、医生登录、患者登录、等待绑定、患者申请）；②医生端（医生首页、患者管理、医生我的、信息编辑、创建计划、计划列表）；③患者端（患者首页、康复中心、患者我的、信息编辑）；④独立功能页（心理助手、示范视频库、视频播放）。每组含「换组件 + 换取值 + 补三态」。
4. 假数据按「能算就算、算不出就占位」逐处落地：能现算的（患者总数、最新患者申请、今日完成数、本周完成率、患者的康复阶段与进度）接真实值；算不出或功能未到的（累计训练、连续打卡、第 N 天、待审核视频数、患者反馈数）统一换成与康复页现有占位完全一致的形态，不出现第二种占位。
5. 死入口按「能接就接、接不上标即将开放」处理；置灰只是临时状态，功能上线后同一入口必须能不重构就恢复响应。
6. 同源数字必须同口径：同一份数据、同一区间、同一算法。
7. 导航与参数：计划列表挂回医生端导航并加入口；账号与工号解析收敛成一个统一入口；合并医生首页两个同义数据控制器。
8. 清理与验证：删未引用图片、死布局、模板与英文占位字符串、幽灵 Activity 声明、模板 Compose 页与 Compose 依赖；补 5 条关键路径界面自动化测试；跑通编译与现有 8 个单测文件、既有 Instrumented 用例；人工截图走查清单留档设计文档。

#### 5. 关联场景划界

纳入 7 组（共享样式资源、12 处主题声明、18 个界面、9 个适配器、两个导航图与 14 处跳转、现有只读数据查询、死资源）；排除 9 项（摄像头录制、视频审核页、反馈页、放松中心、计划调整界面、环形进度自绘控件、患者表冗余字段回写、数据库表结构变更、业务规则与状态机）。逐条理由见本文件 Related-Surface Survey 小节。

#### 6. 受影响的能力

- `patient-rehab`：页面展示字段与口径不变，仅改视觉与组件实现；另需补一条「康复页与患者首页同源同口径」的展示约束。
- `task-execution`：完全不动。
- 新增 `ui-foundation`：界面取值规范、双端主题、可复用组件形态与使用约束、三态与占位口径。
- 新增 `ui-page-wiring`：各页面展示字段来源、占位口径、导航入口与账号传递约束。

#### 7. 规则边界

本轮**不新增、不修改任何业务规则、校验或拦截逻辑 → 规则边界推演「不适用」**。理由：本轮只动展示层与导航接线，任务状态流转、计划进度口径、任务生成时间窗、计划起止日期校验全部沿用现有两份主 spec 的结论，不做任何改动。

但有两类**展示侧约束**需写成可测条款：

| 约束 | 应当成立（拦截） | 应当放行（例外与合法反例） | 判定挂载点 |
|---|---|---|---|
| 「即将开放」入口 | 点击无跳转、无数据变更、视觉置灰；不得表现为「可点但无响应」 | 对应功能上线后必须能不重构就恢复响应；入口本身不因置灰而从布局中消失 | 挂在视图层（入口控件），不挂在数据层或状态机 |
| 同一数字多处同源 | 患者首页与康复页的本周完成率、医生首页与患者管理页的患者总数，任一处与另一处不一致即视为未完成 | 两处刷新时机不同导致的短暂不一致允许存在；同一时刻渲染出的值必须相同 | 挂在数据来源层（同一份查询、同一区间、同一算法），不各自算 |

## Authoring Tier

- Tier: COMPLEX
- Rationale: 触及 2+ 能力（新增 `ui-foundation`、`ui-page-wiring`，并补 `patient-rehab` 一条约束）、任务组之间存在跨组依赖（取值表与组件是全部页面改造的前置）、预期工件体量大（18 个界面 + 9 个适配器 + 完整组件库与测试），按「存疑取 COMPLEX」的默认倾向走生成子代理。

## 执行边界

```yaml
task_groups:
  "1":
    allowed_directories:
      - app/src/main/res/values
      - app/src/main/res/layout
      - app/src/main/res/color
      - app/src/main/res/drawable
    allowed_packages:
      - app/src/main/java/com/example/winsrehab/ui
      - app/src/test/java/com/example/winsrehab/ui
    notes: "旧 40 个颜色名被 layout、drawable-shape、color-selector 三处同时引用，改名与删名必须落在同一次提交，否则资源链接断。values 目录当前只有 strings/colors/themes 三个文件，字号/间距/圆角/阴影取值新建 dimens.xml，文本外观新建 styles.xml。核对断言要覆盖 app/src/main 全量（含 drawable 里写死的 #RRGGBB），不能只扫 layout。"
  "2":
    allowed_directories:
      - app
      - app/src/main
      - app/src/main/res/values
      - app/src/main/res/color
      - gradle
    allowed_packages:
      - app/src/main/java/com/example/winsrehab
      - app/src/main/java/com/example/winsrehab/ui
      - app/src/test/java/com/example/winsrehab
    notes: "12 处主题声明逐条处置：DtInfoActivity 那处是幽灵声明（类与布局都不存在）直接删，其余 11 个在用 Activity 改挂。MainActivity.kt 与 ui/theme 三个文件的删除必须与 compose 构建项移除同组同提交，否则本组中间状态直接编译失败。版本目录新增 material 固定版本条目并同时删掉 compose 的 9 条库条目与 4 个版本条目，避免留下死配置。"
  "3":
    allowed_directories:
      - app/src/main/res/layout
      - app/src/main/res/values
      - app/src/main/res/drawable
      - app/src/test/java/com/example/winsrehab/ui/main/patient/main
    allowed_packages:
      - app/src/main/java/com/example/winsrehab/ui/common
      - app/src/main/java/com/example/winsrehab/ui/main/patient/adapter
      - app/src/test/java/com/example/winsrehab/ui/common
    notes: "状态徽标以既有 TaskStatusStyle 为基座扩展到 ui/common，禁止新旧两套映射并存；兜底分支的语义（UNKNOWN 与缺映射）必须只有一种口径。迁移时同步改 TaskInstanceAdapter：三处调用点改 import（该文件在 ui/main/patient/adapter，属本组允许范围），并从 app/src/test/.../ui/main/patient/main 把 TaskStatusStyleTest 迁到 ui/common 测试包。新布局统一 widget_*.xml 命名。"
  "4":
    allowed_directories:
      - app/src/main/res/layout
      - app/src/main/res/values
      - app/src/main/res/drawable
      - app/src/test/java/com/example/winsrehab/ui/main/patient/main
    allowed_packages:
      - app/src/main/java/com/example/winsrehab/ui/common
      - app/src/main/java/com/example/winsrehab/ui/main/patient/main
      - app/src/test/java/com/example/winsrehab/ui/common
    notes: "加载/空/错三态与占位文案的单一实现落 ui/common（不要放进患者端包），医生端页面要能直接复用。三态与占位文案要从 RehabFormatter 里拆出来（保留 progressCountText/progressPercentText/percent/barRatio/barHeightDp/dayLabel/weekCountText 这些纯格式化函数），所以本组需要同时改 ui/main/patient/main 的 RehabFormatter 与其调用点、并把 RehabFormatterTest 里迁走的三条断言落到 UiTextsTest。顶部栏组件必须同时支持标题与返回（返回行为可注入，供 WaitingBinding 这类需要「退回登录」的页面复用）；本轮要补顶部栏的是 design 里那九页清单，不是只有患者申请页一页。"
  "5":
    allowed_directories:
      - app/src/main/res/layout
      - app/src/main/res/values
      - app/src/main/res/navigation
      - app/src/androidTest
    allowed_packages:
      - app/src/main/java/com/example/winsrehab/ui/main/selectLog
      - app/src/main/java/com/example/winsrehab/ui/main/doctor/login
      - app/src/main/java/com/example/winsrehab/ui/main/patient/login
      - app/src/main/java/com/example/winsrehab/ui/main/patient/binding
      - app/src/main/java/com/example/winsrehab/ui/main/doctor/application
      - app/src/main/java/com/example/winsrehab/ui/main/doctor/home
      - app/src/main/java/com/example/winsrehab/ui/main/doctor/plan
      - app/src/main/java/com/example/winsrehab/ui/common
    notes: "患者申请页必须补顶部栏（原靠唯一一处 Theme.AppCompat.Light 的 ActionBar 提供标题与返回），改挂 NoActionBar 后不补就没有返回路径。ApplicationAdapter 已用视图绑定，不重写。strings.xml 只删两条英文与模板字符串，不整包搬 303 处文案；但删字符串前必须删掉引用它们的两个 TextView（activity_dt_log.xml:57 与 :84），否则引用悬空编译不过。账号解析取不到时返回 null 并进明确空态，禁止 `?: \"\"` 之后拿空串去查库。"
  "6":
    allowed_directories:
      - app/src/main/res/navigation
      - app/src/main/res/layout
      - app/src/main/res/values
      - app/src/androidTest
    allowed_packages:
      - app/src/main/java/com/example/winsrehab/ui/main/doctor/home
      - app/src/main/java/com/example/winsrehab/ui/main/doctor/plan
      - app/src/main/java/com/example/winsrehab/ui/main/doctor/adapter
      - app/src/main/java/com/example/winsrehab/ui/common
    notes: "totalCount 参数退役后，DtHomeActivity 里只为 count 而 new 出来的 PatientAdapter 与 submitList 一并删除。DtHomeVM 与 DtHomeViewModel 合并时保留承载 patients 观察者的那个。三处 navigate 的 Bundle 同步收敛，不要留半套旧参数。待办数与顶部通知横幅必须同一来源（getPendingTaskCount(doctorCode, \"patient_binding\")），否则又造出一处不一致。"
  "7":
    allowed_directories:
      - app/src/main/res/layout
      - app/src/main/res/values
      - app/src/androidTest
    allowed_packages:
      - app/src/main/java/com/example/winsrehab/ui/main/doctor/home
      - app/src/main/java/com/example/winsrehab/ui/main/doctor/adapter
      - app/src/main/java/com/example/winsrehab/ui/main/patient/info
      - app/src/main/java/com/example/winsrehab/ui/common
    notes: "患者卡接 Patient.rehabStage 与 Patient.overallProgress，空值走统一占位。改 PatientAdapter 时顺手删掉被注释且引用不存在字段（stage/progress/aiResult）的 4 行赋值，那是死代码。两页患者数断言必须同源 getPatientsByDoctorFlow。"
  "8":
    allowed_directories:
      - app/src/main/res/layout
      - app/src/main/res/navigation
      - app/src/main/res/values
      - app/src/androidTest
    allowed_packages:
      - app/src/main/java/com/example/winsrehab/ui/main/doctor/home
      - app/src/main/java/com/example/winsrehab/ui/main/doctor/info
      - app/src/main/java/com/example/winsrehab/ui/main/doctor/plan
      - app/src/main/java/com/example/winsrehab/ui/main/doctor/adapter
      - app/src/main/java/com/example/winsrehab/ui/common
    notes: "计划列表页的 doctorCode 必须改走统一解析并进明确空态（它现在用 `?: \"\"`）。CreateRehabPlanActivity 在 patientId 为空时直接 finish 的行为本轮不改，所以列表页不放「新建计划」入口。计划徽标覆盖四态。该页此前零入口，建 action 后纳入可达性冒烟。"
  "9":
    allowed_directories:
      - app/src/main/res/layout
      - app/src/main/res/navigation
      - app/src/main/res/values
      - app/src/androidTest
    allowed_packages:
      - app/src/main/java/com/example/winsrehab/ui/main/patient/main
      - app/src/main/java/com/example/winsrehab/ui/main/patient/adapter
      - app/src/main/java/com/example/winsrehab/ui/main/patient/info
      - app/src/main/java/com/example/winsrehab/ui/common
    notes: "患者首页与康复中心必须共用宿主作用域的同一个 VM（activityViewModels），并让周数据流只订阅一次；两处各自 viewModels 各自 collect 会直接命中本变更要拦的「两处各自计算」。pt_homeFragment 未使用的 navArgs 声明与 PtMainActivity 的传参链一并收敛。康复页既有的三态与七根柱保留，只换组件不重画。置灰入口必须移除点击监听，不能留「可点但无响应」。"
  "10":
    allowed_directories:
      - app/src/main
      - app/src/main/res
      - app/src/main/res/layout
      - app/src/main/res/drawable
      - app/src/main/res/values
      - app/src/androidTest
    allowed_packages:
      - app/src/main/java/com/example/winsrehab/ui/main/patient/psychology
      - app/src/main/java/com/example/winsrehab/ui/main/patient/Video
      - app/src/main/java/com/example/winsrehab/ui/main/patient/adapter
      - app/src/main/java/com/example/winsrehab
    notes: "死资源以 Lint UnusedResources 逐条确认，删前再检索布局与适配器引用。DtInfoActivity 只有清单声明（类与专属布局都不存在），本组不重复处理（归第 2 组）。工作区结束须 git status 干净。"
```

## Initial Capabilities

- `ui-foundation`：界面取值规范（颜色/字号/间距/圆角/阴影）、医生端与患者端双套主题、8 类可复用组件的形态与使用约束、加载/空/错三态与占位口径、「即将开放」置灰入口的可逆约束。
- `ui-page-wiring`：18 个界面的展示字段来源与占位口径、死入口处置、导航入口补全、账号与工号传递收敛、同源数字一致性约束。
- `patient-rehab`（修改）：补充「康复页本周完成数与患者首页同源同口径」的展示约束。
