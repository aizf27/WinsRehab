package com.example.winsrehab.ui.main.patient.binding

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.winsrehab.databinding.ActivityWaitingBindingBinding
import com.example.winsrehab.ui.main.patient.main.PtMainActivity

class WaitingBindingActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityWaitingBindingBinding
    private val viewModel: WaitingBindingViewModel by viewModels()
    private val handler = Handler(Looper.getMainLooper())
    private var account: String = ""
    
    private val refreshRunnable = object : Runnable {
        override fun run() {
            viewModel.checkBindingStatus(account)
            handler.postDelayed(this, 30000) // 每30秒刷新一次
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWaitingBindingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        account = intent.getStringExtra("account") ?: ""
        
        // 观察绑定状态
        viewModel.bindingStatus.observe(this) { status ->
            when(status) {
                "active" -> {
                    Toast.makeText(this, "医生已确认，绑定成功！", Toast.LENGTH_SHORT).show()
                    handler.removeCallbacks(refreshRunnable)
                    // 跳转到主页
                    val intent = Intent(this, PtMainActivity::class.java)
                    intent.putExtra("account", account)
                    startActivity(intent)
                    finish()
                }
                "rejected" -> {
                    Toast.makeText(this, "医生已拒绝您的申请", Toast.LENGTH_SHORT).show()
                    handler.removeCallbacks(refreshRunnable)
                    finish()
                }
                "pending" -> {
                    // 仍在等待
                }
            }
        }
        
        // 刷新按钮
        binding.btnRefresh.setOnClickListener {
            viewModel.checkBindingStatus(account)
            Toast.makeText(this, "正在刷新状态...", Toast.LENGTH_SHORT).show()
        }
        
        // 启动定时刷新
        handler.post(refreshRunnable)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(refreshRunnable)
    }
}



