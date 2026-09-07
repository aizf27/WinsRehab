package com.example.winsrehab.ui.main.patient.main

import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavArgument
import androidx.navigation.NavType
import androidx.navigation.fragment.NavHostFragment

import androidx.navigation.ui.setupWithNavController
import com.example.winsrehab.MyApp
import com.example.winsrehab.R
import com.example.winsrehab.databinding.ActivityPtMainBinding
import com.example.winsrehab.data.repository.TaskInstanceRepository
import com.example.winsrehab.ui.main.patient.info.PtInfoVM
import com.example.winsrehab.utils.TaskGenerationHelper
import kotlinx.coroutines.launch

class PtMainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPtMainBinding
    private val infoViewModel: PtInfoVM by viewModels()
    private var account: String = ""

    private val taskInstanceRepository: TaskInstanceRepository by lazy {
        TaskInstanceRepository(
            MyApp.instance.database.taskInstanceDao(),
            MyApp.instance.database.rehabPlanDao(),
            MyApp.instance.database.trainingTaskDao()
        )
    }

    companion object {
        private const val TAG = "PtMainActivity"
    }

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

        // 首次加载患者信息（供各 Fragment 通过 activityViewModels 共享）
        infoViewModel.loadPatient(account)
    }

    override fun onResume() {
        super.onResume()
        // 界面恢复时刷新任务生成（防止中途激活新计划或跨日期）
        if (account.isNotEmpty()) {
            refreshDailyTasks()
        }
    }

    /**
     * 刷新今日任务生成
     */
    private fun refreshDailyTasks() {
        lifecycleScope.launch {
            try {
                val today = TaskGenerationHelper.getTodayDate()
                val count = taskInstanceRepository.generateDailyTasks(account, today)
                Log.i(TAG, "刷新任务: 新生成${count}个任务")
            } catch (e: Exception) {
                Log.e(TAG, "刷新任务失败", e)
            }
        }
    }
}