# 设计：界面基建与页面业务对接

## Context

现状约束（详见 `proposal.md` 的 Why 与 Related Surface）：

- 视觉取值散落：布局内硬编码色值 389 处、字号 305 处、写死高度 229 处；颜色资源名 40 个。
- 主题不可用：应用默认主题是 Material 家族，但 Manifest 里 13 条 Activity 声明中有 12 条各自覆盖成 AppCompat（11 条 `Theme.AppCompat.Light.NoActionBar`，其中 `DtInfoActivity` 只有声明、类与布局都不存在；1 条 `PatientApplicationActivity` 的 `Theme.AppCompat.Light`，后者靠 ActionBar 提供标题与返回），真正在用的 Activity 是 11 个；`themes.xml` 里 `MyRoundButton` 样式体被写成裸文本（样式名后直接跟一行文本），`MyCheckBox` 又挂在另一套主题家族下（它是 `Theme.AppCompat.Light` 主题而非样式）。Material 组件库依赖未显式声明，靠 `navigation-ui-ktx` 传递进来才编译得过。
- 双轨隐患：`app/build.gradle.kts` 开着 `buildFeatures { compose = true }`、`composeOptions`、声明了 Material3；除模板生成的 `MainActivity.kt`（Compose 页面）与配套 `ui/theme/{Color,Type,Theme}.kt` 外全项目零 Compose 代码——移除 Compose 构建项会让这个模板页直接编译失败，两者必须同组下线。仓库只有单套 `values/`、无 `values-night`，主题却声明为 `DayNight`，而界面实际只按浅色设计。
- 假数据与死入口集中在两个首页：医生首页整页硬编码（三条假任务卡、四个无标识的四宫格卡、无功能搜索框、「查看日历」死文字），患者首页同样（两张假任务卡、假本周完成率与七根硬编码柱、两张无标识死卡、三个无标识快捷卡）。
- 接线断点：`RehabPlanListFragment` 不在任何导航图、全项目零引用；账号/工号存在三条解析路径（`navArgs` / `arguments?.getString` / 宿主 `Intent` 兜底）；医生首页两个同义数据控制器并存（`DtHomeViewModel` 与 `DtHomeVM`）；`dt_info_Fragment` 的患者数靠导航参数 `totalCount` 由宿主页填充，宿主为此在 `DtHomeActivity` 里建了一个只为数条数的适配器。
- 数据侧可复用：`TaskExecutionRepository.observeRehabWeek` 一次带出「今天 + 本周区间 + 本周任务」（`RehabWeekData`），康复页四处数字已同源；`PatientDao.getPatientsByDoctorFlow`、`DoctorTaskDao.getPendingTasksByDoctor/PendingTaskCount`、`PatientRepository.getPatientsByDoctorOnce` 均已是只读查询。
- 项目记忆约束：Kotlin 文件要有通俗单行 `//` 注释、禁多行注释块；ViewModel 不直接访问 DAO 必须经 Repository；Fragment 传参用 Bundle/arguments、Activity 用 Intent extras；日志用 `Log.i("ClassName", "描述: $value")`。

## Goals / Non-Goals

**Goals:**

- 一套取值表 + 双端主题（医生蓝 / 患者绿）作为全部界面的唯一视觉来源，页面里不再出现字面色值与裸字号。
- 8 类可复用组件有明确落点与使用边界，9 个列表适配器全部改造到组件上。
- 18 个界面的改造逐页有清单、可逐页验收；每页都有加载/空/错三态。
- 假数据逐处有落点：能算的接真实值且与已有同源展示同口径，算不出的统一占位，撤掉的补齐空态。
- 导航与参数收敛到单一入口；孤儿页接回；同义数据控制器合并。
- 界面自动化测试给出 5 条关键路径，跑起来就能回归。

**Non-Goals:**

- 不做任何表结构、字段、索引、迁移改动（零 DDL）。
- 不做摄像头录制、视频审核页、反馈页、放松中心、计划调整界面、环形进度自绘控件。
- 不接患者表冗余统计字段（`rehabDays`、`todayTasksCompleted`、`todayTasksTotal`、`weeklyCompletionRate`、`hasNewFeedback`、`lastActiveTime`），也不补反馈表的访问入口。
- 不改任何业务规则、校验与状态机（沿用 `openspec/specs/task-execution/spec.md` 与 `openspec/specs/patient-rehab/spec.md`）。
- 不新增业务功能：患者管理页的按姓名/诊断筛选本轮移除不可用的搜索框，筛选能力留待后续变更。
- 不引入截图基准测试（视觉回归靠人工走查留档）。

## Decisions

### D1 取值表组织方式：语义色 + 固定档位 + 文本外观

**选择**：颜色落 `res/values/colors.xml`，收敛为 26 个语义色 + 7 个浅色容器底（见下，合计 33 个）；间距/圆角/阴影/尺寸落 `res/values/dimens.xml`（`space_4/8/12/16/24/32/48` + `row_trailing_reserve`、`radius_8/12/16`、`elevation_0/1`；`0dp` 表示无间距、`1dp` 是描边线宽，两者都不计入间距档；`space_48` 只用于页面底部与列表底部的统一留白，列表行右侧的 48dp 固定留白单独走 `row_trailing_reserve`——数值与 `space_48` 同源但语义不同，测试同时锁定两者取值相等；这两档都不允许当普通内外边距用；现状里 56dp 这类单点留白就近并入 `space_48`）；字号不落 `dimen`，落 `res/values/styles.xml` 的文本外观（`TextAppearance.WinsRehab.Caption11 / Body12 / Body14 / Title16 / Headline20 / Display24`）。

**理由**：字号写成 `dimen` 会被当成尺寸滥用（有人拿它做 margin），文本外观能同时锁住字号、字重与行距，且在布局里语义明确；用 `textAppearance` 后「6 档」这条约束可被静态检查（grep `android:textSize` 应为零命中）。

**备选**：全部塞 `colors.xml` + 裸 `dimen`（放弃——管不住字重与行距，也管不住「6 档」）；引入设计令牌代码生成（放弃——本轮 XML 路线，生成器成本高于收益）。

**语义色清单（26 个）**：`brand_doctor_primary`、`brand_doctor_primary_dark`、`brand_patient_primary`、`brand_patient_primary_light`、`brand_patient_primary_dark`(#15803D)、`brand_patient_primary_deep`(#166534)、`surface`、`surface_variant`、`on_surface`、`on_surface_secondary`、`on_surface_tertiary`、`outline`、`divider`、`primary`、`on_primary`、`success`、`warning`、`danger`、`info`、`disabled_bg`、`disabled_fg`、`scrim`、`status_pending`、`status_completed`、`status_skipped_rejected`、`status_review`。计划状态色复用 `status_*` 族，不另开一组。患者端之所以要两档深绿：品牌主色 `brand_patient_primary`(3.30:1) 与辅绿 `brand_patient_primary_light`(2.28:1) 都过不了白字门槛，绿底按钮与绿底欢迎卡必须换 `brand_patient_primary_dark`(5.02:1) / `brand_patient_primary_deep`(7.13:1)，原两档只作装饰填充与图标，不再作正文字（浅底上作文字请用 `brand_patient_primary_dark`(5.02:1) / `brand_patient_primary_deep`(7.13:1)）。本轮同时把两个「看着像文字色、实测过不了门槛」的令牌就地加深，而不是逐页改字色：`on_surface_tertiary` #9CA3AF → **#6B7280**（作文字 surface 4.83:1、surface_variant 4.63:1），`warning` #EA580C → **#9A3412**（作文字 7.31:1、6 个容器底上 5.99–6.80:1）；`brand_patient_primary_light` 保持 #22C55E、`status_review` 保持 #EA580C，二者降为「只作装饰填充与图标、不作文字」。

**浅色容器底档（7 个）**：`primary_container`(#DBEAFE)、`success_container`(#DCFCE7)、`warning_container`(#FFEDD5)、`info_container`(#CCFBF1)、`accent_container`(#FAE8FF，紫粉强调)、`notice_container`(#FEF9C3，黄)、`on_primary_overlay`(#33FFFFFF，品牌深色底上的半透明容器层)。这 7 个是「彩色卡片 / 图标圆底 / 标签底」的唯一来源，合计 26 + 7 = 33 个颜色定义。

**容器底与文字色的搭配约束（可执行达标组合表）**：6 个容器底上只允许下列 5 个文字色，全部实测 ≥5.49:1、对任一容器底通用，不需要逐页记忆例外——「容器底配对应语义前景色」的旧说法作废（`primary_container` 配 `status_pending` 只有 4.24:1、`accent_container` 配 `primary` 只有 4.44:1，都过不了 4.5 门槛）。

| 文字色 | `primary_container` | `success_container` | `warning_container` | `info_container` | `accent_container` | `notice_container` |
| --- | --- | --- | --- | --- | --- | --- |
| `on_surface` | 12.03:1 | 13.37:1 | 12.81:1 | 13.03:1 | 12.62:1 | 13.67:1 |
| `on_surface_secondary` | 6.19:1 | 6.88:1 | 6.60:1 | 6.71:1 | 6.50:1 | 7.04:1 |
| `warning` | 5.99:1 | 6.65:1 | 6.38:1 | 6.48:1 | 6.28:1 | 6.80:1 |
| `brand_patient_primary_deep` | 5.84:1 | 6.49:1 | 6.22:1 | 6.33:1 | 6.13:1 | 6.64:1 |
| `brand_doctor_primary_dark` | 5.49:1 | 6.10:1 | 5.85:1 | 5.95:1 | 5.76:1 | 6.24:1 |

容器底上**禁止作文字**（全都不稳定过 4.5，用了就是对比度缺陷）：`primary` = `brand_doctor_primary` = `status_pending`（4.24–4.81，`primary_container` 4.24 / `accent_container` 4.44 直接不足）、`danger` = `status_skipped_rejected` = `on_surface_tertiary`（3.96–4.50）、`brand_patient_primary_dark`（4.11–4.67，仅 success/notice 两底勉强过）、`info`（3.01–3.42）、`success` = `brand_patient_primary` = `status_completed`（2.70–3.07）、`status_review`（2.92–3.31）、`brand_patient_primary_light`（<3.0）、`on_primary`（1.07–1.22）。容器上要表达「待执行/进行中」这类状态语义时，文字用 `brand_doctor_primary_dark` 而不是 `status_pending`（或反过来把 `status_pending` 当实底、文字配 `on_primary` 白字，见 `item_rehab_plan.xml` 的进行中胶囊）；要表达成功语义时用 `brand_patient_primary_deep` 而不是 `success`。

`on_primary`（白字/白图标）只允许落在下表「白字可用」列的实底上。白字压在容器底或 `surface_variant` 上属于对比度故障（1.05:1），不允许。

**量化对比度门槛（可被测试执行）**：文字与其最近的不透明底的对比度按 WCAG 相对亮度公式计算——`L = 0.2126R + 0.7152G + 0.0722B`（R/G/B 先做 `c/255`，≤0.03928 取 `c/12.92`，否则取 `((c+0.055)/1.055)^2.4`），对比度 `(L_亮 + 0.05) / (L_暗 + 0.05)`。门槛分两档：

- **正文（任意字号 ≤16sp，以及所有非 bold 文本）≥ 4.5:1**；
- **大字（≥18sp 常规，或 ≥14sp bold）允许 ≥ 3.0:1**。

底色的解析口径固定为三类来源：`android:background` 指向的 `@color`，或指向的 `@drawable` 里 `<solid android:color>` / `<gradient>` 的各档色值（渐变按**最差一端**算），以及 `android:backgroundTint` 与 `app:cardBackgroundColor`；`backgroundTint` 存在时以 tint 为准（tint 会覆盖 shape 自带填充色）。半透明底（`scrim`、`on_primary_overlay`、`#33FFFFFF` 这类 alpha < 255 的值）、图片底、以及向上找不到任何底的控件，一律判定不合格——要白字就先铺不透明底板（如 `bg_chip` + `backgroundTint="@color/on_surface"`）。`android:alpha` 会按与白色的混合结果重算，靠 0.9 透明度「调浅」白字等于自降对比度，不视为达标手段。

**对比度实测清单（写入 `colors.xml` 逐个注释）**：同一套门槛对两个方向同时成立——「白字压在色上」与「色作文字压在浅底上」；只做前者是半个闭环，本轮补的就是后者。三档表述固定为：**A 档 ≥4.5:1** 可承载白字，也可作任意字号文字；**B 档 3.0–4.5:1** 只作 ≥18sp 或 ≥14sp bold 的大字、深底上的文字/图标与装饰填充；**C 档 <3.0:1** 禁作任何文字与其底，只作描边、分隔线、遮罩与纯装饰。

(a) 白字可用性（决定能不能放 `on_primary` 文字与白图标）：

| 白字可用（≥4.5:1） | 白字不可用（<4.5:1） |
| --- | --- |
| `on_surface` 14.68:1、`on_surface_secondary` 7.56:1、`warning` 7.31:1、`brand_patient_primary_deep` 7.13:1、`brand_doctor_primary_dark` 6.70:1、`primary` = `brand_doctor_primary` = `status_pending` 5.17:1、`brand_patient_primary_dark` 5.02:1、`danger` = `status_skipped_rejected` = `on_surface_tertiary` 4.83:1 | `info` 3.68:1、`status_review` 3.56:1、`brand_patient_primary` = `success` = `status_completed` 3.30:1、`disabled_fg` 2.54:1、`brand_patient_primary_light` 2.28:1、`outline` 1.47:1、`divider` = `disabled_bg` 1.24:1、`surface_variant` 1.05:1、`surface` 1.00:1、6 个浅色容器底 1.07–1.22:1 |

(b) 彩色文字可用性（色作文字压在 `surface` 上，决定正文字能不能用这个色）：

| A 档 ≥4.5:1（任意字号可作文字） | B 档 3.0–4.5:1（只够大字） | C 档 <3.0:1（禁作文字） |
| --- | --- | --- |
| `on_surface` 14.68:1、`on_surface_secondary` 7.56:1、`warning` 7.31:1、`brand_patient_primary_deep` 7.13:1、`brand_doctor_primary_dark` 6.70:1、`primary` = `brand_doctor_primary` = `status_pending` 5.17:1、`brand_patient_primary_dark` 5.02:1、`danger` = `status_skipped_rejected` = `on_surface_tertiary` 4.83:1 | `info` 3.68:1、`status_review` 3.56:1、`brand_patient_primary` = `success` = `status_completed` 3.30:1 | `brand_patient_primary_light` 2.28:1、`outline` 1.47:1、`divider` = `disabled_bg` 1.24:1、`surface` / `surface_variant` / 6 个容器底（只作底，不作文字） |

`disabled_fg`(#9CA3AF, 2.54:1) 是失效控件前景色，按 WCAG 1.4.3 对失效控件的豁免不做文字对比度考核，但它只允许出现在 `enabled=false` 的控件上，不能当普通提示文字色用。

**本轮据此改掉的 11 处布局文字色**（守卫实测的 41 处违规里，20 处靠 `on_surface_tertiary` 加深到 4.83:1、4 处靠 `warning` 加深到 7.31:1 直接转绿，其余 11 处必须换色）：

- 压在容器底上的状态/链接文字：`fragment_dt_home.xml:229`（15位患者）、`:283`（3个待审核）、`:320`（2个待制定）、`fragment_pt_home.xml:193`（查看全部）、`:384`（随时可用）、`:414`（120+ 视频）、`item_key_patient.xml:103`（肌力↑）——`primary` / `status_pending` 一律换 `brand_doctor_primary_dark`，`brand_patient_primary` / `success` 一律换 `brand_patient_primary_dark`。
- `item_rehab_plan.xml:66`（进行中）走另一条路：`status_pending` 压在 `primary_container` 上只有 4.24:1，把状态胶囊整体从「浅蓝底 + 彩字」改成「`status_pending` 实底 + `on_primary` 白字」（5.17:1，本来就是白字可用清单里的达标组合）——语义色交给底承载，文字回到白字档；这也保住了 `status_pending` 这个令牌的唯一使用点，否则它会变成 Lint UnusedResources 报的死资源。
- `fragment_pt_home.xml:471`（3条新消息）压在「`surface` → `info_container`」渐变上，最差端只有 2.92:1，必须换 `brand_patient_primary_deep`（6.33:1）；`brand_patient_primary_dark` 在 `info_container` 上只有 4.45:1，差一点过不去。
- `activity_create_rehab_plan.xml:286` 的「点击右上角『添加任务』开始创建」原先把 `outline`(1.47:1) 当文字色用，改 `on_surface_tertiary`(4.63:1)；`outline` 从此只作描边。

3.0–4.5 之间的那几档（`info` / `brand_patient_primary` / `success` / `status_completed` / `status_review`）理论上只够承载大字，本轮一律不用它们兜白字或兜正文字：`bg_rounded_btn_blue` 改 `primary`，`bg_btn_green_small` / `bg_btn_green_rounded` / `bg_user_bubble` 改 `brand_patient_primary_dark`，`bg_pt_welcome_green` 改 `brand_patient_primary_deep → brand_patient_primary_dark` 渐变，登录页与心理助手按钮的 `backgroundTint` 同理换深绿；`bg_dt_welcome_card`(`info → primary`) 与品牌渐变同色后已无区分意义，删除并统一用 `bg_brand_gradient`(`primary → brand_doctor_primary_dark`，两档 5.17/6.70:1)。

**彩色底不降级为中性灰**：基线里被压成 `surface_variant` 的 11 类自绘彩色底（`#DBEAFE`/`#DCFCE7`/`#FFEDD5`/`#F3E8FF`/`#CCFBF1`/`#FEF9C3`/`#FAE8FF`/`#E3F2FD` + 4 组 3D 卡渐变 + 4 组功能卡渐变 + `#33FFFFFF` 白叠层）逐一对齐到上面 6 个容器档：卡片之间、图标圆底之间靠容器档区分语义，不再整站变灰。渐变形态保留，但只允许两端都是取值表里的颜色——「`surface` → 容器档」（3D 卡、功能卡）；原先靠 `#203B82F6` 这类字面量做的彩色投影与高光层删除，改用 1dp 同色描边。主 CTA 与品牌页头改走 `primary` → `brand_doctor_primary_dark` 的品牌渐变（`bg_brand_gradient` / `bg_button_primary`），不再借用 `warning` 暖色当主按钮色——暖色白字对比度只有 2.5:1，且 `warning` 语义是「警示/待处理」。

**圆形与胶囊不占圆角档**：spec 的圆角是硬性的 8/12/16 三档，圆形/胶囊属于「特殊形状自绘例外」，用 `<shape android:shape="oval">` 表达（`shape_logmap`、`bg_avatar_circle`、`shape_circle_*_light`、`shape_circle_red`），不新增 `radius_full` 档，也不把正圆圆角就近折成 `radius_16`。72dp 头像位因此由 CardView 改 `FrameLayout` + oval 底，仍保留原 id `avatar_container`。

**旧颜色名处置**：40 个旧名在本变更内被消费方替换完后删除；不保留兼容别名（全页面同批改造，编译期即可暴露遗漏）。

### D2 双端主题：一个基础主题 + 两个派生主题

**选择**：`Theme.WinsRehab`（保留原名，parent `Theme.MaterialComponents.Light.NoActionBar`，作为基础主题并**锁定浅色**）；`Theme.WinsRehab.Doctor` 与 `Theme.WinsRehab.Patient` 以点号隐式继承派生，仅覆盖主题色与按钮默认样式。`<application>` 的默认主题保持 `Theme.WinsRehab`（启动选择页这类未区分角色的界面用它）；进入角色域后，宿主 Activity（医生端 `DtHomeActivity`、患者端 `PtMainActivity`）及其余界面挂各自端主题。Manifest 里现有 **12 处** `Theme.AppCompat.*` 覆盖，处置分两类：其中 `AndroidManifest.xml:42-45` 的 `DtInfoActivity` 指向不存在的类（见 D7），删除该声明；其余 **11 个在用 Activity** 把 `android:theme` 改挂双端主题。完成判据是「Manifest 中引用 `Theme.AppCompat.*` 的 activity 数 = 0」，而不是数着改了 11 还是 12 个。

**理由**：点号继承让基础主题的 `android:theme` 声明零改动；两端差异只在颜色，符合「共享取值与组件、只换主色」。

**为什么锁浅色而不是继承 `DayNight`**：现状 12 处覆盖全是 `Theme.AppCompat.Light.*`，界面实际只按浅色设计；仓库只有单套 `values/`、无 `values-night`，全项目也无 `setDefaultNightMode` / `forceDarkAllowed` 相关代码。若基础主题继承 `DayNight`，全部界面会开始跟随系统深色，而取值表仍是固定浅色，Material 的深色属性会与原色值混色；补齐深色需要另起一整套 `values-night` 语义色并逐页验证对比度，属于新增设计面，超出本轮「统一现有外观」的范围。因此本轮锁定浅色，**这是维持现状语义而非改变行为**；深色支持作为后续独立变更登记（交付物为 `values-night` 全套语义色 + 深色下 18 页走查）。若后续要开，只需把基础主题 parent 换回 `DayNight` 并补齐 `values-night`，页面与组件无需改动。

**备选**：两个独立主题各写全量（放弃——取值改两遍，正是本轮要消除的维护面）；用启动页动态换主题（放弃——运行期换主题容易造成控件重建闪烁）；继承 `DayNight` 并同批补 `values-night`（放弃——本轮范围不含新增深色设计面，且深色需要逐页核对对比度与状态色可辨识度，会挤掉页面接线的主线工作）。

**底部导航选中态**：4 个 selector 的选中色改引用 `brand_doctor_primary` / `brand_patient_primary`，未选中统一引用 `on_surface_secondary`。

**全局 `buttonStyle`**：删除 `Theme.WinsRehab` 里指向坏样式的 `buttonStyle`，改由 Material 主题的默认按钮样式承接（第 2 组即可做，可编译）。坏样式 `MyRoundButton` 本体与 `MyCheckBox` 不能同组删——它们仍被 5 处按钮（`activity_dt_log.xml:144,158`、`activity_pt_log.xml:146,160`、`activity_psychology.xml:43`）与 2 处 `android:theme`（`activity_dt_log.xml:132`、`activity_pt_log.xml:133`）引用，早删 AAPT 直接失败；两者随使用点在第 5、10 组被替换后，于 10.5 收尾删除。

**Material 依赖（显式固定版本）**：`gradle/libs.versions.toml` 的 `[versions]` 增 `material = "1.12.0"`、`[libraries]` 增 `material = { group = "com.google.android.material", name = "material", version.ref = "material" }`；`app/build.gradle.kts` 的 `dependencies` 增 `implementation(libs.material)`。

选 `1.12.0` 的理由：现状该库不在 `libs.versions.toml` 里，只经 `navigation-ui-ktx` 传递进来，Gradle 缓存中同时存在 1.10.0 与 1.12.0，所以「沿用传递版本」这件事既不显式也不可核验。1.12.0 提供本变更要用的 `Theme.MaterialComponents.Light.NoActionBar` 全部属性、与 `compileSdk 36` / `minSdk 26` / `AppCompatActivity` 组合兼容，且已是缓存中的版本，锁定它不会把 Navigation 的传递版本往下拉。

**依赖收敛清单（与 Compose 下线同组）**：
- `app/build.gradle.kts` 删 `buildFeatures` 里的 `compose = true`（保留 `viewBinding = true`、`buildConfig = true`）、删整个 `composeOptions { kotlinCompilerExtensionVersion = "1.5.7" }` 块。
- 删 8 条 Compose 依赖声明：`implementation` 下的 `libs.androidx.activity.compose`、`libs.androidx.ui`、`libs.androidx.ui.graphics`、`libs.androidx.ui.tooling.preview`、`libs.androidx.material3`；`debugImplementation` 下的 `libs.androidx.ui.tooling`、`libs.androidx.ui.test.manifest`；`androidTestImplementation` 下的 `libs.androidx.ui.test.junit4`。
- 删 `gradle/libs.versions.toml` 里 9 条 Compose 库条目（含未被 build 文件引用的 `androidx-compose-bom`）与 4 个 Compose 版本条目（`activityCompose`、`composeBom`、`compose`、`material3`）。
- 同组删 `MainActivity.kt`、`ui/theme/Color.kt`、`ui/theme/Type.kt`、`ui/theme/Theme.kt` 与 `AndroidManifest.xml:15-19` 的 `MainActivity` 声明——这五者与 Compose 构建项互为前提，任一侧单独下线都会让另一侧编译失败或留下指向不存在类的清单条目。

### D3 8 类组件的落点与职责边界

| 组件 | 落点 | 允许 | 不允许 | 首个使用方 |
|---|---|---|---|---|
| 按钮 | `styles.xml` 的 `Widget.WinsRehab.Button.Primary/Secondary/Danger/Text` + Material 按钮控件 | 四种语义 × 尺寸档 × 禁用态 | 自建 `LinearLayout` 假按钮、用图片做按钮 | 医生登录页 |
| 输入框 | `Widget.WinsRehab.TextInputLayout` + Material 输入控件 | 标签、必填标记、错误文案、辅助文本 | 裸 `EditText`、无标签输入 | 患者登录页 |
| 卡片 | `Widget.WinsRehab.Card` / `.Clickable` / `.StatusBar` + Material 卡片控件 | 三种形态、可含状态条 | 一个页面混用两种圆角与阴影 | 患者管理列表 |
| 状态徽标 | `layout/widget_status_badge.xml` + `ui/common/TaskStatusStyle.kt`（状态 → 文案/语义色）；映射基座是既有 `TaskStatusStyle`，迁入 `ui/common` 后扩展计划四态，**不新建第二套映射器** | 任务五态 + 计划四态 | 页面内写状态颜色分支；与既有映射并存第二套 | 计划列表 |
| 三态 | `layout/widget_state_loading.xml` / `widget_state_empty.xml` / `widget_state_error.xml` | 统一文案与重试入口 | 每页自写空态句子 | 计划列表 |
| 分区标题 + 信息行 | `Widget.WinsRehab.SectionTitle`、`layout/widget_info_row.xml` | 同级标题样式一致、标签+值结构 | 手工加粗伪标题 | 医生我的 |
| 对话框 | `Widget.WinsRehab.Dialog` + Material 对话框构建器（确认/三选一/底部弹层） | 三种交互、统一主题渲染 | 混用系统默认对话框 | 康复中心任务确认 |
| 顶部栏 + 底部导航 | `layout/widget_top_bar.xml` + 双端主题 + 现有底部导航 | 端内结构一致、选中态取主题色 | 每页各写一套顶部间距 | 医生首页 |

**状态映射的唯一归属（避免两套并存）**：现状已有一份状态映射——`app/src/main/java/com/example/winsrehab/ui/main/patient/main/TaskStatusStyle.kt`（`TaskVisual` 枚举 + `visualOf` / `isActionable` / `labelOf`，含 `UNKNOWN` 兜底），已被 `TaskInstanceAdapter.kt` 第 54／61／65 行三处生产使用，并有 `app/src/test/java/com/example/winsrehab/ui/main/patient/main/TaskStatusStyleTest.kt`。本轮**以它为基座而不是另起一套**：

1. 把文件整体迁到 `ui/common`（包名改 `com.example.winsrehab.ui.common`），同批把 `TaskStatusStyleTest` 迁到 `app/src/test/java/com/example/winsrehab/ui/common/`，并按新包名改写。
2. 在同一对象内补计划四态映射（`PlanVisual` 或复用 `TaskVisual` 的取值 + `planVisualOf` / `planLabelOf`），使「任务五态 + 计划四态」共用一处定义。
3. `TaskInstanceAdapter.kt` 三处调用点同步改 import，不保留旧包下的转发壳。
4. **兜底策略统一为一条**：映射表必须覆盖 `TaskStatus.ALL`（5 值）与 `PlanStatus.ALL`（4 值）的全部取值，由单测逐值断言（双向完备：每个合法状态有映射、每个映射对应一个合法状态）。运行时保留 `UNKNOWN` 分支，用于状态列这类自由字符串字段中的脏值／历史值——脏值让整页崩溃的代价高于显示一个「未知状态」徽标；但该分支必须（a）文案为「未知状态」、（b）用 `danger` 语义色、（c）`Log.e` 记录原值，使其在视觉与日志上都与任何合法状态可区分。**禁止**的做法是让缺映射的取值静默落到某个合法状态的样式上——那才是「以默认样式兜底」；本项目用单测把「缺映射」挡在交付前，用可区分的 `UNKNOWN` 兜住运行期脏数据。

**理由**：能由样式表达的不建自定义视图（少写代码、自动跟随主题）；只有「状态映射」这类需要逻辑的才配纯函数映射器，既便于 JVM 单测，也避免页面里散落 `when` 分支。

### D4 18 个界面的改造清单

每页固定三件事：换组件、换取值、补三态。

| # | 分组 | 页面（布局 / 界面） | 改造要点 |
|---|---|---|---|
| 1 | 入口与身份 | `activity_select_log.xml` / `SelectLogActivity` | 英文改中文；两个角色入口换可点击卡片；补基础主题挂载 |
| 2 | 入口与身份 | `activity_dt_log.xml` / `DtLogActivity` | 输入框换统一输入框（工号/密码必填 + 错误文案）；按钮换主按钮；英文改中文 |
| 3 | 入口与身份 | `activity_pt_log.xml` / `PtLogActivity` | 同 #2（手机号/密码） |
| 4 | 入口与身份 | `activity_waiting_binding.xml` / `WaitingBindingActivity` | 等待中/已通过/被拒绝三态；状态徽标；主次按钮 |
| 5 | 入口与身份 | `activity_patient_application.xml` / `PatientApplicationActivity` | 申请列表换卡片 + 徽标；确认/拒绝换主/次按钮；补空态；**补顶部栏**（标题 + 返回，返回 = `finish()`）——该布局自身没有标题与返回控件，现状全靠唯一一处 `Theme.AppCompat.Light` 的 ActionBar 提供，换成 NoActionBar 后会变成无标题无返回的裸页 |
| 6 | 医生端 | `fragment_dt_home.xml` / `dt_homeFragment` | 撤整页假数据；管理患者数接真、待处理事项接真；「今日任务」区改「最新患者申请」；四宫格 2 可用 2 置灰；移除无功能搜索框与「查看日历」死文字；合并同义数据控制器 |
| 7 | 医生端 | `fragment_dt_pt_manage.xml` / `dt_pt_manageFragment` | 患者卡接真实康复阶段与进度（去掉 AI 评估行）；移除无功能搜索框；空态换统一组件 |
| 8 | 医生端 | `fragment_dt_info_.xml` / `dt_info_Fragment` | 患者数改自行观察（退役 `totalCount` 参数）；本月完成/患者满意度退占位；信息行组件化 |
| 9 | 医生端 | `fragment_dt_info_edit.xml` / `DtInfoEditFragment` | 输入框与按钮统一；错误文案统一；顶部栏 |
| 10 | 医生端 | `activity_create_rehab_plan.xml` / `CreateRehabPlanActivity` | 3 处输入框统一；日期区间校验的错误提示改统一错误文案（判定逻辑本身不动） |
| 11 | 医生端 | `fragment_rehab_plan_list.xml` / `RehabPlanListFragment` | 挂进导航图并补入口；计划卡 + 计划状态徽标；补三态；条目点击带患者身份进创建计划 |
| 12 | 患者端 | `fragment_pt_home.xml` / `pt_homeFragment` | 今日完成数/本周完成率/今日任务/本周进度接真实同源数据；第 N 天占位；病友社区与医生反馈置灰；快捷功能 1 接 2 置灰；移除铃铛/切换账户死件 |
| 13 | 患者端 | `fragment_pt_rehab.xml` / `pt_rehabFragment` | 组件化；开始/停止录制置灰「即将开放」；环形区保持占位；任务确认对话框换统一对话框；底部统计卡占位口径统一 |
| 14 | 患者端 | `fragment_pt_info.xml` / `pt_infoFragment` | 信息行组件化；按钮统一（该页是患者端「我的」tab 根页，不加顶部栏与返回） |
| 15 | 患者端 | `fragment_pt_info_edit.xml` / `PtInfoEditFragment` | 输入框与按钮统一；错误文案统一 |
| 16 | 独立功能页 | `activity_psychology.xml` / `PsychologyActivity` | 顶部栏；消息气泡色改语义色；输入区换统一输入框 + 发送按钮；三态 |
| 17 | 独立功能页 | `activity_training_video.xml` / `TrainingVideoActivity` | 视频卡换卡片组件；补空态与错误态；顶部栏 |
| 18 | 独立功能页 | `activity_video_player.xml` / `VideoPlayerActivity` | 顶部栏；播放控件色改语义色；文件缺失时错误态 |

**顶部栏适用页清单**：凡不是底部导航根页（医生端三 tab、患者端三 tab 的根 Fragment）的界面，一律使用 `widget_top_bar.xml` 提供「标题 + 返回」：`WaitingBindingActivity`、`PatientApplicationActivity`、`DtInfoEditFragment`、`CreateRehabPlanActivity`、`RehabPlanListFragment`（从首页进入，非 tab 根）、`PtInfoEditFragment`、`PsychologyActivity`、`TrainingVideoActivity`、`VideoPlayerActivity`。根页沿用各自布局内已有的标题区，不额外加返回。**例外（流程入口不加返回）**：`SelectLogActivity`、`DtLogActivity`、`PtLogActivity` 三页是流程起点，没有「上一页」可言，只保留标题、不放返回；`WaitingBindingActivity` 的返回语义是退回登录，按钮文字与跳转由本组显式指定，不复用普通二级页的 `finish()`。`PatientApplicationActivity` 是本轮唯一「现状靠 ActionBar 兜住、换主题后必须有新增控件」的页面。

**适配器**：7 个旧式查找控件的适配器（`PatientAdapter`、`PatientForPlanAdapter`、`PatientPlanGroupAdapter`、`RehabPlanAdapter`、`TrainingTaskAdapter`、`TaskInstanceAdapter`、`ChatAdapter`）随宿主页改造为视图绑定 + 组件布局；2 个已用视图绑定（`ApplicationAdapter`、`DemoVideoAdapter`）只换取值与卡片组件。每个适配器改造时顺带删掉宿主页里的死代码（如 `PatientAdapter` 中被注释且引用不存在字段的 4 行赋值）。

### D5 假数据逐处落点

| 位置 | 现状 | 处置 | 数据来源 / 文案 |
|---|---|---|---|
| 医生首页 欢迎卡「管理患者 15」 | 硬编码 | 接真实计数 | `PatientDao.getPatientsByDoctorFlow` 条数 |
| 医生首页 欢迎卡「待审核 3」 | 硬编码 | 退占位 | 与「累计训练」同一占位符 |
| 医生首页 欢迎卡「待处理 5」 | 硬编码 | 接真实 | `DoctorTaskDao.getPendingTaskCount(doctorCode, "patient_binding")`，与通知横幅同源 |
| 医生首页 `tv_brief`「今天有 3 个待审核视频，2 条患者反馈」 | 假句 | 撤掉，改为真实待办摘要；为 0 时不显示 | 同上待办计数 |
| 医生首页四宫格「15位患者」 | 硬编码 | 接真实 | 患者列表条数；点击进患者管理 |
| 医生首页四宫格「3个待审核」 | 硬编码 | 置灰「即将开放」，去掉假数字 | — |
| 医生首页四宫格「2个待制定」 | 硬编码 | 入口接计划列表，副标改「查看计划」，去掉假数字 | — |
| 医生首页四宫格「5条新反馈」 | 硬编码 | 置灰「即将开放」，去掉假数字 | — |
| 医生首页「今日任务」三条假卡（张先生/李女士/王先生） | 假人名假事件 | 整区改「最新患者申请」列表；无待办显空态 | `DoctorTaskDao.getPendingTasksByDoctor(doctorCode, "patient_binding")` |
| 患者管理 患者卡「瘀滞期 / 60% / 握力↑」 | 布局默认值 + 赋值被注释 | 接真实康复阶段与总体进度；AI 评估行（无数据来源）移除 | `Patient.rehabStage`、`Patient.overallProgress` |
| 患者首页 姓名「张先生」 | 布局默认值 | 接真实 | 已有 `PtInfoVM` 观察 |
| 患者首页「第 28 天」 | 硬编码 | 退占位 | 无数据来源 |
| 患者首页「本周完成 85%」 | 硬编码 | 接真实 | `observeRehabWeek` 的周数据，与康复页同一区间 |
| 患者首页「累计天数 28天」 | 硬编码 | 退占位 | 无数据来源 |
| 患者首页「今日任务 2/4」 | 硬编码 | 接真实 | 同一次周数据的今日完成任务数 / 今日任务数 |
| 患者首页 两张假任务卡（上肢功能训练/下肢平衡练习） | 假任务 | 接真实今日任务（按优先级取前 2 条）；无任务显空态；「查看全部」进康复中心 | 同一次周数据 |
| 患者首页「本周完成率 85%」+ 进度条 85 + 「已完成 6/7 项」+ 七根柱 | 全部硬编码 | 全部接真实，与康复页柱状图同源同口径 | 同一次周数据 |
| 患者首页「训练视频库 120+ 视频」 | 假数字 | 去掉数字，只保留描述文案 | — |
| 患者首页 医生反馈卡红色徽标「3」「3条新消息」 | 假徽标 | 卡片置灰「即将开放」，去掉徽标与假数字 | — |
| 医生我的「本月完成 0」「患者满意度 0%」 | 布局默认值 | 退占位 | 无数据来源 |
| 康复中心「累计训练」「连续打卡」 | 已是占位 | 保持（作为全项目占位基准） | — |
| 康复中心「本周完成」「今日进度」等 | 已接真实 | 保持不动 | `observeRehabWeek` |

**占位口径（唯一实现落 `ui/common`）**：全项目唯一占位符是康复页现有形态（`--`），但它现在长在患者端包里——`app/src/main/java/com/example/winsrehab/ui/main/patient/main/RehabFormatter.kt` 第 26 行的 `placeholderText()`，同文件还有 `emptyText()` 与 `errorText(message)`。医生端（待审核数、本月完成、患者满意度）与计划列表都要用同一份文案，跨端引用患者端包会形成反向依赖，而同批新增的状态映射器与 `AccountArgs` 都落在 `ui/common`。

因此：把 `placeholderText()` / `emptyText()` / `errorText()` 三项**上收**到新建的 `ui/common/UiTexts.kt`，`RehabFormatter` 只保留康复页自身的格式化职责（`progressPercentText`、`progressCountText`、`percent`、`barRatio`、`barHeightDp`、`dayLabel`、`weekCountText`）；`RehabFormatterTest` 里针对这三项的断言随迁到 `app/src/test/.../ui/common/UiTextsTest.kt`，`RehabFormatterTest` 只留格式化断言。康复页原有的三态与占位展示行为不变，只是文案来源换成 `UiTexts`。

### D6 导航与参数收敛

- **计划列表接回**：`dt_bot_nav.xml` 新增 `<fragment android:id="@+id/rehab_plan_list" android:name="...RehabPlanListFragment">`，参数 `doctorCode`（string，必填）；从 `dt_homeFragment` 加 action。入口为医生首页「制定计划」四宫格。列表条目点击维持现有行为（带 `patientId` 进创建计划页）。不在列表页上放「新建计划」——`CreateRehabPlanActivity` 在 `patientId` 为空时会直接结束，必须有患者身份，这条约束不改。
- **账号/工号单一解析链（消费方全量）**：新增 `ui/common/AccountArgs.kt`，提供 `resolveAccount(fragment)` 与 `resolveDoctorCode(fragment)`：先取 `arguments`（导航参数），为空再取宿主 Activity 的 `Intent` extras，两者都空返回 `null`。页面拿到 `null` 时进明确空态，不再用空串继续查询。

  现状有 **9 处 Fragment 侧取参声明（8 处真正读取，`pt_homeFragment` 的声明后从未被读，属死声明）**，三套写法连「参数缺失时的行为」都不一致：

  | 消费方 | 现状写法 | 参数缺失时 |
  |---|---|---|
  | `dt_homeFragment.kt:22,27` | `navArgs<dt_homeFragmentArgs>()` | 抛异常 |
  | `dt_info_Fragment.kt:35,44`（另有 `:45 totalCount`） | `navArgs<dt_info_FragmentArgs>()` | 抛异常 |
  | `dt_pt_manageFragment.kt:33` | `arguments?.getString("doctorCode") ?: ""` | 静默空串，发空查询 |
  | `RehabPlanListFragment.kt:27` | `arguments?.getString("doctorCode") ?: ""` | 静默空串，发空查询 |
  | `pt_rehabFragment.kt:58-59` | 导航参数 + 宿主 `Intent` 兜底 | 静默空串 |
  | `pt_infoFragment.kt:199` | 仅宿主 `Intent` 兜底（无导航参数） | 静默空串 |
  | `ui/main/doctor/info/DtInfoEditFragment.kt:29,34` | `navArgs<DtInfoEditFragmentArgs>()` | 抛异常 |
  | `ui/main/patient/info/PtInfoEditFragment.kt:28,33` | `navArgs<PtInfoEditFragmentArgs>()` | 抛异常 |
  | `ui/main/patient/main/pt_homeFragment.kt:20` | `navArgs<pt_homeFragmentArgs>()` 声明后未使用 | 无（死声明，随本轮删除） |

  八处读取点分两批改：第 5 组改 6 处（`dt_homeFragment`、`dt_info_Fragment`、`dt_pt_manageFragment`、`RehabPlanListFragment`、`pt_rehabFragment`、`pt_infoFragment`），第 8 组改 `DtInfoEditFragment`、第 9 组改 `PtInfoEditFragment` 并一并删掉 `pt_homeFragment` 的死声明。SafeArgs 仍保留用于**类型安全地传参**（导航图的 `argument` 声明与 `Directions` 调用不改），但页面**读参**统一走 `AccountArgs`，不再各自 `navArgs` / `getString` / `intent`。宿主的 `Intent` extras（`DtHomeActivity:26`、`PtMainActivity:44`、`WaitingBindingActivity:32`、`PatientApplicationActivity:22`、`CreateRehabPlanActivity:35`、`VideoPlayerActivity:22-23`）是这条链的第一跳来源，本就该由宿主读取，不在收敛范围内，继续作为 `AccountArgs` 的兜底来源。`RehabPlanListFragment` 本轮才第一次有入口，必须从建 action 那一步就按统一链取参，不得复制旧的 `?: ""` 写法。
- **`dt_info_Fragment` 的 `totalCount` 参数退役**：从 `dt_bot_nav.xml` 移除该参数，`DtHomeActivity` 里只为数条数而建的适配器随之删除（连同 `PatientAdapter` 的构造调用）；医生我的页面自行观察患者查询。`dt_info_FragmentArgs` 由 SafeArgs 重新生成，`dt_homeFragment` 各 `navigate` 调用里的 `Bundle` 同步删掉该键。
- **合并同义数据控制器**：把 `DtHomeVM` 与 `DtHomeViewModel` 合成一个（保留被更多页面使用、且承载 `patients` 观察的那个名字），医生首页改用它；两个文件不再并存。
- **医生首页通知横幅**：现有实现（`getPendingTaskCount` + 点击进 `PatientApplicationActivity` 带 `doctorCode`）保留不动，只换取值与组件。
- **患者端同源数字的共享机制（必须共用宿主作用域同一 VM）**：这是「同一数字多处同源」能否成立的关键——康复页现在是 fragment 级 `viewModels()` 订阅 `observeRehabWeek`，**每次订阅都会重查一次周数据**；若患者首页再自己建一个 VM 各自订阅，就正好命中「两处各自计算」这条应拦场景，即使数值碰巧相等也不合格。做法：
  - 复用现有 `PtRehabViewModel`（它已承载周数据逻辑，`observeRehabWeek` 在 `PtRehabViewModel.kt:66` 单点调用），患者首页与康复页**都改用 `activityViewModels<PtRehabViewModel>()`** 取宿主 `PtMainActivity` 作用域下的同一实例；
  - `PtRehabViewModel` 内部的周数据流改为共享热流（`stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 初始值)` 或 `.asLiveData()`），使两个观察者共享同一次上游订阅，而不是各自触发一次查询；
  - `patientId` / `account` 由 `PtMainActivity` 经 `AccountArgs` 解析后一次性交给该 VM，两个 Fragment 不再各自解析；
  - 可核验判据：`observeRehabWeek` 的生产代码调用点在改造前后都只有 `PtRehabViewModel` 内一处（现状可由 `grep observeRehabWeek app/src/main` 验证）；两个页面同时活跃时，两处数值来自同一次发射。

### D7 死资源清理范围

- **模板 Compose 页（与 Compose 构建项同组下线）**：`app/src/main/java/com/example/winsrehab/MainActivity.kt`（Compose 页面：`setContent` + `WinsRehabTheme` + `Greeting` + `@Preview`）、`ui/theme/Color.kt`、`ui/theme/Type.kt`、`ui/theme/Theme.kt`，以及 `AndroidManifest.xml:15-19` 的 `MainActivity` 声明。删除依据：该页是新建项目的模板页，不是启动入口（启动入口是 `SelectLogActivity`），全项目无跳转引用，清单里自带注释「把 MainActivity 改成普通 Activity（不作为启动入口）」，而它存在的唯一前提就是 Compose 运行时——不删它就无法移除 Compose 依赖。删除后 `MainActivity` 在项目内不再存在。
- 未引用图片：约 30 个 drawable（以 Lint `UnusedResources` 输出为准逐条确认，不做批量猜删）。
- 死布局：`item_core_function.xml`、`item_today_task.xml`。
- 模板字符串：`hello_blank_fragment`、两条英文（`dotlog_Inspirational_words`、`dotlog_theme`）。
- **幽灵 Activity 声明**：`AndroidManifest.xml:42-45` 声明了 `.ui.main.doctor.info.DtInfoActivity`，但**该类和它的专属布局都不在仓库里**（`app/src/main/java` 下无 `DtInfoActivity*` 文件，也没有对应布局），属建库时的残留声明。清理动作是**只删这 4 行声明**，没有类或布局可删。该声明留着有实际风险：一旦被 `startActivity` 指向就是运行时 `ClassNotFoundException`。
- 坏样式：`MyRoundButton`（样式体写成裸文本）、`MyCheckBox`（`parent="Theme.AppCompat.Light"` 是主题家族不是样式）——两者的**定义**留到 10.5 使用点全部替换完再删，删除顺序写进任务依赖，避免中途 AAPT 找不到样式。
- 构建声明与依赖：见 D2 的收敛清单（`buildFeatures.compose`、`composeOptions`、8 条依赖声明、9 条库条目、4 个版本条目）。
- **不做**：`strings.xml` 里被页面引用的 303 处硬编码文案不整体搬移，只抽跨页面复用与需要状态映射的部分。
- **完成判据**：`grep -r "androidx.compose\|setContent\|@Composable\|WinsRehabTheme" app/src` 零命中；Manifest 的 activity 声明为 11 条、且引用 `Theme.AppCompat.*` 的为 0 条；`assembleDebug` 通过。

### D8 测试策略

**取值表守卫单测（`DesignTokenTableTest`）+ 对比度守卫**：该测试锁定 33 个语义色取值、6 档文本外观、间距/圆角/阴影档位、旧色名已删、界面资源无字面色值。其中 `任意文字必须落在达标底上` 一条是全站唯一的对比度闸门，覆盖范围必须是**任意文字**而不是只有白字：对每个布局元素的 `android:textColor` 与 `android:textColorHint`（经 style 链与 selector 逐状态解析）、以及 `app:tint="@color/on_primary"` 的白图标，按上表两档门槛核算。底取最近的不透明底（`background` / `backgroundTint` / `cardBackgroundColor`，drawable 里只认 `<solid>` 与 `<gradient>` 的各端且**显式排除 `<stroke>`**——否则「白底 + 主色描边」的次要按钮（`bg_button_secondary`）会拿描边色当底而误报）；半透明底按与 `surface` 混合后重算，视图 `alpha` 与颜色 alpha 同样按混合后判。违规文案形如 `layout/xxx.xml:274 <TextView> 14sp（门槛 4.5:1） 文字 @color/outline 落在 surface_variant 上仅 1.41:1`，逐条可定位。

**JVM 单测（随实现，与被测实现同组）**：状态徽标映射纯函数、占位与三态文案映射、`RehabFormatter` 既有断言保持全绿、`PlanProgress` 既有断言保持全绿、`TaskScheduleTest` 不动。

**界面自动化（Espresso，5 条关键路径，替代人工验收）**：

| # | 路径 | 断言要点 |
|---|---|---|
| 1 | 启动 → 医生登录 → 医生首页 | 登录成功进入首页；首页显示通知横幅与待办区；四宫格中 2 个可用入口可点击、2 个「即将开放」点击后界面不变 |
| 2 | 医生患者管理 → 创建计划 | 患者列表加载出真实患者；点击卡片进入创建计划页并显示该患者姓名 |
| 3 | 医生确认患者申请 | 首页最新申请出现待办；确认后该条消失、待处理计数减 1、患者管理列表新增该患者、两处患者总数一致 |
| 4 | 患者登录 → 康复中心完成/跳过 → 回患者首页 | 完成任务与跳过任务后状态正确；返回首页后今日完成数与本周完成率与康复中心一致；跳过不计入完成 |
| 5 | 底部导航三 tab 切换 + 计划列表可达 | 三个 tab 均可切换且选中态正确；医生首页「制定计划」可进入计划列表，列表能显示计划卡 |

**测试数据与环境**：沿用测试账号（患者 `test001/123456`、医生 `1/123456`）。必须先讲清 `TestDataHelper` 的现状能力与边界，否则验收路径不可复现：

- `initTaskGenerationTestData()` 建的是：患者 `test001`（`bindingStatus = "active"`、已绑定、`rehabStage = 恢复期`、`rehabStartDate` 为空）、一条 30 天计划、**3 个任务模板**（daily 每天 / weekly `"1,3,5"` / interval `"2"`）。
- 因此**当天生成的实例数只可能是 2～3 条，且随星期几波动**（生成动作发生前是 0 条）：daily 恒有 1 条；interval 在计划开始日（即今天）必命中 1 条；weekly 只在周一/三/五命中——周二/四/六/日是 2 条，周一/三/五是 3 条。**不存在「今日 4 条待完成」的现成数据**，凡把数字写死成 `2/4` 的断言都不可复现。
- 该 helper **不是幂等的**：重复调用会再插一条计划与三个模板，实例数翻倍。用例前必须以「一次调用」为前提，不可在 `@Before` 里无条件重复调用。
- 该 helper 建的患者是 `active`，**没有待处理绑定申请**，所以「确认患者申请」路径拿它当数据源必然失败。

据此逐路径给前置构造：

| 路径 | 前置数据 | 断言写法 |
|---|---|---|
| 1 启动→医生登录→首页 | `initTestDoctors()` 提供医生 `1`；无需患者 | 只断言页面元素与入口可点击性，不依赖具体计数 |
| 2 患者管理→创建计划 | 至少 1 名 `bindingStatus="active"` 且 `doctorCode="1"` 的患者 | 用该患者的真实姓名断言页面标题 |
| 3 确认患者申请 | **需额外造**：一名 `bindingStatus="pending"` 的患者 + 一条指向医生 `1` 的 `doctor_task`（`taskType="patient_binding"`、待处理）。现有 helper 不提供，用例内经 DAO/Repository 直接插这两条前置（造测试数据是本轮唯一允许绕过 Repository 写法的场景，必须在 `@After` 删除） | 「待办计数 = k → 确认后 = k-1」的相对断言，不假定固定初值 |
| 4 患者完成/跳过→首页一致 | **不用** `initTaskGenerationTestData()` 的自然命中，改为用例内直接插当天 4 条 `TaskInstance`，必须是**4 条不同任务模板**（`taskId` 各不相同、同一 `patientId`、`scheduledDate = 今天`、状态 `pending`）——`TaskInstance` 有 `(taskId, patientId, scheduledDate)` 唯一索引，同一 task 插 4 条只会落进 1 条 | 「完成 1 条 + 跳过 1 条 → 今日完成数 1/4（跳过不计入完成），剩余 2 条待完成」 |
| 5 底部导航 + 计划列表可达 | 复用路径 2 的患者与计划数据 | 列表能显示至少 1 张计划卡 |

- 断言一律针对可见文本与控件状态，不直接读数据库（路径 3、4 的前置造数除外）。
- 用例结束清理造出的实例、计划与待办，避免相互污染。**注意 `TestDataHelper.clearTestTaskInstances()` 现在是空操作**——方法体只打了一行日志、删除语句被注释掉了，且它跑在 `Dispatchers.IO` 的 fire-and-forget 协程里，不能当清理手段；清理必须在用例的 `@After` 里用 DAO 直接删（同样属于「造/清测试数据」的例外），并在删完后断言相关查询为空。

**既有 Instrumented 测试不能写成「零用例」**：`app/src/androidTest` 下已有 3 个文件——`ExampleInstrumentedTest`（包名断言，模板）、`RehabQueryTest`（113 行，经 Room in-memory 验证康复页本周查询字段与聚合）、`TaskExecutionRepositoryTest`（192 行，验证完成/跳过/状态守卫/回滚/幂等）。本轮新增的是**界面层** Espresso 用例，既有两个数据层用例保持不动、必须继续通过；`libs.androidx.ui.test.junit4` 随 Compose 下线移除后，这 3 个既有用例不依赖它，编译不受影响。

### D9 编码约定（对实现方的约束）

- 新增 Kotlin 文件写通俗单行 `//` 注释，禁多行注释块（AGENTS.md）。
- ViewModel 不直接访问 DAO；新增观察逻辑仍经 Repository。
- 关键节点（数据加载、空/错分支、导航跳转）加 `Log.i("ClassName", "描述: $value")`。
- 页面参数仍走 Bundle/arguments 与 Intent extras，不新造全局单例传参。

## Data Model

**本变更不涉及持久化。** 零 DDL、零字段、零索引、零迁移，数据库版本停留 v8，`fallbackToDestructiveMigration()` 不动。

只读复用到的既有表与列（仅引用，不修改）：

- `task_instance`：`instanceId`、`taskId`、`patientId`、`scheduledDate`、`status`、`completedAt` —— 用于今日完成数、本周完成率、任务列表。
- `training_task`：`taskId`、`taskName`、`suggestedDuration`、`priority` —— 用于任务卡展示字段。
- `rehab_plan`：`planId`、`patientId`、`status`、`progress` —— 用于计划列表与计划状态徽标。
- `patient`：`patientId`、`doctorCode`、`name`、`diagnosis`、`rehabStage`、`overallProgress`、`hasAlert` —— 用于患者卡与计数；**不接** `rehabDays`、`todayTasksCompleted`、`todayTasksTotal`、`weeklyCompletionRate`、`hasNewFeedback`、`lastActiveTime`（全项目无写入点）。
- `doctor_task`：`doctorCode`、`taskType`、`status`、`createdAt` —— 用于最新患者申请与待处理计数。

## Rule Boundary Attachment

**业务规则侧：不适用。** 本变更不新增、不修改任何业务规则、校验或拦截逻辑，也不引入或重排任何业务执行路径（状态流转、计划进度、应完成数、任务生成时间窗、计划起止日期校验全部沿用两份主规范）。因此不存在「判定挂在哪个业务动作」的问题——写入路径一行未改。

两条展示侧约束的挂载点：

1. **「即将开放」置灰** 挂在**视图层**（入口控件自身的可点击开关与视觉态）：不挂在数据层、不挂在状态机。为何不误触其它执行路径——置灰只作用于该入口控件本身，不改变任何查询与写入；同一能力若另有入口，互不影响；能力上线后只需打开该控件的可点击并接上真实数据，不动任何业务判定。
2. **同一数字多处同源** 挂在**数据来源层**（同一份查询 + 同一区间 + 同一算法）：患者首页与康复中心都消费 `TaskExecutionRepository.observeRehabWeek` 产出的同一份周数据（今日、周区间、周任务），自然周区间在来源处确定；医生端三处患者数都消费 `PatientDao.getPatientsByDoctorFlow`。为何不误触其它执行路径——来源层只读、无副作用，新增消费方自动继承同口径；本变更不新增任何写入，因此不存在「一次写入触发多处重算」的顺序问题。

## Risks / Trade-offs

- [18 页改造体量大，中间态会持续存在] → 按四组顺序推进，每组独立可编译可验证；取值表与主题先行，避免每组各自起名。
- [从 AppCompat 换到 Material 会改变所有控件默认外观与间距，可能破版] → 逐页改造后跑该页并截图；**人工截图走查留档**见下；编译与自动化测试之外，走查只作为发布前的一次性留档，不进任务列表。
- [删除旧颜色名与死资源后出现悬空引用] → 先改消费方再删定义；删除后必须编译通过；死资源删除以 Lint `UnusedResources` 输出逐条确认，不批量猜删。
- [同源数字因两页刷新时机不同出现短暂不一致，被误判为缺陷] → 以「同一时刻渲染值相同」为判定口径；自动化用例在断言前触发一次刷新（如 `onResume` 路径），避免时序 flaky。
- [置灰入口被做成删除，功能上线时要回填结构] → 一律保留入口控件与布局，只切换可点击与视觉态。
- [计划列表回接后，创建计划入口出现两条路径（患者卡直连、计划列表条目）] → 两条都是合法路径，都必须携带患者身份；列表页不提供「无患者新建」入口，因为创建计划页在缺患者身份时直接结束。
- [移除医生首页与患者管理页两处无功能搜索框，可能被认为功能回退] → 二者当前点击与输入均无任何效果；移除后界面不再出现「可输入但无响应」的假交互，筛选能力留给后续变更并在 proposal 的后续变更清单中登记。
- [测试依赖真实数据库，数据易相互污染] → 用例前置自建数据、后置清理；断言只看界面可见状态，不直读库；路径 3/4 的造数在用例内完成并在 `@After` 删除。
- [主题锁定浅色，系统处于深色模式的用户仍看到浅色界面] → 与现状完全一致（现状 12 处覆盖全是 Light 系）、不构成回归；深色支持作为后续独立变更登记在 Open Questions，届时只需换 parent 并补 `values-night`，页面与组件无需改动。
- [删除 `MainActivity` 与 Compose 依赖后，未来若要用 Compose 需重新引入] → 本轮明确选择单轨 XML，理由是双轨并存正是当前「依赖在却零使用」的隐患来源；Compose 引入属新增技术选型，应走独立变更并同时决定 XML/Compose 主题互通方案。
- [`PatientApplicationActivity` 因补顶部栏出现可见变化] → 该页现状靠唯一一处 `Theme.AppCompat.Light` 的 ActionBar 提供标题与返回，换主题后若不同步补控件就会退化为无标题无返回；补 `widget_top_bar` 是必要修复，视觉上多一条标题栏属改进而不是回归。

**人工截图走查留档（发布前一次性，不进任务列表）**：

1. 逐页截图：18 个界面在双端各自主题下的默认态、空态、错误态（有数据驱动者）。
2. 重点核对：顶部栏高度与标题层级、卡片圆角与阴影是否只有允许的档位、状态徽标在两处的文案与颜色是否一致、置灰入口是否「明显不可用且点击无反应」。
3. 同源数字核对：同一患者账号下，患者首页与康复中心的今日完成数、本周完成率各截一张，数值必须相同；同一医生账号下，医生首页、患者管理页、医生我的页面的患者数三张截图数值必须相同。
4. 留档位置：作为本变更归档说明的附件提交（仓库既有文档规则只维护 4 个根文档，不新增按日期命名的临时文档）。

## Migration Plan

1. **无数据库迁移**：v8 不变，安装包覆盖安装即可，不涉及数据兼容问题。
2. **资源重命名**：旧颜色名、旧样式名、死资源在同一组任务的末尾删除，删除前该组所有消费方已改造完并编译通过；任一组编译失败即回滚该组改动（资源名为纯编译期契约，回滚无数据风险）。
3. **回滚策略**：本变更不写数据、不改表，回滚等于回退构建；已安装用户的本地数据不受影响。
4. **发布前检查**：编译通过、现有单测全绿、5 条界面自动化测试通过、质量门通过、走查留档完成。

## Open Questions

- 患者管理页的按姓名/诊断筛选能力归属：本轮移除不可用搜索框，筛选本身作为后续独立变更（涉及列表过滤与可能的查询条件），需在开发计划里排期。
- 界面深色（夜间）模式的归属：本轮把主题锁定浅色以维持现状语义，深色支持需要 `values-night` 全套语义色与逐页对比度验证，作为后续独立变更登记，不在本变更内。
