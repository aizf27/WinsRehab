package com.example.winsrehab.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "doctor")
data class Doctor(
    @PrimaryKey val id: String,            //工号
    val password: String,                  //密码
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
