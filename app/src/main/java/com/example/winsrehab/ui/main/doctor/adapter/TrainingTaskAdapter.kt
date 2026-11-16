package com.example.winsrehab.ui.main.doctor.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.winsrehab.data.entity.TrainingTask
import com.example.winsrehab.databinding.ItemTrainingTaskBinding

// 训练任务适配器：视图绑定取控件，频率文案在适配器内统一格式化
class TrainingTaskAdapter(
    private val onDeleteClick: (TrainingTask) -> Unit
) : ListAdapter<TrainingTask, TrainingTaskAdapter.ViewHolder>(DiffCallback) {

    object DiffCallback : DiffUtil.ItemCallback<TrainingTask>() {
        override fun areItemsTheSame(oldItem: TrainingTask, newItem: TrainingTask): Boolean =
            oldItem.taskId == newItem.taskId

        override fun areContentsTheSame(oldItem: TrainingTask, newItem: TrainingTask): Boolean =
            oldItem == newItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTrainingTaskBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemTrainingTaskBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(task: TrainingTask) {
            binding.tvTaskName.text = task.taskName
            binding.tvTaskDescription.text = task.description
            binding.tvTaskDuration.text = "${task.suggestedDuration}分钟"
            binding.tvTaskFrequency.text = frequencyText(task)
            binding.btnDeleteTask.setOnClickListener { onDeleteClick(task) }
        }

        // 频率文案：每天 / 每周一、三、五 / 每隔 N 天
        private fun frequencyText(task: TrainingTask): String = when (task.frequency) {
            "daily" -> "每天"
            "weekly" -> "每周${weekdayLabels(task.frequencyDetail)}"
            "interval" -> "每隔${task.frequencyDetail}天"
            else -> "未设置"
        }

        // 1..7 映射成中文星期，多个值用顿号连接
        private fun weekdayLabels(detail: String): String {
            val names = arrayOf("一", "二", "三", "四", "五", "六", "日")
            return detail.split(",").mapNotNull { raw ->
                raw.trim().toIntOrNull()?.let { names.getOrNull(it - 1) }
            }.joinToString("、")
        }
    }
}
