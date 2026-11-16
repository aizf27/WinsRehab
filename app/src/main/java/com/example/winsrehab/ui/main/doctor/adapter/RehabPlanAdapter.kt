package com.example.winsrehab.ui.main.doctor.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.winsrehab.data.entity.RehabPlan
import com.example.winsrehab.databinding.ItemRehabPlanBinding
import com.example.winsrehab.ui.common.StatusBadge

// 康复计划条目适配器：状态徽标走 StatusBadge，页面不再各写一套状态文案与配色
class RehabPlanAdapter(
    private val onItemClick: (RehabPlan) -> Unit
) : ListAdapter<RehabPlan, RehabPlanAdapter.ViewHolder>(DiffCallback) {

    object DiffCallback : DiffUtil.ItemCallback<RehabPlan>() {
        override fun areItemsTheSame(oldItem: RehabPlan, newItem: RehabPlan): Boolean =
            oldItem.planId == newItem.planId

        override fun areContentsTheSame(oldItem: RehabPlan, newItem: RehabPlan): Boolean =
            oldItem == newItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRehabPlanBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemRehabPlanBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(plan: RehabPlan) {
            binding.tvPlanName.text = plan.planName
            binding.tvPlanGoal.text = plan.goal.ifBlank { "--" }
            // 四态徽标：草稿 / 进行中 / 已完成 / 已取消
            StatusBadge.bindPlan(binding.tvPlanStatus, plan.status)
            binding.tvPlanProgress.text = "进度 ${plan.progress}%"
            binding.tvPlanDate.text = "${plan.startDate} 至 ${plan.endDate}"
            binding.root.setOnClickListener { onItemClick(plan) }
        }
    }
}
