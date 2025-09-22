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
            .fallbackToDestructiveMigration()
            .build()
    }

    companion object {
        lateinit var instance: MyApp
            private set
        //外部只能通过 MyApp.instance 读取实例
    }
}
