package com.example.winsrehab.ui.main.doctor.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.winsrehab.R
import com.example.winsrehab.data.entity.DoctorTask
import com.example.winsrehab.databinding.ItemHomeApplicationBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 医生首页的最新患者申请列表：只展示患者与申请时间，点击整条进入患者申请页
class HomeApplicationAdapter(
    private val onClick: (DoctorTask) -> Unit
) : ListAdapter<DoctorTask, HomeApplicationAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHomeApplicationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemHomeApplicationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(task: DoctorTask) {
            val context = binding.root.context
            binding.tvApplicationName.text = task.patientName
            binding.tvApplicationAccount.text =
                context.getString(R.string.patient_application_account, task.patientAccount)
            binding.tvApplicationTime.text =
                context.getString(R.string.patient_application_time, formatTime(task.createdAt))

            // 整条卡片可点，统一进患者申请页处理，不在首页做确认/拒绝
            binding.root.setOnClickListener { onClick(task) }
        }

        private fun formatTime(timestamp: Long): String {
            val diff = System.currentTimeMillis() - timestamp
            return when {
                diff < 60 * 1000 -> "刚刚"
                diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}分钟前"
                diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}小时前"
                else -> SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
                    .format(Date(timestamp))
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<DoctorTask>() {
        override fun areItemsTheSame(oldItem: DoctorTask, newItem: DoctorTask): Boolean {
            return oldItem.taskId == newItem.taskId
        }

        override fun areContentsTheSame(oldItem: DoctorTask, newItem: DoctorTask): Boolean {
            return oldItem == newItem
        }
    }
}
