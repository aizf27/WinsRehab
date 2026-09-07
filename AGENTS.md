# WinsRehab 项目记忆

**用于codex和claude code的项目记忆文档**

---

## 项目核心

WinsRehab是**医患双端康复管理系统**，实现康复计划制定、任务分配、进度追踪和视频指导的完整闭环。

**技术栈**：Kotlin + MVVM + Room（版本7，9张表）

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
Entity（9张表）
```

### 核心数据模型
- **用户**：Doctor、Patient（bindingStatus: unbound→pending→active）
- **康复计划**：RehabPlan（draft→active→completed）
- **任务系统**：TrainingTask（模板，频率规则）→ TaskInstance（实例，患者执行）
- **调整追踪**：PlanAdjustment（记录医生调整历史）

### 关键业务流程
1. 患者登录 → 输入医生工号绑定 → 医生确认
2. 医生创建康复计划 → 添加训练任务（设置频率：daily/weekly/interval）
3. **系统自动生成任务**：患者登录时根据频率规则生成每日TaskInstance
4. 患者完成训练上传视频 → 医生审核反馈
5. 医生根据反馈调整计划（保留调整记录）

---

## 代码规范

### 命名约定
- Activity：`XxxActivity.kt`
- Fragment：`xxx_fragment.kt`（小写下划线，历史遗留）
- ViewModel：`XxxVM.kt` 或 `XxxViewModel.kt`
- Adapter：`XxxAdapter.kt`
- 布局：`activity_xxx.xml`、`fragment_xxx.xml`、`item_xxx.xml`

### 数据库操作
- Flow用于响应式查询（自动更新UI）
- suspend用于一次性操作（如插入、删除）
- 获取数据库实例：`MyApp.instance.database`

### 导航
- 医生端/患者端使用Navigation Component + BottomNavigationView
- Fragment间传参通过Bundle + arguments
- Activity间传参通过Intent extras

---

## 当前状态（2026-09-07）

### 已完成（45%）
✅ 用户系统（登录、绑定、信息管理）  
✅ 康复计划数据层（4个Entity + DAO + Repository）  
✅ 医生端创建康复计划UI  
✅ **任务自动生成逻辑**（第1阶段完成）
  - TaskGenerationHelper工具类
  - generateDailyTasks()方法
  - 三种频率支持：daily/weekly/interval
  - 登录触发 + onResume刷新
  - 智能去重 + 批量插入优化
✅ 数据库版本7（9张表）

### 待实现（55%）
🚧 患者端任务列表UI（Day 3）- **下一步**  
🚧 视频录制上传（Day 4-6）  
🚧 医生端视频审核（Day 7-9）  
🚧 患者反馈与计划调整（MVP后）

---

## 任务生成机制（重要）

### 频率规则
- **daily**：每天生成
- **weekly**：特定星期几生成（frequencyDetail存储"1,3,5"表示周一三五）
- **interval**：间隔N天生成（frequencyDetail存储"2"表示间隔2天）

### 生成流程
```kotlin
患者登录（PtLogVM.loginPatient）
  ↓
TaskInstanceRepository.generateDailyTasks(patientId, today)
  ↓
1. 获取患者所有活跃计划（status='active'）
2. 遍历计划的任务模板（TrainingTask）
3. 判断频率规则（TaskGenerationHelper.shouldGenerateToday）
4. 检查去重（isInstanceExists）
5. 批量插入TaskInstance（IGNORE策略）
```

### 触发点
- 患者登录成功后（PtLogVM）
- 应用恢复时（PtMainActivity.onResume）

### 去重保护
- 数据库级别：OnConflictStrategy.IGNORE
- 业务级别：isInstanceExists查询

---

## 常见问题

### 1. ViewModel中获取数据库
❌ 错误：`AppDatabase.getDatabase()`  
✅ 正确：`MyApp.instance.database`

### 2. Repository构造函数
TaskInstanceRepository需要3个DAO：
```kotlin
TaskInstanceRepository(
    taskInstanceDao,
    rehabPlanDao,
    trainingTaskDao
)
```

### 3. 数据库迁移
当前开发阶段使用`fallbackToDestructiveMigration()`，发布前需编写Migration脚本。

### 4. 冗余字段更新
Patient表中的`todayTasksCompleted`、`weeklyCompletionRate`等统计字段需要在相关操作后手动更新。

---

## 重要文档

- **项目总览**：`docs/项目总览.md`
- **开发进度**：`docs/开发进度跟踪.md`
- **开发计划**：`docs/开发计划.md`
- **第1阶段实现方案**：`docs/第1阶段-任务生成逻辑实现方案.md`
- **第1阶段测试报告**：`docs/第1阶段-测试验证报告.md`

---

## 启动入口

`SelectLogActivity` → 医生/患者登录 → 各自主界面

- 医生主界面：`DtHomeActivity`（3个Fragment：首页、患者管理、个人信息）
- 患者主界面：`PtMainActivity`（3个Fragment：首页、康复训练、个人信息）

---

## 测试账号

**测试患者**：test001 / 123456  
**测试医生**：1 / 123456

使用`TestDataHelper.initTaskGenerationTestData()`可初始化完整测试数据。

---

**最后更新**：2026-09-07
