package com.example.winsrehab.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.winsrehab.data.dao.*
import com.example.winsrehab.data.entity.*

@Database(
    entities = [
        Doctor::class,
        Patient::class,
        PtRehabVideo::class,
        DemoVideo::class,
        DoctorTask::class,
        RehabPlan::class,
        TrainingTask::class,
        TaskInstance::class,
        PlanAdjustment::class
    ],
    version = 7,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun doctorDao(): DoctorDao
    abstract fun patientDao(): PatientDao
    abstract fun ptRehabVideoDao(): PtRehabVideoDao
    abstract fun demoVideoDao(): DemoVideoDao
    abstract fun doctorTaskDao(): DoctorTaskDao
    
    // 新增的康复计划相关DAO
    abstract fun rehabPlanDao(): RehabPlanDao
    abstract fun trainingTaskDao(): TrainingTaskDao
    abstract fun taskInstanceDao(): TaskInstanceDao
    abstract fun planAdjustmentDao(): PlanAdjustmentDao
}
