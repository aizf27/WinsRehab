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
