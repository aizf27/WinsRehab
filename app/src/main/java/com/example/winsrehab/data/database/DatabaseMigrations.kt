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
}



