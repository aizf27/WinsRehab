package com.example.winsrehab.ui.main.doctor.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.winsrehab.R
import com.example.winsrehab.data.entity.TrainingTask

/**
 * 训练任务适配器
 */
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
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_training_task, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val task = getItem(position)
        holder.bind(task)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTaskName: TextView = itemView.findViewById(R.id.tv_task_name)
        private val tvTaskDescription: TextView = itemView.findViewById(R.id.tv_task_description)
        private val tvTaskDuration: TextView = itemView.findViewById(R.id.tv_task_duration)
        private val tvTaskFrequency: TextView = itemView.findViewById(R.id.tv_task_frequency)
        private val btnDelete: ImageView = itemView.findViewById(R.id.btn_delete_task)

        fun bind(task: TrainingTask) {
            tvTaskName.text = task.taskName
            tvTaskDescription.text = task.description
            tvTaskDuration.text = "${task.suggestedDuration}分钟"
            
            // 格式化频率显示
            tvTaskFrequency.text = when (task.frequency) {
                "daily" -> "每天"
                "weekly" -> {
                    val days = task.frequencyDetail.split(",").map { 
                        when(it) {
                            "1" -> "一"
                            "2" -> "二"
                            "3" -> "三"
                            "4" -> "四"
                            "5" -> "五"
                            "6" -> "六"
                            "7" -> "日"
                            else -> ""
                        }
                    }.joinToString("、")
                    "每周$days"
                }
                "interval" -> "每隔${task.frequencyDetail}天"
                else -> "未设置"
            }

            btnDelete.setOnClickListener {
                onDeleteClick(task)
            }
        }
    }
}



