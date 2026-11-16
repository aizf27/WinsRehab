package com.example.winsrehab

import android.app.Application
import android.util.Log
import androidx.room.Room
import com.example.winsrehab.data.database.AppDatabase
import com.example.winsrehab.data.database.DatabaseMigrations
import com.example.winsrehab.data.repository.PatientBindingRepair
import com.example.winsrehab.utils.TestDataHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
            // 开发阶段：数据库结构变更时，删除旧数据库重建
            .fallbackToDestructiveMigration()
            
            // 正式发布时使用Migration（取消上面的fallbackToDestructiveMigration，启用下面的addMigrations）
            // .addMigrations(
            //     DatabaseMigrations.MIGRATION_3_4,
            //     DatabaseMigrations.MIGRATION_4_5,
            //     DatabaseMigrations.MIGRATION_6_7
            // )
            .build()
        
        // 初始化测试数据（确保有医生数据）
        TestDataHelper.initTestDoctors()

        // 修一次旧版本的占位医生绑定，修完患者才能重新绑定并把工号显示出来
        CoroutineScope(Dispatchers.IO).launch {
            try {
                PatientBindingRepair.repair(database.patientDao(), database.doctorTaskDao())
            } catch (e: Exception) {
                Log.e("MyApp", "旧绑定数据修复失败，不影响启动", e)
            }
        }
    }

    companion object {
        lateinit var instance: MyApp
            private set
        //外部只能通过 MyApp.instance 读取实例
    }
}
