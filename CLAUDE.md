# CLAUDE.md - WinsRehab项目指导

本文档为WinsRehab项目提供Claude Code专用指导。

---

## 项目背景

WinsRehab是**医患双端康复管理系统**，使用Kotlin + MVVM + Room架构。

**核心业务**：医生制定康复计划 → 系统生成任务 → 患者执行上传视频 → 医生审核反馈 → 动态调整计划

---

## 代码规范

### 1. 注释要求
- 所有Kotlin文件必须有通俗易懂的单行注释 `//`
- 禁止长篇大论的多行注释块
- 注释说明"是什么"和"为什么"，不重复代码逻辑

### 2. Commit规范
- 每实现一个小功能点就commit
- commit message用中文，清晰描述做了什么
- 示例：`"创建康复计划页面UI和ViewModel"`

### 3. Log规范
- 关键节点加`Log.i`或`Log.e`
- 格式：`Log.i("ClassName", "description: $value")`
- 用途：网络请求、成功/失败分支、导航跳转

### 4. 架构约定
- 严格遵循MVVM：Entity → DAO → Repository → ViewModel → UI
- 获取数据库实例：`MyApp.instance.database`
- Flow用于响应式查询，suspend用于一次性操作
- ViewModel中不直接访问DAO，必须通过Repository

---

## 开发流程

### 新增功能模块
1. **先读代码**：改任何文件前必须Read，不靠记忆
2. **数据层优先**：Entity → DAO → Repository → 测试
3. **UI层跟进**：ViewModel → Activity/Fragment → 布局
4. **及时commit**：每完成一层就commit

### 涉及3个以上文件
- 先`EnterPlanMode`写计划，经用户确认后再动手
- 单文件小改、bug修复直接干

### 不确定的操作
- 删除文件、改API签名、修改已有业务逻辑 → **先问用户**
- 不自己硬拍板

---

## 技术决策

### 数据库
- 当前开发阶段：`fallbackToDestructiveMigration()`
- 发布前需编写Migration脚本

### 任务生成
- TrainingTask是模板（包含频率规则）
- TaskInstance是实例（患者实际操作）
- 频率类型：daily、weekly（1,3,5）、interval（间隔N天）

### 视频处理
- 初期本地存储（`/storage/emulated/0/WinsRehab/videos/`）
- 后期扩展云存储

### 通知系统
- 使用本地通知NotificationManager
- 不做远程推送（简化复杂度）

---

## 常见陷阱

### 1. ViewModel数据库访问
❌ `AppDatabase.getDatabase(context)`  
✅ `MyApp.instance.database`

### 2. Fragment命名不一致
- 历史遗留：小写下划线（如`pt_homeFragment.kt`）
- 新文件可统一为驼峰（如`RehabPlanListFragment.kt`）

### 3. Repository方法缺失
- 需要同时更新DAO和Repository
- 示例：添加`getPatientByIdOnce`需在DAO和Repository都加

### 4. 冗余字段未更新
- Patient表的统计字段（`todayTasksCompleted`等）需手动更新
- 相关操作后记得调用更新方法

---

## 文档索引

| 文档 | 用途 |
|------|------|
| `docs/项目架构总览.md` | 快速了解项目结构 |
| `docs/开发进度跟踪.md` | 当前进度和待办任务 |
| `docs/开发计划与优先级.md` | 完整开发路线图 |
| `docs/项目规划总览.md` | 业务需求和功能设计 |
| `AGENTS.md` | 项目记忆（codex/claude code） |

---

## 配合用户习惯

- 所有对话用中文
- 禁止客套、寒暄、夸赞，只给干货
- 解释只讲关键点，不啰嗦
- 遇到问题先尝试解决，解决不了再问用户

---

**最后更新**：2026-09-07
