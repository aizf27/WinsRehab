package com.example.winsrehab.ui.main.patient.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.winsrehab.databinding.ItemTaskInstanceBinding
import com.example.winsrehab.data.model.TaskInstanceWithTask
import com.example.winsrehab.ui.common.StatusBadge
import com.example.winsrehab.ui.common.TaskStatusStyle
import com.google.android.material.button.MaterialButton

/**
 * 今日任务列表适配器：只做赋值，状态视觉由 ui/common 的状态映射决定
 */
class TaskInstanceAdapter(
    private val onStartClick: (TaskInstanceWithTask) -> Unit
) : ListAdapter<TaskInstanceWithTask, TaskInstanceAdapter.Holder>(Diff) {

    object Diff : DiffUtil.ItemCallback<TaskInstanceWithTask>() {
        override fun areItemsTheSame(oldItem: TaskInstanceWithTask, newItem: TaskInstanceWithTask): Boolean =
            oldItem.instance.instanceId == newItem.instance.instanceId

        override fun areContentsTheSame(oldItem: TaskInstanceWithTask, newItem: TaskInstanceWithTask): Boolean =
            oldItem == newItem
    }

    inner class Holder(val binding: ItemTaskInstanceBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(ItemTaskInstanceBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = getItem(position)
        val context = holder.binding.root.context

        holder.binding.tvTaskName.text = item.taskName
        holder.binding.tvDuration.text = "${item.suggestedDuration} 分钟"
        holder.binding.tvPriority.text = if (item.priority == "important") "重要" else "普通"
        holder.binding.tvPriority.visibility = if (item.priority == "important") android.view.View.VISIBLE else android.view.View.GONE

        // 状态条与徽标都只喂状态值，配色全部来自 ui/common 的同一张映射表
        StatusBadge.bindTask(holder.binding.statusBadge.tvStatusBadge, item.instance.status)
        val visual = TaskStatusStyle.visualOf(item.instance.status)
        holder.binding.statusStrip.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(context, TaskStatusStyle.colorOf(visual)))

        val actionable = TaskStatusStyle.isActionable(item.instance.status)
        holder.binding.btnStartTask.visibility = if (actionable) android.view.View.VISIBLE else android.view.View.GONE
        holder.binding.btnStartTask.setOnClickListener {
            if (actionable) onStartClick(item)
        }
    }
}
