package com.example.winsrehab.ui.main.patient.main

import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.winsrehab.R
import com.example.winsrehab.databinding.ActivityPtMainBinding
import com.example.winsrehab.ui.main.patient.info.PtInfoVM

class PtMainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPtMainBinding
    private val infoViewModel: PtInfoVM by viewModels()
    private var account: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPtMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 从Intent获取account
        account = intent.getStringExtra("account") ?: ""
        Log.d("PtMainActivity", "account: $account")

        // 设置Fragment容器和导航
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_container) as NavHostFragment
        val navController = navHostFragment.navController

        // 设置导航图并传递参数
        val navGraph = navController.navInflater.inflate(R.navigation.pt_bot_nav)
        val bundle = Bundle().apply {
            putString("account", account)
        }
        navController.setGraph(navGraph, bundle)

        // 设置底部导航栏
        binding.bottomNav.setupWithNavController(navController)

        // 加载患者信息并更新顶部栏
        loadPatientInfo()

        // 监听信息更新通知
        supportFragmentManager.setFragmentResultListener("patient_info_result", this) { _, result ->
            if (result.getBoolean("patient_info_updated", false)) {
                loadPatientInfo() // 重新加载信息
            }
        }
    }

    private fun loadPatientInfo() {
        infoViewModel.loadPatient(account)
        infoViewModel.patient.observe(this) { patient ->
            patient?.let {
                // 更新顶部栏
                binding.tvNickname.text = it.name ?: "患者"
                binding.tvSignature.text = "康复进度: ${it.progress}%"

                // 可以在这里设置头像（如果有的话）
                // binding.ivAvatar.setImageResource(...)
            }
        }
    }
}