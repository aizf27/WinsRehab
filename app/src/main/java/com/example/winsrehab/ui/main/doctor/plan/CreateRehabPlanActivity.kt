package com.example.winsrehab.ui.main.doctor.plan

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.winsrehab.R
import com.example.winsrehab.databinding.ActivityCreateRehabPlanBinding
import com.example.winsrehab.data.entity.TrainingTask
import com.example.winsrehab.ui.main.doctor.adapter.TrainingTaskAdapter
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.*

/**
 * 创建康复计划Activity
 */
class CreateRehabPlanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateRehabPlanBinding
    private val viewModel: CreateRehabPlanViewModel by viewModels()
    private lateinit var taskAdapter: TrainingTaskAdapter
    private var selectedStartDate: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateRehabPlanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val patientId = intent.getStringExtra("patientId") ?: ""
        if (patientId.isEmpty()) {
            Toast.makeText(this, "患者信息错误", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupViews()
        setupObservers()
        viewModel.loadPatient(patientId)
    }

    private fun setupViews() {
        // 返回按钮
        binding.btnBack.setOnClickListener {
            finish()
        }

        // 任务列表
        taskAdapter = TrainingTaskAdapter { task ->
            showDeleteTaskDialog(task)
        }
        binding.rvTasks.layoutManager = LinearLayoutManager(this)
        binding.rvTasks.adapter = taskAdapter

        // 添加任务按钮
        binding.btnAddTask.setOnClickListener {
            showAddTaskDialog()
        }

        // 选择开始日期
        binding.tvStartDate.setOnClickListener {
            showDatePicker()
        }

        // 保存草稿
        binding.btnSaveDraft.setOnClickListener {
            savePlan(isDraft = true)
        }

        // 激活计划
        binding.btnActivatePlan.setOnClickListener {
            savePlan(isDraft = false)
        }
    }

    private fun setupObservers() {
        // 观察患者信息
        viewModel.patient.observe(this) { patient ->
            patient?.let {
                binding.tvPatientName.text = it.name
                binding.tvPatientDiagnosis.text = it.diagnosis
            }
        }

        // 观察任务列表
        viewModel.tasks.observe(this) { tasks ->
            taskAdapter.submitList(tasks)
            if (tasks.isEmpty()) {
                binding.emptyTasksLayout.visibility = View.VISIBLE
                binding.rvTasks.visibility = View.GONE
            } else {
                binding.emptyTasksLayout.visibility = View.GONE
                binding.rvTasks.visibility = View.VISIBLE
            }
        }

        // 观察保存结果
        viewModel.saveResult.observe(this) { result ->
            result.onSuccess {
                Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show()
                finish()
            }.onFailure { e ->
                Toast.makeText(this, "保存失败：${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val dialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                calendar.set(year, month, dayOfMonth)
                selectedStartDate = sdf.format(calendar.time)
                binding.tvStartDate.text = selectedStartDate
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        // 即时反馈的第二道：开始日不能早于今天；权威判定仍在保存时
        dialog.datePicker.minDate = System.currentTimeMillis()
        dialog.show()
    }

    private fun showAddTaskDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_task, null)
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(dialogView)

        val etTaskName = dialogView.findViewById<android.widget.EditText>(R.id.et_task_name)
        val etTaskDescription = dialogView.findViewById<android.widget.EditText>(R.id.et_task_description)
        val spinnerTaskType = dialogView.findViewById<android.widget.Spinner>(R.id.spinner_task_type)
        val etTaskDuration = dialogView.findViewById<android.widget.EditText>(R.id.et_task_duration)
        val rgFrequency = dialogView.findViewById<android.widget.RadioGroup>(R.id.rg_frequency)
        val layoutFrequencyDetail = dialogView.findViewById<android.view.ViewGroup>(R.id.layout_frequency_detail)
        val layoutWeeklySelection = dialogView.findViewById<android.view.ViewGroup>(R.id.layout_weekly_selection)
        val layoutIntervalInput = dialogView.findViewById<android.view.ViewGroup>(R.id.layout_interval_input)
        val rgPriority = dialogView.findViewById<android.widget.RadioGroup>(R.id.rg_priority)
        val btnCancel = dialogView.findViewById<android.widget.Button>(R.id.btn_cancel)
        val btnConfirm = dialogView.findViewById<android.widget.Button>(R.id.btn_confirm)

        // 设置任务类型下拉列表
        val taskTypes = arrayOf("上肢训练", "下肢训练", "平衡训练", "协调训练", "全身训练")
        val taskTypeValues = arrayOf("upper_limb", "lower_limb", "balance", "coordination", "full_body")
        spinnerTaskType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, taskTypes)

        // 频率选择监听
        rgFrequency.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rb_daily -> {
                    layoutFrequencyDetail.visibility = View.GONE
                }
                R.id.rb_weekly -> {
                    layoutFrequencyDetail.visibility = View.VISIBLE
                    layoutWeeklySelection.visibility = View.VISIBLE
                    layoutIntervalInput.visibility = View.GONE
                }
                R.id.rb_interval -> {
                    layoutFrequencyDetail.visibility = View.VISIBLE
                    layoutWeeklySelection.visibility = View.GONE
                    layoutIntervalInput.visibility = View.VISIBLE
                }
            }
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnConfirm.setOnClickListener {
            val taskName = etTaskName.text.toString().trim()
            val taskDescription = etTaskDescription.text.toString().trim()
            val taskTypeIndex = spinnerTaskType.selectedItemPosition
            val taskType = taskTypeValues[taskTypeIndex]
            val durationStr = etTaskDuration.text.toString().trim()

            if (taskName.isEmpty()) {
                Toast.makeText(this, "请输入任务名称", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (durationStr.isEmpty()) {
                Toast.makeText(this, "请输入建议时长", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val duration = durationStr.toIntOrNull() ?: 0
            if (duration <= 0) {
                Toast.makeText(this, "请输入有效的时长", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 获取频率信息
            val (frequency, frequencyDetail) = when (rgFrequency.checkedRadioButtonId) {
                R.id.rb_daily -> "daily" to ""
                R.id.rb_weekly -> {
                    val days = mutableListOf<String>()
                    if (dialogView.findViewById<android.widget.CheckBox>(R.id.cb_monday).isChecked) days.add("1")
                    if (dialogView.findViewById<android.widget.CheckBox>(R.id.cb_tuesday).isChecked) days.add("2")
                    if (dialogView.findViewById<android.widget.CheckBox>(R.id.cb_wednesday).isChecked) days.add("3")
                    if (dialogView.findViewById<android.widget.CheckBox>(R.id.cb_thursday).isChecked) days.add("4")
                    if (dialogView.findViewById<android.widget.CheckBox>(R.id.cb_friday).isChecked) days.add("5")
                    if (dialogView.findViewById<android.widget.CheckBox>(R.id.cb_saturday).isChecked) days.add("6")
                    if (dialogView.findViewById<android.widget.CheckBox>(R.id.cb_sunday).isChecked) days.add("7")
                    
                    if (days.isEmpty()) {
                        Toast.makeText(this, "请至少选择一天", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    "weekly" to days.joinToString(",")
                }
                R.id.rb_interval -> {
                    val intervalDays = dialogView.findViewById<android.widget.EditText>(R.id.et_interval_days)
                        .text.toString().trim()
                    if (intervalDays.isEmpty()) {
                        Toast.makeText(this, "请输入间隔天数", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    "interval" to intervalDays
                }
                else -> "daily" to ""
            }

            // 获取优先级
            val priority = when (rgPriority.checkedRadioButtonId) {
                R.id.rb_important -> "important"
                else -> "normal"
            }

            // 创建任务
            val task = TrainingTask(
                planId = 0, // 临时值，保存时会更新
                taskName = taskName,
                taskType = taskType,
                description = taskDescription,
                suggestedDuration = duration,
                frequency = frequency,
                frequencyDetail = frequencyDetail,
                priority = priority
            )

            viewModel.addTask(task)
            dialog.dismiss()
            Toast.makeText(this, "任务已添加", Toast.LENGTH_SHORT).show()
        }

        dialog.show()
    }

    private fun showDeleteTaskDialog(task: TrainingTask) {
        MaterialAlertDialogBuilder(this)
            .setTitle("删除任务")
            .setMessage("确定要删除任务${task.taskName}吗？")
            .setPositiveButton("删除") { _, _ ->
                viewModel.removeTask(task)
                Toast.makeText(this, "任务已删除", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun savePlan(isDraft: Boolean) {
        val planName = binding.etPlanName.text.toString().trim()
        val goal = binding.etPlanGoal.text.toString().trim()
        val durationStr = binding.etPlanDuration.text.toString().trim()
        val durationDays = durationStr.toIntOrNull() ?: 0

        viewModel.savePlan(planName, goal, selectedStartDate, durationDays, isDraft)
    }
}



