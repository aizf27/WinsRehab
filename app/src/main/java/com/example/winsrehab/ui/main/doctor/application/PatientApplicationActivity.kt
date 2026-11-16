package com.example.winsrehab.ui.main.doctor.application

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.winsrehab.R
import com.example.winsrehab.databinding.ActivityPatientApplicationBinding

// 患者申请页：顶部栏 + 申请卡片列表，确认/拒绝后列表自动刷新，空列表显示空态
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

        setupTopBar()
        setupList()
        observeData()
    }

    // 顶部栏自己画：标题固定，返回按钮关闭本页（页面已不挂 ActionBar 主题）
    private fun setupTopBar() {
        binding.topBar.tvTopBarTitle.setText(R.string.patient_application_title)
        binding.topBar.btnTopBarBack.setOnClickListener { finish() }
        binding.stateEmpty.tvStateEmpty.setText(R.string.patient_application_empty)
    }

    // 列表与两个操作回调
    private fun setupList() {
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
    }

    // 申请列表与操作结果
    private fun observeData() {
        viewModel.getPendingApplications(doctorCode)
        viewModel.applications.observe(this) { tasks ->
            adapter.submitList(tasks)
            // 有数据看列表，没数据看空态
            val empty = tasks.isEmpty()
            binding.stateEmpty.root.visibility = if (empty) View.VISIBLE else View.GONE
            binding.recyclerView.visibility = if (empty) View.GONE else View.VISIBLE
        }

        viewModel.operationResult.observe(this) { result ->
            val message = when (result) {
                "confirm_success" -> R.string.toast_confirm_success
                "reject_success" -> R.string.toast_reject_success
                else -> R.string.toast_operation_failed
            }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }
}
