package com.example.winsrehab.ui.main.doctor.home

import android.content.Intent
import android.os.Bundle
import android.util.Log

import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.winsrehab.R
import com.example.winsrehab.databinding.ActivityDtHomeBinding
import com.example.winsrehab.ui.main.doctor.adapter.PatientAdapter

import com.google.android.material.bottomnavigation.BottomNavigationView

class DtHomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDtHomeBinding
    private val viewModel: DtHomeVM by viewModels()
    var doctorCode: String=""
    var PtCount:Int=0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDtHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

         doctorCode = intent.getStringExtra("doctorCode") ?: ""
        Log.d("DtHomeActivity", "doctorCode: $doctorCode")

        //设置片段容器
        val navHostFragment=supportFragmentManager.findFragmentById(R.id.nav_host_fragment_container) as NavHostFragment
        val navController=navHostFragment.navController
        

        //设置底部导航栏（连接导航图）
        val navGraph = navController.navInflater.inflate(R.navigation.dt_bot_nav)
        //为所有Fragment设置doctorCode参数
        val bundle = Bundle().apply {
            putString("doctorCode", doctorCode)
            putInt("totalCount", PtCount) // 预先设置totalCount参数
        }
        navController.setGraph(navGraph, bundle)
        //导航行为使用 NavigationUI 默认逻辑
        binding.bottomNav.setupWithNavController(navController)

        val adapter= PatientAdapter()
//        让列表纵向排列
     //   binding.rvKeyPatients.layoutManager = LinearLayoutManager(this)
        //绑定adapter
     //   binding.rvKeyPatients.adapter = adapter

        //观察数据变化并更新UI
        viewModel.patients.observe(this) { list ->
            adapter.submitList(list)  //把最新的患者列表交给adapter，触发DiffUtil检查变化并刷新
            //更新患者总数
            PtCount=adapter.itemCount
            //binding.tvTotalValue.text = PtCount.toString()
            Log.d("DtHomeActivity", "PtCount: $PtCount")
            // 将参数写入当前目的地的 savedStateHandle，供信息页读取
            navController.currentBackStackEntry
                ?.savedStateHandle
                ?.apply {
                    set("doctorCode", doctorCode)
                    set("totalCount", PtCount)
                }
        }

//     //   binding.btnInfoCenter.setOnClickListener {
//            val intent = Intent(this, DtInfoActivity::class.java)
//             intent.putExtra("doctorCode", doctorCode)
//             intent.putExtra("totalCount", PtCount)
//            startActivity(intent)
//        }
//
        viewModel.loadPatients(doctorCode)
       // binding.tvTotalValue.text=adapter.itemCount.toString()
    }
}

