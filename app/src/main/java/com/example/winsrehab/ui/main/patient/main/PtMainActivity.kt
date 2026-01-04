package com.example.winsrehab.ui.main.patient.main

import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavArgument
import androidx.navigation.NavType
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

        //从Intent获取account
        account = intent.getStringExtra("account") ?: ""
        Log.d("PtMainActivity", "account: $account")

        //设置Fragment容器和导航
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_container) as NavHostFragment
        val navController = navHostFragment.navController

        //设置导航图并传递参数（导航图默认参数）
        val navGraph = navController.navInflater.inflate(R.navigation.pt_bot_nav).apply {
            addArgument(
                "account",
                NavArgument.Builder()
                    .setType(NavType.StringType)
                    .setDefaultValue(account)
                    .build()
            )
        }
        //绑定导航图并把 account 作为启动参数传给 startDestination
        val startArgs = Bundle().apply { putString("account", account) }
        navController.setGraph(navGraph, startArgs)

        //设置底部导航栏
        binding.bottomNav.setupWithNavController(navController)

        // 观察患者信息，更新顶部栏
        infoViewModel.patient.observe(this) { patient ->
            patient?.let {
                renderTopBar(it.name, it.signature, it.progress)
            }
        }
        // 首次加载患者信息
        infoViewModel.loadPatient(account)

        //监听信息更新通知
        supportFragmentManager.setFragmentResultListener("patient_info_result", this) { _, result ->
            val updatedName = result.getString("patient_name")
            val updatedSignature = result.getString("patient_signature")
            
            // 直接使用事件中的新数据更新顶部栏，不依赖重新加载
            renderTopBar(
                updatedName ?: infoViewModel.patient.value?.name,
                updatedSignature ?: infoViewModel.patient.value?.signature,
                infoViewModel.patient.value?.progress
            )
        }
    }

    private fun renderTopBar(name: String?, signature: String?, progress: Int?) {
        binding.tvNickname.text = name ?: "患者"
        //只展示个性签名，没有就给一条默认提示
        binding.tvSignature.text = signature
            ?.takeIf { it.isNotBlank() }
            ?: "这个人还没有写个性签名"

        // 可以在这里设置头像（如果有的话）
        // binding.ivAvatar.setImageResource(...)
    }
}