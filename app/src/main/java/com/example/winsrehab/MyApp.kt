package com.example.winsrehab

import android.app.Application
import androidx.room.Room
import com.example.winsrehab.data.database.AppDatabase
import com.example.winsrehab.utils.TestDataHelper

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
            .fallbackToDestructiveMigration()  // 数据库结构变更时，删除旧数据库重建
            .build()
        
        // 初始化测试数据（确保有医生数据）
        TestDataHelper.initTestDoctors()
    }

    companion object {
        lateinit var instance: MyApp
            private set
        //外部只能通过 MyApp.instance 读取实例
    }
}
