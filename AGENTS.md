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
2. 医生创建康复计划 → 添加训练任务（设置频率）
3. 系统根据频率规则生成每日任务实例
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

### 已完成
✅ 用户系统（登录、绑定、信息管理）  
✅ 康复计划数据层（4个Entity + DAO + Repository）  
✅ 医生端创建康复计划UI  
✅ 数据库版本7（9张表）

### 待实现
🚧 任务生成逻辑（根据TrainingTask频率规则生成TaskInstance）  
🚧 患者端任务列表与完成功能  
🚧 视频录制上传  
🚧 医生端视频审核  
🚧 患者反馈与计划调整

---

## 常见问题

### 1. ViewModel中获取数据库
❌ 错误：`AppDatabase.getDatabase()`  
✅ 正确：`MyApp.instance.database`

### 2. Repository方法缺失
如遇到Repository方法不存在，需同步更新DAO和Repository两层。

### 3. 数据库迁移
当前开发阶段使用`fallbackToDestructiveMigration()`，发布前需编写Migration脚本。

### 4. 冗余字段更新
Patient表中的`todayTasksCompleted`、`weeklyCompletionRate`等统计字段需要在相关操作后手动更新。

---

## 重要文档

- **架构总览**：`docs/项目架构总览.md`
- **开发进度**：`docs/开发进度跟踪.md`
- **开发计划**：`docs/开发计划与优先级.md`
- **项目规划**：`docs/项目规划总览.md`

---

## 启动入口

`SelectLogActivity` → 医生/患者登录 → 各自主界面

- 医生主界面：`DtHomeActivity`（3个Fragment：首页、患者管理、个人信息）
- 患者主界面：`PtMainActivity`（3个Fragment：首页、康复训练、个人信息）

---

**最后更新**：2026-09-07
