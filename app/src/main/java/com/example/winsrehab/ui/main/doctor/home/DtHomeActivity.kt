package com.example.winsrehab.ui.main.doctor.home

import android.os.Bundle
import android.util.Log

import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment

import com.example.winsrehab.R
import com.example.winsrehab.databinding.ActivityDtHomeBinding

// 医生端宿主页：只负责导航图、底部导航与工号传递，不再为传患者数建适配器
class DtHomeActivity : AppCompatActivity() {
    lateinit var binding: ActivityDtHomeBinding
    var doctorCode: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDtHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        doctorCode = intent.getStringExtra("doctorCode") ?: ""
        Log.d("DtHomeActivity", "doctorCode: $doctorCode")

        //设置底部导航栏
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_container) as NavHostFragment
        val navController = navHostFragment.navController

        //设置 NavGraph 并传递初始参数给起始 Fragment
        val navGraph = navController.navInflater.inflate(R.navigation.dt_bot_nav)
        val startArgs = Bundle().apply {
            putString("doctorCode", doctorCode)
        }
        navController.setGraph(navGraph, startArgs)

        //自定义底部导航点击，传递参数
        binding.bottomNav.setOnItemSelectedListener { item ->
            val navOptions = NavOptions.Builder()
                .setPopUpTo(R.id.dt_bot_nav, inclusive = false)
                .setLaunchSingleTop(true)
                .build()

            when (item.itemId) {
                R.id.dt_homeFragment -> {
                    val bundle = Bundle().apply {
                        putString("doctorCode", doctorCode)
                    }
                    navController.navigate(R.id.dt_homeFragment, bundle, navOptions)
                    true
                }
                R.id.dt_pt_manageFragment -> {
                    val bundle = Bundle().apply {
                        putString("doctorCode", doctorCode)
                    }
                    navController.navigate(R.id.dt_pt_manageFragment, bundle, navOptions)
                    true
                }
                R.id.dt_info_Fragment -> {
                    // 患者数由「我的」页面自行观察，这里只传工号
                    val bundle = Bundle().apply {
                        putString("doctorCode", doctorCode)
                    }
                    Log.d("DtHomeActivity", "Navigating to dt_info_Fragment with doctorCode: $doctorCode")
                    navController.navigate(R.id.dt_info_Fragment, bundle, navOptions)
                    true
                }
                else -> false
            }
        }
    }
}
