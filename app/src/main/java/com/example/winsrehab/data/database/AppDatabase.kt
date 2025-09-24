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
