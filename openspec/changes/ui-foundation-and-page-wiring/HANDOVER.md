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

## 下一步：任务组 2（风险最高的一组）

「双端主题与依赖收敛」。**必须同组完成**，否则中间任一提交都会编译失败：

- `Theme.WinsRehab` 的 parent 由 `DayNight` 改 `Theme.MaterialComponents.Light.NoActionBar`（锁浅色）
- 删 `MainActivity.kt` + `ui/theme/{Color,Type,Theme}.kt` + `AndroidManifest.xml:15-19` 声明
- 删 Compose 构建项：`compose = true`、整个 `composeOptions` 块、8 条依赖、9 条库条目、4 个版本条目
- 版本目录加 `material = "1.12.0"` + `libs.material`
- 删 `AndroidManifest.xml:42-45` 的 `DtInfoActivity` 幽灵声明（组 1 已申请豁免、排期在此）
- 11 个在用 Activity 改挂新主题，去掉 `Theme.AppCompat.*` 覆盖
- 修 `MyRoundButton` 坏样式体（只删 `Theme.WinsRehab` 的 `buttonStyle` 默认指向；样式本体留到 10.5 再删，因仍有 5 处按钮 + 2 处 `android:theme` 引用）

## 已知坑（后续每组都会遇到）

1. **假绿**：`:app:testDebugUnitTest` 不把 `res` 当任务输入，只改资源会 UP-TO-DATE 直接打成功而不跑断言。改资源后必须 `--rerun-tasks`。
2. **lint 4 条基线 error**：`AndroidManifest.xml:42` MissingClass（组 2 删）、`PtInfoVM.kt:39/43/46` NullSafeMutableLiveData（组 9 处理）。中间各组按「与基线对照、不新增」判定，不算回归。
3. **主仓库 openspec 有未提交变更**（`w1d2d3` 归档残留、`openspec/specs/` 等），非本次变更产生，收口合并前需人工处置。
4. **同一 worktree 不可并行跑两个子代理**：曾出现一个复核者的探针改动被另一个误认为脏改动。

## 剩余任务组

组 3 基础控件 → 组 4 状态与结构组件 → 组 5 入口与身份页 → 组 6 医生首页与计划列表入口 → 组 7 患者管理页 → 组 8 医生其余页 → 组 9 患者端与同源数字 → 组 10 独立功能页与死资源收敛。任务清单见同目录 `tasks.md`（67 条），执行边界见 `clarify.md`。
