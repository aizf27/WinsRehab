# 澄清记录：W1D2 查询与事务 + W1D3 患者康复页

## Open Questions

- Q1 进度分子口径：计划进度里的「已完成」该不该把「跳过」算进去？
- Q2 进度分母口径：「应完成数」按计划排期推算，还是按实际已生成的实例数？
- Q3 「跳过」是否也触发计划进度重算？
- Q4 每日任务生成是否补上计划起止时间窗？
- Q5 计划开始日期允许早于今天吗？早于今天会导致进度分母带上不可能完成的天数。
- Q6 「周期天数」换算结束日期是否 off-by-one？（现在 start + durationDays）
- Q7 今日任务的「完成 / 跳过」交互形态？
- Q8 多任务环形进度视图本轮做到什么程度？
- Q9 底部三张统计卡（累计训练 / 本周完成 / 连续打卡）本轮接不接真实数据？
- Q10 本周柱状图的周区间（周日边界）怎么处理？

## User Answers

- A1 认可：只有 `completed` 算完成，`skipped` 不算。连锁影响：今天任务全部跳过时进度显示 `0/N`。
- A2 按推荐：应完成数做成纯函数，等于计划内每个任务在 `[开始日, min(今天, 结束日)]` 内按频率规则应生成的天数之和，不看已生成实例。
- A3 按计划：`skip` 也触发重算（结果不变，保持两条路径对称幂等）。
- A4 按计划：生成补 `开始日 <= 日期 <= 结束日` 时间窗。
- A5 选 **B**：创建计划时禁止开始日期早于今天，从源头堵死。
- A6 默认纳入修正：结束日 = 开始日 + 周期天数 - 1，让「7 天计划」正好覆盖 7 个自然日（本项在方案摘要中复述，随摘要一并确认）。
- A7 认可：`pending` 任务点「开始」弹确认框（完成 / 跳过 / 取消）。
- A8 认可但降级：环形进度本轮**先占位**，不做分段自绘环；今日进度区保留横向进度条 + 真实「x/y 项已完成 / 百分比」。
- A9 认可：本轮只接「本周完成」，另两张留占位（「连续打卡」要 streak 逻辑，延后）。
- A10 认可：用 `java.time` 重算周区间并顺手修周日边界。

关联场景（Q11-Q15 的划界结论见下节），用户答复「现在提」即确认全部划界结论。

## Related-Surface Survey

- **任务生成入口 A（纳入）**：`ui/main/patient/login/PtLogVM.kt:59` 调用 `TaskInstanceRepository.generateDailyTasks`。改时间窗后须经同一函数，避免登录后生成口径不一致。
- **任务生成入口 B（纳入）**：`ui/main/patient/main/PtMainActivity.kt:88` 的 `onResume` 调用 `generateDailyTasks`。与入口 A 共用同一函数，统一适配。
- **共享处理逻辑（纳入）**：`data/repository/TaskInstanceRepository.kt:325` 的 `getWeekDateRange`（`Calendar` 实现）被 4 个周统计方法引用——`getWeekInstances`(L104)、`getWeekInstancesOnce`(L112)、`getWeekTaskCount`(L260)、`getWeekCompletedCount`(L268)；这 4 个方法当前均无调用方。本 change 把周区间抽为 `DateRanges` 纯函数供新康复页统计使用，并删除旧实现与 4 个死方法，避免两套周区间口径并存。
- **下游数据消费方（纳入·自动受益）**：`ui/main/doctor/adapter/RehabPlanAdapter.kt:60` 显示 `plan.progress`（「进度 X%」）。开始写入真实进度后自动显示真实值，无需额外改动。
- **现有方法无外部调用方（纳入·无回归风险）**：`completeInstance` / `skipInstance` / `getTodayInstances` / `getWeekInstances` 在 `app/src/main` 内除定义处外无调用方，可安全演进或替换为新的执行仓库。
- **仓库边界（排除）**：本 change 只覆盖当前仓库；无另一端前端/后端仓库，不适用跨仓拆分。
- **测试数据写入方（纳入）**：`utils/TestDataHelper.kt:163-216` 的 `initTaskGenerationTestData()` 直接写 `rehab_plan`/`training_task`（绕开 `savePlan`），种子计划 `startDate=2026-09-01`、`endDate=2026-09-30`；加时间窗后对今天出窗、生成 0 条任务。本 change 把种子起止日改为相对今天，保证 `test001` 当天能生成任务。
- **患者首页写死周统计（排除）**：`res/layout/fragment_pt_home.xml:518-549` 的「本周完成率 85%」「6/7」等，属首页统计卡语义，不在 patient-rehab 能力内，另提 change；验收时两页数值可能不一致属预期。
- **下游数据承载（排除）**：`data/entity/Patient.kt:28-33`：`rehabDays`/`weeklyCompletionRate`/`todayTasksCompleted`/`todayTasksTotal` 全仓无读写；`overallProgress` 仅有一个无调用方的患者自评写入（`PtInfoVM.saveRehabInfo`，语义是自评而非任务派生缓存）。判定：本轮不写这些字段（写了也没有读者），排除出本 change；若 Day 4 医生患者进度页需要，由那个 change 决定读取现算还是回写。

## Architecture Direction

### 方向

1. 新增 `TaskExecutionRepository`（构造依赖 `AppDatabase`，取其三个 DAO，事务用 `withTransaction`），承载完成/跳过与计划进度重算；`complete` / `skip` 在事务内：读实例 → 非 `pending` 早退 → 更新状态与操作时间戳 → 经 `taskId` 查任务得 `planId` → 重算并写回 `RehabPlan.progress`。
2. 进度的「应完成数」抽为纯函数（与 `TaskSchedule` 同风格、可 JVM 单测）：Σ 每个任务在 `[plan.startDate, min(today, plan.endDate)]` 内 `shouldGenerate` 的天数；分子为与分母同窗口内 `status = completed` 的实例数（窗口 `[plan.startDate, min(today, plan.endDate)]`，窗口外 completed 不计入）；`progress = completed / due * 100`，clamp 0..100，`due == 0` 时为 0。
3. 新增查询模型 `TaskInstanceWithTask`（`@Embedded TaskInstance` + 任务名/类型/建议时长/优先级/计划名），DAO 用 JOIN 查今日任务。
4. `generateDailyTasks` 增加计划时间窗判断（`planStartDate <= 目标日期 <= planEndDate`），不满足直接跳过该计划。
5. 计划创建：校验开始日期不早于今天；结束日期改为 `start + durationDays - 1`。
6. UI：`PtRehabViewModel` 暴露 `StateFlow<RehabUiState>`（Loading/Content/Error）与 `completeTask`/`skipTask` 动作；`TaskInstanceAdapter` + `item_task_instance.xml` 取代 4 个 `include`；环形进度区域本轮沿用现有图标占位（保留横向进度条 + 真实数值），不新增 `MultiTaskRingView` 之类自绘控件；周柱状图接真实数据（含今天标记），周区间用 `java.time` 重算。

### 备选与取舍

- 进度分母：应完成数（选）vs 已生成实例数（弃）。后者在患者漏登录几天后会因实例缺失导致分母缩小、进度突然跳变，与 7.3 冲突。
- 开始日期边界：创建校验 B（选）vs 纯计划窗口 A（弃）。B 从源头消除「过去天数被计入分母且无法完成」的演示隐患。
- 环形进度：完整分段自绘（本轮否）vs 占位（本轮选）。换时间先把真实数据闭环与周统计稳住。

### 方案摘要（七要素，待用户确认后定稿）

1. 一句话：让患者康复页显示数据库里今天的真实任务，支持完成/跳过，并把计划进度算准。
   - 今日训练列表改读真实数据，每条显示任务名称、建议时长、优先级，取代现有四条写死模拟任务。
   - 今日任务支持「完成 / 跳过」：点「开始」弹确认框，完成后标记已完成、跳过标记已跳过，列表即时刷新。
   - 今日进度卡片显示真实「已完成数 / 今日任务总数」，保留横向进度条；分段环形图本轮占位。
   - 每次完成或跳过，自动重算该任务所属康复计划的完成进度并落库。
   - 计划进度按「已完成数 / 截至今天的应完成数」计算；应完成数只由计划起止日期与任务频率规则推算，不看实际生成条数。
   - 今日任务生成补时间窗：只在计划开始日到结束日之间生成，未开始或已到期的计划不再生成。
   - 创建计划时开始日期不得早于今天；结束日期按「开始日 + 周期天数 - 1」，让「7 天计划」覆盖 7 个自然日。
   - 本周完成柱状图接真实数据；周区间按周一到周日用新日期方式重算，顺手修周日边界。
   - 底部三张统计卡本轮只接「本周完成」，另两张占位。
2. 契约：不涉及 API；不涉及数据库表结构变更（复用现有表），无 DDL 迁移；`rehab_plan.progress` 字段已存在。
3. 方案与取舍：选应完成数做分母（非已生成实例数），避免进度跳变；环形图占位换真实数据闭环。
4. 实施逻辑（人话）：
   1) 医生建计划时校验开始日不早于今天，结束日按周期天数 - 1 推算。
   2) 患者登录或回到康复页，系统按「计划时间窗 + 频率规则」为今天生成任务，重复生成被唯一约束挡掉。
   3) 康复页读取今天该患者的任务，连同任务名称、时长、优先级一起展示。
   4) 患者点「开始」→ 确认框「完成 / 跳过」→ 系统在事务里更新任务状态，找到所属计划，按「已完成 / 应完成」重算进度写回。
   5) 界面收到更新后刷新列表、进度条与本周柱状图。
   6) 周柱状图用重算后的周区间（周一到周日）统计每天完成/总数。
5. 关联场景判定：任务生成两入口（登录、onResume）本 change 内适配——纳入；周区间旧实现与 4 个无调用方方法删除、新查询统一走 DateRanges——纳入；医生计划列表进度自动受益——纳入（自动）；现有 complete/skip/查询方法与死包装无外部调用方，本轮删除——纳入（无回归）。
6. 影响能力：`task-execution`（新）、`patient-rehab`（新）。
7. 规则边界判定：
   - 规则 A（完成口径）：拦截——只有「已完成」计入分子；放行——跳过、待审核、已驳回均不计完成；等价路径——「完成」与「跳过」都走同一事务与重算，但只有完成改变分子。
   - 规则 B（生成时间窗）：拦截——日期早于开始日或晚于结束日不生成；放行——结束日当天仍生成；等价路径——登录与 onResume 两入口经同一函数，行为一致。
   - 规则 C（开始日期校验）：拦截——创建时开始日早于今天；放行——开始日等于今天或未来；等价路径——保存草稿与直接激活两条创建路径都须走同一校验，不能只堵一条。
   - 规则 D（进度重算挂载点）：挂在「任务完成 / 跳过」这一业务动作的事务内，不挂在底层单条状态更新上，避免其它流程（如 Week 2 的视频提交）误触发。

**用户确认**：2026-11-10 用户以文字「确认」明确确认上述七要素方案摘要，无修改。

## Authoring Tier

- Tier: COMPLEX
- Rationale: 涉及「数据/事务」与「患者康复页」两组任务且跨组依赖（UI 消费数据层），命中「2+ 任务组且跨组依赖」；按标准 COMPLEX 交由单个生成子代理一次性产出 proposal/specs/design/tasks。

## 执行边界

> 说明：第 3 轮（流程允许的最后一轮）未达 PASS，但剩余问题已按审查建议修复并经用户授权进入实施；以下边界取自第 3 轮架构审查输出，按实施顺序逐组生效。

```yaml
task_groups:
  "1":
    allowed_directories:
      - "app/src/main/java/com/example/winsrehab/utils"
      - "app/src/main/java/com/example/winsrehab/ui/main/doctor/plan"
      - "app/src/main/java/com/example/winsrehab/data/repository"
      - "app/src/test/java/com/example/winsrehab/utils"
    allowed_packages:
      - "app/src/main/java/com/example/winsrehab/utils"
      - "app/src/main/java/com/example/winsrehab/ui/main/doctor/plan"
      - "app/src/main/java/com/example/winsrehab/data/repository"
      - "app/src/test/java/com/example/winsrehab/utils"
    notes: "PlanDates 新增、CreateRehabPlanViewModel.savePlan 校验与结束日、CreateRehabPlanActivity minDate、generateDailyTasks 计划级窗口、TestDataHelper 种子日期相对今天；不要改 TaskSchedule。"
  "2":
    allowed_directories:
      - "app/src/main/java/com/example/winsrehab/utils"
      - "app/src/main/java/com/example/winsrehab/data/dao"
      - "app/src/test/java/com/example/winsrehab/utils"
    allowed_packages:
      - "app/src/main/java/com/example/winsrehab/utils"
      - "app/src/main/java/com/example/winsrehab/data/dao"
      - "app/src/test/java/com/example/winsrehab/utils"
    notes: "PlanProgress 纯函数 + TaskInstanceDao 分子查询（带计划窗口）；不建表不加列。"
  "3":
    allowed_directories:
      - "app/src/main/java/com/example/winsrehab/data/repository"
      - "app/src/main/java/com/example/winsrehab/data/dao"
      - "app/src/androidTest/java/com/example/winsrehab"
    allowed_packages:
      - "app/src/main/java/com/example/winsrehab/data/repository"
      - "app/src/main/java/com/example/winsrehab/data/dao"
      - "app/src/androidTest/java/com/example/winsrehab"
    notes: "TaskExecutionRepository 事务与进度回写、删死方法与 RehabPlanRepository.updateProgress、androidTest 内存库用例。"
  "4":
    allowed_directories:
      - "app/src/main/java/com/example/winsrehab/utils"
      - "app/src/main/java/com/example/winsrehab/data/repository"
      - "app/src/main/java/com/example/winsrehab/data/dao"
      - "app/src/test/java/com/example/winsrehab/utils"
    allowed_packages:
      - "app/src/main/java/com/example/winsrehab/utils"
      - "app/src/main/java/com/example/winsrehab/data/repository"
      - "app/src/main/java/com/example/winsrehab/data/dao"
      - "app/src/test/java/com/example/winsrehab/utils"
    notes: "DateRanges 新增；删除 5 个周统计方法与私有日期助手；不改 TaskGenerationHelper。"
  "5":
    allowed_directories:
      - "app/src/main/java/com/example/winsrehab/data/model"
      - "app/src/main/java/com/example/winsrehab/data/dao"
      - "app/src/main/java/com/example/winsrehab/data/repository"
      - "app/src/main/java/com/example/winsrehab/ui/main/patient/main"
      - "app/src/main/java/com/example/winsrehab/ui/main/patient/adapter"
      - "app/src/main/res/layout"
      - "app/src/test/java/com/example/winsrehab/ui/main/patient"
      - "app/src/androidTest/java/com/example/winsrehab"
    allowed_packages:
      - "app/src/main/java/com/example/winsrehab/data/model"
      - "app/src/main/java/com/example/winsrehab/data/dao"
      - "app/src/main/java/com/example/winsrehab/data/repository"
      - "app/src/main/java/com/example/winsrehab/ui/main/patient/main"
      - "app/src/main/java/com/example/winsrehab/ui/main/patient/adapter"
      - "app/src/test/java/com/example/winsrehab/ui/main/patient"
      - "app/src/androidTest/java/com/example/winsrehab"
    notes: "today/weekRange 每次订阅重算；patientId 走 args→intent 兜底；状态→视觉只归 TaskStatusStyle；新布局仅 item_task_instance.xml。"
  "6":
    allowed_directories:
      - "app/src/main/java/com/example/winsrehab/ui/main/patient/main"
      - "app/src/main/res/layout"
      - "app/src/test/java/com/example/winsrehab/ui/main/patient"
    allowed_packages:
      - "app/src/main/java/com/example/winsrehab/ui/main/patient/main"
      - "app/src/test/java/com/example/winsrehab/ui/main/patient"
    notes: "fragment_pt_rehab.xml + pt_rehabFragment + RehabFormatter；删 item_exercise_task.xml；不改 item_week_bar.xml/item_stat_card.xml。"
```

## Initial Capabilities

- `task-execution`: 任务完成/跳过状态流转、计划进度重算、每日生成时间窗、计划起止日期规则。
- `patient-rehab`: 患者康复页今日任务列表、完成/跳过交互、进度显示（占位环）、本周柱状图。
