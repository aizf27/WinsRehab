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
                 val signature: String? = null,          //个性签名
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
                null, // signature
                null, // aiResult
                false, // hasAlert
                null  // lastTrainingDate
        )
}