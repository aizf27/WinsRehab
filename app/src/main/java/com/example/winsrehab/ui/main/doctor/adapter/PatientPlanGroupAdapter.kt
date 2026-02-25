package com.example.winsrehab.ui.main.doctor.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.winsrehab.R
import com.example.winsrehab.data.entity.Patient
import com.example.winsrehab.data.entity.RehabPlan

/**
 * 患者计划分组适配器 - 按患者分组显示康复计划
 */
class PatientPlanGroupAdapter(
    private val onCreatePlanClick: (Patient) -> Unit,
    private val onPlanClick: (RehabPlan) -> Unit
) : RecyclerView.Adapter<PatientPlanGroupAdapter.ViewHolder>() {

    private val items = mutableListOf<PatientPlanGroup>()

    data class PatientPlanGroup(
        val patient: Patient,
        val plans: List<RehabPlan>,
        var isExpanded: Boolean = false
    )

    fun submitList(list: List<PatientPlanGroup>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_patient_plan_group, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvPatientName: TextView = itemView.findViewById(R.id.tv_patient_name)
        private val tvPlanCount: TextView = itemView.findViewById(R.id.tv_plan_count)
        private val btnCreatePlan: ImageView = itemView.findViewById(R.id.btn_create_plan)
        private val ivExpand: ImageView = itemView.findViewById(R.id.iv_expand)
        private val rvPlans: RecyclerView = itemView.findViewById(R.id.rv_plans)
        private val patientHeader: View = itemView.findViewById(R.id.patient_header)

        private val planAdapter = RehabPlanAdapter { plan ->
            onPlanClick(plan)
        }

        init {
            rvPlans.layoutManager = LinearLayoutManager(itemView.context)
            rvPlans.adapter = planAdapter
        }

        fun bind(item: PatientPlanGroup) {
            tvPatientName.text = item.patient.name
            tvPlanCount.text = "${item.plans.size}个康复计划"

            // 设置展开/收起状态
            if (item.isExpanded) {
                rvPlans.visibility = View.VISIBLE
                ivExpand.rotation = 90f
                planAdapter.submitList(item.plans)
            } else {
                rvPlans.visibility = View.GONE
                ivExpand.rotation = 0f
            }

            // 点击头部展开/收起
            patientHeader.setOnClickListener {
                item.isExpanded = !item.isExpanded
                notifyItemChanged(adapterPosition)
            }

            // 点击创建计划按钮
            btnCreatePlan.setOnClickListener {
                onCreatePlanClick(item.patient)
            }
        }
    }
}

