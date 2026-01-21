package com.example.winsrehab.ui.main.doctor.application

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.winsrehab.data.entity.DoctorTask
import com.example.winsrehab.databinding.ItemPatientApplicationBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ApplicationAdapter(
    private val onConfirm: (DoctorTask) -> Unit,
    private val onReject: (DoctorTask) -> Unit
) : ListAdapter<DoctorTask, ApplicationAdapter.ViewHolder>(DiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPatientApplicationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class ViewHolder(
        private val binding: ItemPatientApplicationBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(task: DoctorTask) {
            binding.tvPatientName.text = task.patientName
            binding.tvPatientAccount.text = "账号：${task.patientAccount}"
            binding.tvApplyTime.text = "申请时间：${formatTime(task.createdAt)}"
            
            binding.btnReject.setOnClickListener {
                onReject(task)
            }
            
            binding.btnConfirm.setOnClickListener {
                onConfirm(task)
            }
        }
        
        private fun formatTime(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp
            
            return when {
                diff < 60 * 1000 -> "刚刚"
                diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}分钟前"
                diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}小时前"
                else -> {
                    val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
                    sdf.format(Date(timestamp))
                }
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



