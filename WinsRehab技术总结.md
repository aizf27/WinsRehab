# WinsRehab 康复医疗应用技术总结

## 1. 项目概述

WinsRehab是一款基于Android平台开发的康复医疗应用，旨在为患者提供便捷的康复训练指导和心理支持服务，同时为医生提供患者管理工具。应用采用Kotlin语言开发，使用MVVM架构模式，集成了现代Android开发技术栈，提供了专业的康复医疗解决方案。

### 1.1 核心功能

- **双角色支持**：系统支持患者和医生两种用户角色，提供不同的功能入口和操作界面
- **康复训练**：为患者提供个性化的康复训练视频指导
- **心理支持**：内置心理助手功能，帮助患者进行心理调节和恢复
- **患者管理**：医生端可查看和管理患者信息
- **数据持久化**：使用Room数据库存储用户信息和康复训练数据

### 1.2 技术栈

- **编程语言**：Kotlin
- **架构模式**：MVVM (Model-View-ViewModel)
- **数据持久化**：Room Database
- **UI组件**：Android XML布局 + 部分Jetpack Compose
- **架构组件**：ViewModel, LiveData, Lifecycle, ViewBinding
- **网络请求**：OkHttp
- **数据处理**：Gson

## 2. 系统架构设计

WinsRehab应用采用MVVM架构模式，遵循Android官方推荐的应用架构指南，实现了UI与业务逻辑的清晰分离，提高了代码的可维护性和可测试性。

### 2.1 架构概览

```
+----------------+      +----------------+      +----------------+
|                |      |                |      |                |
|    View层      |<---->|    ViewModel   |<---->|    Model层     |
|  (Activities)  |      |                |      |                |
|                |      |                |      |                |
+----------------+      +----------------+      +----------------+
                                                         ^
                                                         |
                                                   +----------------+
                                                   |                |
                                                   |    Repository  |
                                                   |                |
                                                   +----------------+
                                                         ^
                                                         |
                                                   +----------------+
                                                   |                |
                                                   |  数据源(Room)  |
                                                   |                |
                                                   +----------------+
```

### 2.2 核心组件

- **View层**：主要由Activities和XML布局文件组成，使用ViewBinding进行UI绑定，负责UI展示和用户交互
- **ViewModel层**：持有UI所需的数据，处理业务逻辑，在配置变更时保存数据状态，使用Coroutines进行异步操作
- **Model层**：定义数据结构和业务实体，负责数据的来源和存储
- **Repository**：统一管理数据源，为ViewModel提供数据访问接口
- **Room数据库**：提供本地数据持久化功能

## 3. 核心技术实现

### 3.1 数据持久化实现

WinsRehab使用Room数据库实现本地数据持久化，设计了四个主要实体表：Doctor（医生）、Patient（患者）、PtRehabVideo（患者康复视频）和DemoVideo（演示视频）。

**数据库配置**：

```kotlin
package com.example.winsrehab.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.winsrehab.data.dao.DemoVideoDao
import com.example.winsrehab.data.dao.DoctorDao
import com.example.winsrehab.data.dao.PatientDao
import com.example.winsrehab.data.dao.PtRehabVideoDao
import com.example.winsrehab.data.entity.DemoVideo
import com.example.winsrehab.data.entity.Doctor
import com.example.winsrehab.data.entity.Patient
import com.example.winsrehab.data.entity.PtRehabVideo

@Database(entities = [Doctor::class, Patient::class, PtRehabVideo::class, DemoVideo::class],
    version = 3, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun doctorDao(): DoctorDao
    abstract fun patientDao(): PatientDao
    abstract fun ptRehabVideoDao(): PtRehabVideoDao
    abstract fun demoVideoDao(): DemoVideoDao
}
```

**实体设计 - 医生实体**：

```kotlin
package com.example.winsrehab.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "doctor")
data class Doctor(
    @PrimaryKey val id: String,            //工号
    val password: String,                 //密码
    val name: String? = null,              //姓名
    val gender: String? = null,            //性别
    val age: Int? =0,                  //年龄
    val department: String? = null,        //科室
    val patientCount: Int = 0              //当前患者数
){
    constructor(id: String,password: String):this(id,password,null,null,null,null,0)

    constructor(id: String, name: String, gender: String, age: Int, department: String, patientCount: Int) : this(
        id, "", name, gender, age, department, patientCount)
}
```

**实体设计 - 患者实体**：

```kotlin
package com.example.winsrehab.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "patient")
data class Patient (

         @PrimaryKey val id : String,   //主键
                 val account : String,  //手机号
                 val password : String, //密码
                 val name: String? = null,//姓名
                 val age :Int= 0,    //年龄
                 val gender: String? = null, //性别
                 val physicianName: String? = null ,     //医生姓名
                 val physicianCode: String? = null ,     //医生工号
                 val diagnosis: String? = null,    //诊断/病情描述
                 val stage: String? = null ,             //康复阶段（如：软瘫期/痉挛期/恢复期）
                 val progress :Int= 0,                      //康复进度（0-100）
                 val aiResult: String? = null,           //AI 分析结果
                 val hasAlert : Boolean= false,                //是否有异常预警
                 val lastTrainingDate: String? = null //最近训练日期

){
        //添加一个次要构造函数，用于创建包含姓名、性别、年龄和医生信息的患者实例
        constructor(id: String,name: String, gender: String, age: Int, physicianName: String, physicianCode: String) : this(
                "", // id
                "", // account
                "", // password
                name,
                age,
                gender,
                physicianName,
                physicianCode,
                null, // diagnosis
                null, // stage
                0,    // progress
                null, // aiResult
                false, // hasAlert
                null  // lastTrainingDate
        )
}
```

**实体设计 - 康复视频实体**：

```kotlin
package com.example.winsrehab.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "ptrehab_video")
data class PtRehabVideo(
    @PrimaryKey(autoGenerate = true)
    val videoId: Long = 0,         //主键，自增
    val patientId: String="",         //对应 Patient.id
    val videoUrl: String="",          //视频路径/URL
    val uploadDate: String="",        //上传日期yyyy-MM-dd HH:mm
    val description: String? = null//备注/说明
)
```

### 3.2 应用入口与初始化

应用使用自定义的Application类（MyApp）作为全局入口，负责初始化Room数据库并提供单例访问。

```kotlin
package com.example.winsrehab

import android.app.Application
import androidx.room.Room
import com.example.winsrehab.data.database.AppDatabase

class MyApp : Application() {

    lateinit var database: AppDatabase
        private set     //外部不能随意修改数据库实例

    override fun onCreate() {
        super.onCreate()

        instance = this
        //只有在这里才能设置 database
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "rehab.db"
        )
            .fallbackToDestructiveMigration()   //升级就把旧数据删光光
            .build()
    }

    companion object {
        lateinit var instance: MyApp
            private set
        //外部只能通过 MyApp.instance 读取实例
    }
}
```

### 3.3 用户认证与路由

应用根据不同用户角色提供了不同的登录入口和主页：

- **患者端**：通过PtLogActivity登录，成功后进入PtHomeActivity
- **医生端**：通过DtLogActivity登录，成功后进入DtHomeActivity

应用的主要活动包括：
- DtLogActivity：医生登录页面
- DtHomeActivity：医生主页
- DtInfoActivity：医生信息页面
- PtLogActivity：患者登录页面
- PtHomeActivity：患者主页
- PtInfoActivity：患者信息页面
- TrainingVideoActivity：训练视频页面
- PsychologyActivity：心理助手页面
- VideoPlayerActivity：视频播放器页面

## 4. 关键模块详解

### 4.1 患者端功能模块

#### 4.1.1 患者主页

患者主页提供了三个主要功能入口：个人信息、训练视频和心理助手。

```kotlin
package com.example.winsrehab.ui.main.patient.home

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.winsrehab.databinding.ActivityPtHomeBinding
import com.example.winsrehab.ui.main.patient.Video.TrainingVideoActivity
import com.example.winsrehab.ui.main.patient.info.PtInfoActivity
import com.example.winsrehab.ui.main.patient.psychology.PsychologyActivity

class PtHomeActivity: AppCompatActivity() {
    private lateinit var binding: ActivityPtHomeBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPtHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //个人信息
        binding.btnViewInfo.setOnClickListener({ view ->
            val intent: Intent = Intent(this, PtInfoActivity::class.java)
            //把 account 传过去
            val account = getIntent().getStringExtra("account")
            intent.putExtra("mode", "patient")
            intent.putExtra("account", account)
            startActivity(intent)
        })
        //跳到示范视频页
        binding.cardTraining.setOnClickListener({ view ->
            val intent: Intent = Intent(this, TrainingVideoActivity::class.java)
            startActivity(intent)
        })
        //跳到心理助手页
        binding.cardPsychology.setOnClickListener({ view ->
            val intent: Intent = Intent(this, PsychologyActivity::class.java)
            startActivity(intent)
        })
    }
}
```

#### 4.1.2 康复训练模块

该模块管理和展示患者的康复训练视频，支持视频播放和进度跟踪。系统会根据患者的诊断信息推荐个性化的训练计划。训练视频数据通过PtRehabVideo实体进行存储和管理。

#### 4.1.3 心理助手模块

提供心理支持和咨询服务，帮助患者进行心理调节，促进全面康复。

### 4.2 医生端功能模块

#### 4.2.1 医生主页

医生主页展示所管理的患者列表，提供患者信息查看和管理功能。

```kotlin
package com.example.winsrehab.ui.main.doctor.home

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.winsrehab.databinding.ActivityDtHomeBinding
import com.example.winsrehab.ui.main.doctor.adapter.PatientAdapter
import com.example.winsrehab.ui.main.doctor.info.DtInfoActivity

class DtHomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDtHomeBinding
    private val viewModel: DtHomeVM by viewModels()
    var doctorCode: String=""
    var PtCount:Int=0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDtHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

         doctorCode = intent.getStringExtra("doctorCode") ?: ""
        Log.d("DtHomeActivity", "doctorCode: $doctorCode")

        val adapter= PatientAdapter()
//        让列表纵向排列
        binding.rvKeyPatients.layoutManager = LinearLayoutManager(this)
        //绑定adapter
        binding.rvKeyPatients.adapter = adapter
        //观察数据变化并更新UI
        viewModel.patients.observe(this) { list ->
            adapter.submitList(list)  //把最新的患者列表交给adapter，触发DiffUtil检查变化并刷新
            //更新患者总数
            PtCount=adapter.itemCount
            binding.tvTotalValue.text = PtCount.toString()
            Log.d("DtHomeActivity", "PtCount: $PtCount")
        }

        binding.btnInfoCenter.setOnClickListener {
            val intent = Intent(this, DtInfoActivity::class.java)
             intent.putExtra("doctorCode", doctorCode)
             intent.putExtra("totalCount", PtCount)
            startActivity(intent)
        }

        viewModel.loadPatients(doctorCode)
        binding.tvTotalValue.text=adapter.itemCount.toString()
    }
}
```

#### 4.2.2 医生主页ViewModel

医生主页使用ViewModel模式管理数据和业务逻辑，实现了数据与UI的分离。

```kotlin
package com.example.winsrehab.ui.main.doctor.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.winsrehab.MyApp
import com.example.winsrehab.data.entity.Patient
import com.example.winsrehab.data.repository.PatientRepository
import kotlinx.coroutines.launch

class DtHomeVM: ViewModel (){
    private val patientRepository = PatientRepository(MyApp.instance.database.patientDao())

    private val _patients = MutableLiveData<List<Patient>>()
    val patients: LiveData<List<Patient>> = _patients

    fun loadPatients(doctorCode: String) {
        viewModelScope.launch {
            patientRepository.getPatientsByDoctor(doctorCode)
                .collect { list ->
                    _patients.postValue(list)
                }
        }
    }

}
```

#### 4.2.3 患者管理模块

医生可以查看、编辑患者信息，包括基本信息、病历、诊断结果和康复进度等。系统使用RecyclerView高效展示患者列表，并通过DiffUtil优化列表更新性能。

## 5. 技术难点与解决方案

### 5.1 高效数据展示

**挑战**：医生端需要展示大量患者数据，普通的列表渲染方式可能导致性能问题。

**解决方案**：应用使用RecyclerView配合DiffUtil实现高效的数据更新，只刷新发生变化的项，提高列表滚动流畅度和内存使用效率。在DtHomeActivity中可以看到，通过调用adapter.submitList(list)触发DiffUtil进行差异比较并智能刷新列表。

### 5.2 数据持久化与版本管理

**挑战**：应用数据需要持久化存储，并且在应用升级时需要处理数据库版本变更。

**解决方案**：使用Room数据库的版本管理机制，当前应用数据库版本为3。在MyApp中使用fallbackToDestructiveMigration()策略简化开发阶段的数据库迁移。

```kotlin
database = Room.databaseBuilder(
    applicationContext,
    AppDatabase::class.java,
    "rehab.db"
)
    .fallbackToDestructiveMigration()   //升级就把旧数据删光光
    .build()
```

### 5.3 MVVM架构实现

**挑战**：实现数据与UI的分离，保证配置变更时数据不丢失。

**解决方案**：
1. 使用ViewModel存储UI数据和业务逻辑，通过LiveData实现数据观察
2. 使用viewModelScope处理异步操作，避免内存泄漏
3. 在DtHomeVM中，使用协程从Repository加载数据，并通过LiveData通知UI更新

## 6. 代码质量与开发规范

### 6.1 代码组织与命名规范

项目采用清晰的包结构，按功能模块和架构层次组织代码：
- `com.example.winsrehab`：应用根包
- `com.example.winsrehab.data`：数据层，包含database、dao、entity和repository
- `com.example.winsrehab.ui`：UI层，按用户角色和功能模块组织
  - `com.example.winsrehab.ui.main.doctor`：医生相关UI组件
  - `com.example.winsrehab.ui.main.patient`：患者相关UI组件

### 6.2 架构设计原则

项目遵循以下架构设计原则：
1. **单一职责原则**：每个类和方法只负责一个功能
2. **数据驱动UI**：UI通过观察数据变化自动更新
3. **视图绑定**：使用ViewBinding代替findViewById，提高代码安全性和可读性
4. **依赖注入**：通过构造函数注入依赖，提高代码可测试性

### 6.3 错误处理与日志

项目使用Logcat输出详细的日志信息，便于问题排查：

```kotlin
Log.d("DtHomeActivity", "doctorCode: $doctorCode")
Log.d("DtHomeActivity", "PtCount: $PtCount")
```

## 7. 项目成果与未来规划

### 7.1 已实现功能

- 患者和医生双角色支持
- 用户认证与登录系统
- 患者个人信息管理
- 康复训练视频管理与播放
- 心理助手功能
- 医生端患者管理功能
- Room数据库数据持久化

### 7.2 未来规划

1. **功能扩展**
   - 添加远程视频会诊功能
   - 引入康复进度数据分析与可视化
   - 实现医生与患者的即时通讯功能
   - 增加智能康复方案推荐算法

2. **性能优化**
   - 实现Room数据库的自定义迁移策略，替代fallbackToDestructiveMigration
   - 实现数据缓存策略，减少网络请求
   - 优化大列表滚动性能

3. **技术升级**
   - 逐步迁移到Jetpack Compose实现UI
   - 集成Hilt实现依赖注入
   - 使用Flow替代LiveData，更好地支持异步数据流

## 8. 总结与经验分享

WinsRehab项目通过采用现代Android开发技术栈和最佳实践，成功实现了一个功能完备的康复医疗应用。项目开发过程中，我们深刻体会到良好架构设计和代码规范对于应用可维护性和扩展性的重要性。

### 8.1 关键经验

1. **架构选择至关重要**：MVVM架构使代码结构清晰，降低了模块间的耦合度，提高了代码可维护性。通过ViewModel和LiveData的结合，有效解决了配置变更导致的数据丢失问题。

2. **数据持久化设计**：合理的数据模型设计是应用稳定运行的基础，Room数据库提供了强大且易用的本地数据持久化解决方案。通过实体类和Dao接口的设计，简化了数据库操作。

3. **ViewBinding的使用**：使用ViewBinding代替findViewById，避免了空指针异常和类型转换错误，提高了代码的安全性和可读性。

4. **协程与异步操作**：使用Kotlin协程和viewModelScope处理异步操作，使异步代码更加清晰易读，同时避免了内存泄漏问题。

### 8.2 技术学习与成长

项目开发过程中，团队成员对Android现代开发技术栈有了更深入的理解和应用，特别是在MVVM架构、Room数据库、ViewModel、LiveData和Kotlin协程等技术的应用方面积累了宝贵经验。同时，对于康复医疗领域的专业知识也有了一定的了解，为后续项目开发奠定了基础。

---

通过不断优化和完善，WinsRehab应用将继续为康复医疗领域提供更加专业、便捷的服务，帮助更多患者进行科学有效的康复训练，提高生活质量。