package com.example.winsrehab.ui.main.patient.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.winsrehab.R
import com.example.winsrehab.data.model.TaskInstanceWithTask
import com.example.winsrehab.ui.main.patient.main.TaskStatusStyle
import com.example.winsrehab.ui.main.patient.main.TaskVisual

/**
 * 今日任务列表适配器：只做赋值，状态视觉由 TaskStatusStyle 决定
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

    inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val imgStatus: ImageView = view.findViewById(R.id.imgStatus)
        val tvTaskName: TextView = view.findViewById(R.id.tvTaskName)
        val tvDuration: TextView = view.findViewById(R.id.tvDuration)
        val tvPriority: TextView = view.findViewById(R.id.tvPriority)
        val btnStart: TextView = view.findViewById(R.id.btnStartTask)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task_instance, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = getItem(position)
        val context = holder.itemView.context

        holder.tvTaskName.text = item.taskName
        holder.tvDuration.text = "${item.suggestedDuration} 分钟"
        holder.tvPriority.text = if (item.priority == "important") "重要" else "普通"
        holder.tvPriority.visibility = if (item.priority == "important") View.VISIBLE else View.GONE

        val visual = TaskStatusStyle.visualOf(item.instance.status)
        holder.imgStatus.setImageResource(
            if (visual == TaskVisual.COMPLETED) R.drawable.ic_check_circle else R.drawable.ic_circle_outline
        )
        holder.imgStatus.setColorFilter(
            ContextCompat.getColor(
                context,
                if (visual == TaskVisual.COMPLETED) R.color.status_completed else R.color.on_surface_tertiary
            )
        )

        val actionable = TaskStatusStyle.isActionable(item.instance.status)
        holder.btnStart.visibility = if (actionable) View.VISIBLE else View.GONE
        holder.btnStart.setOnClickListener {
            if (actionable) onStartClick(item)
        }
    }
}
