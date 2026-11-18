# 交接说明（预算中断时的状态快照）

**日期**：2026-11-11
**分支**：`change/ui-foundation-and-page-wiring`（worktree：`.worktree/ui-foundation-and-page-wiring/ws`）
**基线**：`dev_week1` @ `15931ca`
**变更**：`ui-foundation-and-page-wiring`（规划五工件均已 done，`validate --strict` 通过）

## 已完成

任务组 1「界面取值表」全部交付，7 次提交：

| commit | 内容 |
|---|---|
| `f31e023` | 语义色/间距/圆角/阴影/文本外观全部令牌化 |
| `dd85999` | 补浅色容器底档，修彩色底灰化与白字对比 |
| `cd58ba4` | 白字落底全量达标，字号补齐并加守卫测试 |
| `099870d` | 非白字对比度达标，守卫扩到任意文字 |
| `8139bdc` | design.md 细化容器底禁用文字清单 |
| `96b8203` | 导航标签对比度 + 守卫覆盖 `app:` 命名空间 |
| `051c63a` | design.md 登记 4 个预留令牌与消费任务号 |

交付物：`colors.xml` 33 个颜色定义（26 语义 + 7 容器底）、`dimens.xml`（间距 7 档 / 圆角 3 档 / 阴影 2 档）、`styles.xml` 6 档文本外观、36 个布局 + 93 个 drawable + 4 个 selector 全量去字面值、`DesignTokenTableTest` 守卫（64 用例全绿）。

## 未收口的一件事

任务组 1 的 `quality_review` 第 5 轮复核未能提交：两次派出的复核子代理（`06f9128b`、`3adee27f`）都在运行中异常终止、无关闭消息，4 条 issue 停在「待复核」。代码与文档本身已提交干净，缺的只是这一次复核裁定。

恢复方式（二选一）：
1. 重新派一名 `openspec-reviewer` 复核这 4 条（导航标签对比度、`app:` 命名空间守卫、预留令牌登记、`fragment_dt_home.xml` 注释死布局），通过后 `implement` 与 `quality_review` 双双 passed，即可推进任务组 2。
2. 人工确认这 4 条已修（见上表 `96b8203`），直接以豁免/裁定方式推进。

## 任务组 2 已完成（双端主题与依赖收敛）

- `Theme.WinsRehab` parent 改 `Theme.MaterialComponents.Light.NoActionBar`（锁浅色）；新增 `Theme.WinsRehab.Doctor`（蓝）与 `Theme.WinsRehab.Patient`（绿）两个 overlay，只换品牌主色。
- 过渡主题 `Theme.WinsRehab.Doctor.WithBar`（parent `Theme.MaterialComponents.Light`）：`PatientApplicationActivity` 目前仍靠系统标题栏提供标题+返回，第 5 组补 `widget_top_bar` 后改挂 `Theme.WinsRehab.Doctor` 并删掉这个过渡主题。
- 删 `MainActivity.kt` + `ui/theme/{Color,Type,Theme}.kt` + Manifest 里那条声明；删 Compose 构建项（`compose = true`、`composeOptions` 块、8 条依赖、9 条库条目、4 个版本条目）；版本目录加 `material = "1.12.0"` 与 `implementation(libs.material)`。
- Manifest：删 `DtInfoActivity` 幽灵声明；activity 声明 11 条，`Theme.AppCompat.*` 计数 0。
- 坏样式：只删了 `Theme.WinsRehab` 的 `buttonStyle` 默认指向；`MyRoundButton` / `MyCheckBox` 本体仍留着（还有 5 处按钮 + 2 处 `android:theme` 引用），等第 5、10 组替换完再删。
- 守卫测试 `app/src/test/java/com/example/winsrehab/ui/design/ThemeAndDependencyTest.kt`（5 条：基础主题锁浅色、双端主色不同且取自品牌色、界面主题只指向双端主题且声明 11 条、Compose 零痕迹、material 固定版本）。

## 已知坑（后续每组都会遇到）

1. **假绿**：`:app:testDebugUnitTest` 不把 `res` 当任务输入，只改资源会 UP-TO-DATE 直接打成功而不跑断言。改资源后必须 `--rerun-tasks`。
2. **lint 4 条基线 error**：`AndroidManifest.xml:42` MissingClass（组 2 删）、`PtInfoVM.kt:39/43/46` NullSafeMutableLiveData（组 9 处理）。中间各组按「与基线对照、不新增」判定，不算回归。
3. **主仓库 openspec 有未提交变更**（`w1d2d3` 归档残留、`openspec/specs/` 等），非本次变更产生，收口合并前需人工处置。
4. **同一 worktree 不可并行跑两个子代理**：曾出现一个复核者的探针改动被另一个误认为脏改动。

## 剩余任务组

组 3 基础控件 → 组 4 状态与结构组件 → 组 5 入口与身份页 → 组 6 医生首页与计划列表入口 → 组 7 患者管理页 → 组 8 医生其余页 → 组 9 患者端与同源数字 → 组 10 独立功能页与死资源收敛。

## 更新（2026-11-16）：组 1 已收口，流程已降档

- **任务组 1 正式完成**：4 条待复核项全部通过（`677a3e63` 复核：导航标签 5.02:1、`app:` 命名空间守卫、预留令牌登记、首页注释死布局），`opx_orch_complete_task_group` 已记账。累计 17 条已验证 + 1 条豁免 + 8 次提交；64 用例全绿；lint 4 errors 全为基线存量。
- **openspec 编排流程已停用**：后续组不再走 implement / quality_review / 必做项申报 / 子代理审查。`tasks.md` 顶部新增的「0. 执行规则」是唯一流程依据（三条红线、10 组执行边界、L0–L3 自检、提交约定）。
- 规划文档定位：`tasks.md` = 唯一清单，`design.md` = 唯一技术手册（只查 D 段），`clarify.md` / `proposal.md` / `.review/issues.md` / 三份 `specs/` 全部冻结不再依据。
- 上面「未收口的一件事」一节已作废，仅作历史留痕。

## 更新（2026-11-16）：组 2 收口与主仓库合并

- **任务组 2 完成**，提交 `4708ec9`（锁浅色 Material 基础主题 + 医生端/患者端两个 overlay、删 Compose 构建项与模板页、删 `DtInfoActivity` 幽灵声明、material 显式 1.12.0、`ThemeAndDependencyTest` 5 条守卫）。L0/L1/L2 全过：activity 声明 11 条、`Theme.AppCompat.*` 计数 0、Compose 零残留、64 用例全绿。
- **主仓库已合并组 1 + 组 2**：`dev_week1` 现在包含全部已交付代码，`D:\Android\MyProject\WinsRehab` 可直接用 Android Studio 运行到最新进度（`--no-ff` 合并，见 tasks.md 0.6）。
- **组 2 视觉上几乎无变化是预期**：它只换主题地基与依赖。肉眼判据只有「状态栏白底深色图标」和「未显式上色控件的 `?attr/colorPrimary` 色相」。真正的视觉变化从组 3/4 开始。
- **真机「没变化」最常见的两个原因**：① 跑的是主仓库旧代码（本次已合并修掉）；② 装的是 Android Studio 旧输出，而不是 `app/build/outputs/apk/debug/app-debug.apk`。判断方法：看状态栏是否为白底深色图标。
- **模拟器全屏黑屏（非代码问题）**：2026-11-16 出现过一次——窗口 `mDrawState=HAS_DRAWN` 但 `Surface: shown=false`、`mShownAlpha=0.0`，即 WindowManager 把窗口显示透明度按在 0，布局/点击/日志全部正常却整屏黑。`adb reboot` 即恢复；用快照恢复的模拟器容易复发，建议冷启动（`-no-snapshot-load`）。

## 下一步：组 3 基础控件组件

新会话承接顺序：`tasks.md` 顶部 0 节 → 本文件 → `design.md` D3（基础控件）与 D4（状态徽标）。

组 3 交付：主/次/危险/文字四类按钮（含尺寸与禁用态）、统一输入框（标签/必填/错误/辅助文本）、卡片的三种形态、状态徽标（任务五态 + 计划四态，以 `TaskStatusStyle` 为基座整体迁入 `ui/common`，不新建第二套映射，同步改 `TaskInstanceAdapter` 三处调用点与测试路径）。

## 更新（2026-11-16）：组 3 收口（基础控件组件）

**样式层（`styles.xml` 160 行，共 20 个组件样式）**
- 按钮：`Widget.WinsRehab.Button`（Title16 / 48dp / radius_16）+ `.Primary/.Secondary/.Danger/.Text` + 四个 `.Small`（Body12 / 36dp / radius_8）。
- 输入框：`Widget.WinsRehab.TextInputLayout`（OutlinedBox，四角 radius_8、boxStrokeColor 走 `@color/input_box_stroke`、error/helper 双开）。
- 卡片：`Widget.WinsRehab.Card`（surface / radius_12 / elevation_1）+ `.Clickable`（clickable+focusable+`?attr/selectableItemBackground`）+ `.StatusBar`（elevation_0 + clipToOutline）。
- 徽标：`Widget.WinsRehab.StatusBadge`（`parent=""`，必须显式切断隐式继承，否则 AAPT 报 `resource style/Widget.WinsRehab not found`）。

**两个 AAPT 硬坑（后续写样式必踩）**
1. style 内 `<item name>` **不能带 `app:` 前缀**，写成裸名（`cornerRadius` / `cardBackgroundColor` / `boxStrokeColor`…）；框架属性才带 `android:`。
2. 点号命名的样式若不想继承同名父样式，必须写 `parent=""`。

**颜色与选择器（新增 9 个 `res/color/*.xml`）**：`btn_primary_text` / `btn_secondary_bg` / `btn_secondary_text` / `btn_secondary_stroke` / `btn_danger_bg` / `btn_danger_text` / `btn_text_fg` / `input_box_stroke`，全部带 `state_enabled=false → disabled_bg|disabled_fg`；本轮把 `disabled_bg`/`disabled_fg`/`status_review`/`status_skipped_rejected` 四个预留令牌全部消费掉。

**主按钮颜色跟主题走（用户 2026-11-16 拍板）**：`Widget.WinsRehab.Button.Primary` 的 `backgroundTint` 写 `?attr/winsButtonPrimaryTint`，由新增 `res/values/attrs.xml` 声明、三个主题分别赋值 —— 基础主题与 `Theme.WinsRehab.Doctor.WithBar` → `@color/btn_primary_bg_doctor`（品牌蓝），`Theme.WinsRehab.Patient` → `@color/btn_primary_bg_patient`（深绿 #15803D）。理由：患者端主题 `colorPrimary` 本就是深绿，写死 `primary` 蓝会让患者页 CTA 与绿色主题打架（`design.md:95` 也要求患者侧按钮用深绿）。代价是守卫测试 `DesignTokenTableTest` 做了**第 4 处**小改：新增 `themeAttrColors()`，把 `?attr/xxx` 展开成「所有主题里该属性的取值并集」，逐个底色都过对比度（蓝 5.17:1、深绿 5.02:1 均达标）。

**状态映射（唯一归属 `ui/common`）**
- `ui/common/TaskStatusStyle.kt`：`TaskVisual`（6 值）+ `PlanVisual`（5 值）、`visualOf`/`planVisualOf`/`isActionable`、`labelOf`/`planLabelOf`、`colorOf`/`planColorOf`（语义条色）、`containerColorOf`/`planContainerColorOf`（徽标底）、`labelColorOf`/`planLabelColorOf`（徽标字色，全部落 D1 允许的 5 色白名单：`on_surface` / `on_surface_secondary` / `warning` / `brand_doctor_primary_dark` / `brand_patient_primary_deep`）。
- `UNKNOWN` 兜底：文案「未知状态」、语义条色 `danger`、`Log.e` 记原值；徽标文字改用 `on_surface`（`danger` 不能压容器底当小字）。
- `ui/common/StatusBadge.kt`：`bindTask` / `bindPlan` 两入口。
- 旧 `ui/main/patient/main/TaskStatusStyle.kt` 与其测试**同组删除**（删除与使用点同提交），`TaskInstanceAdapter` 三处调用点改 import 到 `ui/common`，无转发壳。

**输入框组件**：`layout/widget_text_field.xml`（根 `<merge>`：标签行 + 红星 + `TextInputLayout` + `TextInputEditText`）+ `ui/common/TextField.kt`（`setLabel/setRequired/setHelperText/setError/editText()/text()`）。本组只建组件、由契约测试兜住，页面接线在第 9/14/15 组。

**页面验证（3.6「可见」用真机截图）**：只改患者任务卡 `item_task_instance.xml` + `TaskInstanceAdapter`——`CardView` 换 `MaterialCardView(Card.StatusBar)`、顶部 `statusStrip` 语义色条、右挂 `<include layout="@layout/widget_status_badge"/>`、「开始」由 TextView 冒充改为 `MaterialButton(Button.Primary.Small)`。模拟器（AVD `Pixel_4_XL`）实测五态全部渲染正确、色条上两角随卡片圆角（`bg_status_strip.xml` 的 `topLeft/topRightRadius=@dimen/radius_12`）。

**自检结果**
- L0 `:app:assembleDebug` 通过。
- L1：旧包 `ui.main.patient.main.TaskStatusStyle` 零引用；`TaskStatusStyle|TaskVisual|PlanVisual` 只出现在 `ui/common/{TaskStatusStyle,StatusBadge}.kt`、`TaskInstanceAdapter.kt`、`TaskStatusStyleTest.kt`；两个旧文件已不存在；`item_task_instance.xml` 无 `bg_btn_green_small` / `imgStatus`（全库仅剩 `fragment_pt_home.xml:257` 一处旧按钮，组外保留）。
- L2 `:app:testDebugUnitTest --rerun-tasks` 82 用例 0 失败（新增 `ui/common/TaskStatusStyleTest` 9 条、`ui/common/ComponentContractTest` 7 条；`DesignTokenTableTest` 仍 15 条、`ThemeAndDependencyTest` 5 条）。
- Lint：与基线对照不新增（`PtInfoVM.kt:39/43/46` 三条 NullSafeMutableLiveData 留组 9）。

**模拟器验证小抄（下次要截真机图直接用）**
- 启动：`& "D:\My AS\Android Studio SDK\emulator\emulator.exe" -avd Pixel_4_XL -no-snapshot-load -gpu swiftshader_indirect`；截图只能 `adb shell screencap -p /sdcard/s.png` + `adb pull`（`adb exec-out screencap -p > file` 在 PowerShell 下产物损坏）。
- 导航：`SelectLogActivity` → 点头像 `imagePtLog`（点文字无效）→ 患者登录页 → 登录后首页 → 底部第二个 tab「康复中心」。
- 造数据：设备上 `sqlite3` 可用，`adb push seed.sql /data/local/tmp/` 后 `adb shell "cat /data/local/tmp/seed.sql | run-as com.example.winsrehab sqlite3 databases/rehab.db"`。**坑**：`patient.patientId` 必须等于 `account`（`PtLogVM` 用 `isExist(account)` 判存在，patientId 对不上会直接提示「账号不存在」），`task_instance.patientId` 同理存 account。
