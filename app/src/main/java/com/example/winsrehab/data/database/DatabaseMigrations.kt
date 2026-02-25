package com.example.winsrehab.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room migrations used by AppDatabase.
 */
object DatabaseMigrations {
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE patient ADD COLUMN signature TEXT")
        }
    }
    
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // 添加 bindingStatus 字段到 patient 表
            database.execSQL("ALTER TABLE patient ADD COLUMN bindingStatus TEXT NOT NULL DEFAULT 'unbound'")
            
            // 创建 doctor_task 表
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS doctor_task (
                    taskId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    doctorCode TEXT NOT NULL,
                    taskType TEXT NOT NULL,
                    patientId TEXT NOT NULL,
                    patientName TEXT NOT NULL,
                    patientAccount TEXT NOT NULL,
                    status TEXT NOT NULL DEFAULT 'pending',
                    createdAt INTEGER NOT NULL
                )
            """.trimIndent())
        }
    }
    
    /**
     * 版本6到版本7的迁移：添加康复计划相关表
     */
    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // 1. 创建康复计划表 (rehab_plan)
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS rehab_plan (
                    planId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    patientId TEXT NOT NULL,
                    doctorCode TEXT NOT NULL,
                    planName TEXT NOT NULL,
                    goal TEXT NOT NULL,
                    startDate TEXT NOT NULL,
                    endDate TEXT NOT NULL,
                    status TEXT NOT NULL DEFAULT 'draft',
                    progress INTEGER NOT NULL DEFAULT 0,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
            """.trimIndent())
            
            // 为 rehab_plan 创建索引
            database.execSQL("CREATE INDEX IF NOT EXISTS index_rehab_plan_patientId ON rehab_plan(patientId)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_rehab_plan_status ON rehab_plan(status)")
            
            // 2. 创建训练任务模板表 (training_task)
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS training_task (
                    taskId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    planId INTEGER NOT NULL,
                    taskName TEXT NOT NULL,
                    taskType TEXT NOT NULL,
                    description TEXT NOT NULL DEFAULT '',
                    suggestedDuration INTEGER NOT NULL,
                    frequency TEXT NOT NULL,
                    frequencyDetail TEXT NOT NULL DEFAULT '',
                    priority TEXT NOT NULL DEFAULT 'normal',
                    referenceVideoId INTEGER,
                    createdAt INTEGER NOT NULL
                )
            """.trimIndent())
            
            // 为 training_task 创建索引
            database.execSQL("CREATE INDEX IF NOT EXISTS index_training_task_planId ON training_task(planId)")
            
            // 3. 创建任务实例表 (task_instance)
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS task_instance (
                    instanceId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    taskId INTEGER NOT NULL,
                    patientId TEXT NOT NULL,
                    scheduledDate TEXT NOT NULL,
                    scheduledTime TEXT NOT NULL DEFAULT '',
                    status TEXT NOT NULL DEFAULT 'pending',
                    completedAt INTEGER,
                    actualDuration INTEGER,
                    videoId INTEGER,
                    createdAt INTEGER NOT NULL
                )
            """.trimIndent())
            
            // 为 task_instance 创建索引
            database.execSQL("CREATE INDEX IF NOT EXISTS index_task_instance_patientId_scheduledDate ON task_instance(patientId, scheduledDate)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_task_instance_taskId ON task_instance(taskId)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_task_instance_status ON task_instance(status)")
            
            // 4. 创建计划调整记录表 (plan_adjustment)
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS plan_adjustment (
                    adjustmentId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    planId INTEGER NOT NULL,
                    doctorCode TEXT NOT NULL,
                    adjustmentType TEXT NOT NULL,
                    adjustmentDetail TEXT NOT NULL,
                    reason TEXT NOT NULL DEFAULT '',
                    createdAt INTEGER NOT NULL
                )
            """.trimIndent())
            
            // 为 plan_adjustment 创建索引
            database.execSQL("CREATE INDEX IF NOT EXISTS index_plan_adjustment_planId ON plan_adjustment(planId)")
        }
    }
}



