package com.example.winsrehab.ui.main.doctor.home

import android.os.Bundle
import android.util.Log

import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment

import com.example.winsrehab.R
import com.example.winsrehab.databinding.ActivityDtHomeBinding
import com.example.winsrehab.ui.main.doctor.adapter.PatientAdapter

class DtHomeActivity : AppCompatActivity() {
    lateinit var binding: ActivityDtHomeBinding
    private val viewModel: DtHomeVM by viewModels()
    var doctorCode: String = ""
    var ptCount: Int = 0

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
                .setPopUpTo(R.id.dt_bot_nav, inclusive = true)
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
                R.id.dt_info_Fragment -> {
                    val bundle = Bundle().apply {
                        putString("doctorCode", doctorCode)
                        putInt("totalCount", ptCount)
                    }
                    Log.d("DtHomeActivity", "Navigating to dt_info_Fragment with doctorCode: $doctorCode, ptCount: $ptCount")
                    navController.navigate(R.id.dt_info_Fragment, bundle, navOptions)
                    true
                }
                else -> false
            }
        }

        //观察患者数据变化，更新患者总数
        val adapter = PatientAdapter()
        viewModel.patients.observe(this) { list ->
            adapter.submitList(list)
            ptCount = list.size
            Log.d("DtHomeActivity", "PtCount updated: $ptCount")
        }

        //加载患者数据
        viewModel.loadPatients(doctorCode)
    }
}
