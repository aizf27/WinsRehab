package com.example.winsrehab.ui.main.doctor.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.winsrehab.R
import com.example.winsrehab.data.entity.RehabPlan

/**
 * 康复计划适配器
 */
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
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_rehab_plan, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val plan = getItem(position)
        holder.bind(plan)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvPlanName: TextView = itemView.findViewById(R.id.tv_plan_name)
        private val tvPlanGoal: TextView = itemView.findViewById(R.id.tv_plan_goal)
        private val tvPlanStatus: TextView = itemView.findViewById(R.id.tv_plan_status)
        private val tvPlanProgress: TextView = itemView.findViewById(R.id.tv_plan_progress)
        private val tvPlanDate: TextView = itemView.findViewById(R.id.tv_plan_date)
        private val tvTaskCount: TextView = itemView.findViewById(R.id.tv_task_count)

        fun bind(plan: RehabPlan) {
            tvPlanName.text = plan.planName
            tvPlanGoal.text = plan.goal
            
            // 状态显示
            tvPlanStatus.text = when (plan.status) {
                "draft" -> "草稿"
                "active" -> "进行中"
                "completed" -> "已完成"
                "cancelled" -> "已取消"
                else -> "未知"
            }
            
            tvPlanProgress.text = "进度 ${plan.progress}%"
            tvPlanDate.text = "${plan.startDate} 至 ${plan.endDate}"
            tvTaskCount.text = "查看详情"

            itemView.setOnClickListener {
                onItemClick(plan)
            }
        }
    }
}



