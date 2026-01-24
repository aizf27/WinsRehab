# UI数据填充梳理报告

## 📋 项目概述

本文档梳理了患者端和医生端个人信息页面的UI数据填充逻辑，并补充了编辑页面缺失的字段。

---

## 一、数据库实体结构

### 1.1 Doctor 实体（医生）

```kotlin
@Entity(tableName = "doctor")
data class Doctor(
    @PrimaryKey val doctorCode: String,        // 医生工号（唯一标识）
    val password: String,                      // 密码
    val name: String = "未设置",                // 姓名
    val gender: String = "未设置",              // 性别
    val age: Int = 0,                          // 年龄
    val title: String = "未设置",               // 职称
    val department: String = "未设置",          // 科室
    val hospital: String = "未设置",            // 所属医院
    val licenseNumber: String = "未设置",       // 医师资格证号
    val phone: String = "未设置",               // 手机号码
    val email: String = "未设置",               // 电子邮箱
    val avatar: String? = null,                // 头像
    val patientCount: Int = 0,                 // 管理患者数
    val monthlyCompletedPlans: Int = 0,        // 本月完成计划数
    val satisfactionRate: Float = 0f,          // 患者满意度
    val createdAt: Long,                       // 创建时间
    val updatedAt: Long                        // 更新时间
)
```

### 1.2 Patient 实体（患者）

```kotlin
@Entity(tableName = "patient")
data class Patient(
    @PrimaryKey val patientId: String,         // 患者ID
    val account: String,                       // 登录账号
    val password: String,                      // 密码
    val name: String = "未设置",                // 姓名
    val gender: String = "未设置",              // 性别
    val age: Int = 0,                          // 年龄
    val phone: String = "未设置",               // 手机号码
    val email: String = "未设置",               // 电子邮箱
    val dateOfBirth: String = "未设置",         // 出生日期
    val address: String = "未设置",             // 地址
    val avatar: String? = null,                // 头像
    val nickname: String = "未设置",            // 昵称（社区显示）
    val signature: String = "这个人很懒，什么都没留下", // 个性签名
    
    // 医生信息（只读）
    val doctorCode: String = "未设置",          // 主治医生工号
    val doctorName: String = "未设置",          // 主治医生姓名
    
    // 康复信息（只读）
    val diagnosis: String = "未设置",           // 诊断信息
    val rehabStage: String = "未设置",          // 康复阶段
    val rehabCenter: String = "未设置",         // 康复中心
    val rehabStartDate: String = "未设置",      // 康复开始日期
    val rehabDays: Int = 0,                    // 累计康复天数
    val overallProgress: Int = 0,              // 总体康复进度
    
    // 其他字段...
    val createdAt: Long,
    val updatedAt: Long
)
```

---

## 二、医生端信息页面

### 2.1 展示页面（fragment_dt_info_.xml）

**布局文件：** `app/src/main/res/layout/fragment_dt_info_.xml`  
**Fragment：** `dt_info_Fragment.kt`  
**ViewModel：** `DtInfoVM.kt`

#### 数据填充逻辑

```kotlin
// 顶部卡片
binding.tvDoctorName.text = doctor.name
binding.tvDoctorInfo.text = "${doctor.gender} · ${doctor.age}岁 · ${doctor.department}"
binding.tvDoctorCode.text = "工号：${doctor.doctorCode}"

// 执业信息板块
- 医师资格证号：doctor.licenseNumber
- 所属医院：doctor.hospital
- 职称：doctor.title
- 管理患者数：doctor.patientCount

// 联系方式板块
- 手机号码：doctor.phone
- 电子邮箱：doctor.email

// 工作统计板块
- 本月完成：doctor.monthlyCompletedPlans
- 患者满意度：doctor.satisfactionRate
```

### 2.2 编辑页面（fragment_dt_info_edit.xml）

**布局文件：** `app/src/main/res/layout/fragment_dt_info_edit.xml`  
**Fragment：** `DtInfoEditFragment.kt`

#### 可编辑字段

| 字段 | 数据库字段 | 编辑方式 | 验证规则 |
|------|-----------|---------|---------|
| 头像 | avatar | 点击上传（待实现） | - |
| 姓名 | name | 文本输入 | 必填，最多20字符 |
| 性别 | gender | 单选对话框 | 必填，男/女 |
| 年龄 | age | 数字输入 | 必填，1-120 |
| 科室 | department | 单选对话框 | 可选，预设选项+自定义 |
| 职称 | title | 单选对话框 | 可选，预设选项+自定义 |
| 所属医院 | hospital | 文本输入 | 可选，最多50字符 |
| 医师资格证号 | licenseNumber | 文本输入 | 可选，最多30字符 |
| 手机号码 | phone | 手机号输入 | 可选，11位手机号格式 |
| 电子邮箱 | email | 文本输入 | 可选，最多50字符 |

#### 只读字段

- **工号**（doctorCode）：系统分配，不可修改

#### 保存逻辑

```kotlin
val updatedDoctor = doctor.copy(
    name = name,
    gender = gender,
    age = age,
    department = department,
    title = title,
    hospital = hospital,
    licenseNumber = licenseNumber,
    phone = phone,
    email = email,
    updatedAt = System.currentTimeMillis()
)
viewModel.saveDoctorInfo(updatedDoctor)
```

---

## 三、患者端信息页面

### 3.1 展示页面（fragment_pt_info.xml）

**布局文件：** `app/src/main/res/layout/fragment_pt_info.xml`  
**Fragment：** `pt_infoFragment.kt`  
**ViewModel：** `PtInfoVM.kt`

#### 数据填充逻辑

```kotlin
// 顶部卡片
binding.tvPatientName.text = patient.name
binding.tvPatientGender.text = "${patient.gender} · ${patient.age}岁"
binding.tvPatientSignature.text = patient.signature

// 基本信息板块
- 电话：patient.phone
- 邮箱：patient.email
- 出生日期：patient.dateOfBirth
- 地址：patient.address

// 康复信息板块（只读）
- 康复记录：查看详情（跳转到康复中心）
- 训练计划：查看详情
- 主治医生：patient.doctorName
```

### 3.2 编辑页面（fragment_pt_info_edit.xml）

**布局文件：** `app/src/main/res/layout/fragment_pt_info_edit.xml`  
**Fragment：** `PtInfoEditFragment.kt`

#### 可编辑字段

| 字段 | 数据库字段 | 编辑方式 | 验证规则 |
|------|-----------|---------|---------|
| 头像 | avatar | 点击上传（待实现） | - |
| 姓名 | name | 文本输入 | 必填，最多20字符 |
| 性别 | gender | 单选对话框 | 必填，男/女 |
| 年龄 | age | 数字输入 | 必填，1-120 |
| 昵称 | nickname | 文本输入 | 可选，最多20字符 |
| 个性签名 | signature | 文本输入 | 可选，最多100字符 |
| 手机号码 | phone | 手机号输入 | 可选，11位手机号格式 |
| 电子邮箱 | email | 文本输入 | 可选，最多50字符 |
| 出生日期 | dateOfBirth | 日期选择器 | 可选，yyyy-MM-dd格式 |
| 地址 | address | 文本输入 | 可选，最多100字符 |

#### 只读字段（不可编辑）

**医生信息：**
- 主治医生（doctorName）
- 医生工号（doctorCode）

**康复信息：**
- 诊断信息（diagnosis）
- 康复阶段（rehabStage）

> 说明：康复信息由医生管理，患者无法修改

#### 保存逻辑

```kotlin
val updatedPatient = patient.copy(
    name = name,
    gender = gender,
    age = age,
    nickname = nickname,
    signature = signature,
    phone = phone,
    email = email,
    dateOfBirth = dateOfBirth,
    address = address,
    updatedAt = System.currentTimeMillis()
)
viewModel.savePatientInfo(updatedPatient)
```

---

## 四、修改文件清单

### 4.1 布局文件

1. ✅ `fragment_dt_info_edit.xml` - 医生编辑页面
   - 新增：职称、所属医院、医师资格证号
   - 新增：手机号码、电子邮箱

2. ✅ `fragment_pt_info_edit.xml` - 患者编辑页面
   - 新增：昵称
   - 新增：手机号码、电子邮箱、出生日期、地址

### 4.2 Kotlin代码文件

1. ✅ `DtInfoEditFragment.kt` - 医生编辑Fragment
   - 更新 `observeData()` - 显示新增字段
   - 更新 `setupClickListeners()` - 添加新字段点击事件
   - 新增 `showTitleDialog()` - 职称选择对话框
   - 新增 `showPhoneDialog()` - 手机号输入对话框
   - 更新 `saveDoctorInfo()` - 保存所有字段

2. ✅ `PtInfoEditFragment.kt` - 患者编辑Fragment
   - 更新 `observeData()` - 显示新增字段
   - 更新 `setupClickListeners()` - 添加新字段点击事件
   - 新增 `showPhoneDialog()` - 手机号输入对话框
   - 新增 `showDatePickerDialog()` - 日期选择对话框
   - 更新 `savePatientInfo()` - 保存所有字段

3. ✅ `PtInfoVM.kt` - 患者ViewModel
   - 新增 `savePatientInfo()` - 保存完整患者信息

---

## 五、字段编辑权限总结

### 5.1 医生端

| 字段类型 | 可编辑 | 只读 |
|---------|-------|------|
| 基本信息 | 姓名、性别、年龄、头像 | - |
| 执业信息 | 科室、职称、所属医院、医师资格证号 | 工号 |
| 联系方式 | 手机号码、电子邮箱 | - |
| 统计数据 | - | 管理患者数、本月完成计划数、患者满意度 |

### 5.2 患者端

| 字段类型 | 可编辑 | 只读 |
|---------|-------|------|
| 基本信息 | 姓名、性别、年龄、昵称、个性签名、头像 | - |
| 联系方式 | 手机号码、电子邮箱、出生日期、地址 | - |
| 医生信息 | - | 主治医生、医生工号 |
| 康复信息 | - | 诊断信息、康复阶段 |

---

## 六、数据验证规则

### 6.1 必填字段

**医生端：**
- 姓名
- 性别
- 年龄

**患者端：**
- 姓名
- 性别
- 年龄

### 6.2 格式验证

| 字段 | 验证规则 | 错误提示 |
|------|---------|---------|
| 年龄 | 1-120的整数 | "请输入有效的年龄（1-120）" |
| 手机号 | 11位，1开头，第二位3-9 | "请输入有效的手机号码" |
| 出生日期 | yyyy-MM-dd格式 | 日期选择器自动格式化 |

### 6.3 长度限制

| 字段 | 最大长度 |
|------|---------|
| 姓名 | 20字符 |
| 昵称 | 20字符 |
| 个性签名 | 100字符 |
| 科室 | 20字符 |
| 职称 | 20字符 |
| 所属医院 | 50字符 |
| 医师资格证号 | 30字符 |
| 电子邮箱 | 50字符 |
| 地址 | 100字符 |

---

## 七、UI交互说明

### 7.1 编辑方式

1. **文本输入**：弹出对话框，输入文本
2. **单选对话框**：从预设选项中选择，或选择"其他"自定义输入
3. **数字输入**：弹出对话框，限制数字键盘
4. **手机号输入**：弹出对话框，限制手机号键盘，验证格式
5. **日期选择**：弹出系统日期选择器

### 7.2 预设选项

**科室选项：**
- 康复科
- 骨科
- 神经内科
- 神经外科
- 理疗科
- 其他（自定义输入）

**职称选项：**
- 住院医师
- 主治医师
- 副主任医师
- 主任医师
- 其他（自定义输入）

**性别选项：**
- 男
- 女

---

## 八、待实现功能

1. **头像上传功能**
   - 选择图片
   - 裁剪图片
   - 上传到服务器
   - 更新本地数据库

2. **邮箱格式验证**
   - 添加邮箱格式正则验证

3. **数据同步**
   - 与服务器同步用户信息
   - 处理冲突

---

## 九、测试建议

### 9.1 功能测试

- [ ] 医生端所有字段的编辑和保存
- [ ] 患者端所有字段的编辑和保存
- [ ] 必填字段验证
- [ ] 格式验证（年龄、手机号）
- [ ] 长度限制验证
- [ ] 只读字段不可编辑
- [ ] 数据持久化（重启应用后数据保留）

### 9.2 UI测试

- [ ] 所有对话框正常弹出
- [ ] 日期选择器正常工作
- [ ] 长文本显示省略号
- [ ] 返回按钮正常工作
- [ ] 保存按钮正常工作

### 9.3 边界测试

- [ ] 输入空值
- [ ] 输入超长文本
- [ ] 输入特殊字符
- [ ] 快速连续点击保存按钮

---

## 十、总结

本次修改完成了以下工作：

1. ✅ 梳理了医生和患者实体的所有字段
2. ✅ 分析了展示页面的数据填充逻辑
3. ✅ 补充了编辑页面缺失的字段（除康复信息外）
4. ✅ 实现了所有字段的编辑功能
5. ✅ 添加了数据验证规则
6. ✅ 明确了字段的编辑权限

所有可编辑字段均已添加到编辑页面，并实现了相应的编辑逻辑。康复信息字段按照需求设置为只读，由医生端管理。

---

**文档版本：** 1.0  
**创建日期：** 2026-01-24  
**最后更新：** 2026-01-24

