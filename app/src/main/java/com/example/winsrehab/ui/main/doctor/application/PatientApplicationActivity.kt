package com.example.winsrehab.ui.main.doctor.application

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.winsrehab.databinding.ActivityPatientApplicationBinding

class PatientApplicationActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityPatientApplicationBinding
    private val viewModel: PatientApplicationViewModel by viewModels()
    private lateinit var adapter: ApplicationAdapter
    private var doctorCode: String = ""
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPatientApplicationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        doctorCode = intent.getStringExtra("doctorCode") ?: ""
        
        // 设置标题
        supportActionBar?.title = "患者申请列表"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // 初始化 RecyclerView
        adapter = ApplicationAdapter(
            onConfirm = { task ->
                viewModel.confirmPatient(task.patientId, doctorCode, task.taskId)
            },
            onReject = { task ->
                viewModel.rejectPatient(task.patientId, task.taskId)
            }
        )
        
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        
        // 观察申请列表
        viewModel.getPendingApplications(doctorCode)
        viewModel.applications.observe(this) { tasks ->
            adapter.submitList(tasks)
            if (tasks.isEmpty()) {
                binding.tvEmpty.visibility = android.view.View.VISIBLE
                binding.recyclerView.visibility = android.view.View.GONE
            } else {
                binding.tvEmpty.visibility = android.view.View.GONE
                binding.recyclerView.visibility = android.view.View.VISIBLE
            }
        }
        
        // 观察操作结果
        viewModel.operationResult.observe(this) { result ->
            when(result) {
                "confirm_success" -> {
                    Toast.makeText(this, "已确认患者绑定", Toast.LENGTH_SHORT).show()
                }
                "reject_success" -> {
                    Toast.makeText(this, "已拒绝患者申请", Toast.LENGTH_SHORT).show()
                }
                "error" -> {
                    Toast.makeText(this, "操作失败，请重试", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}





