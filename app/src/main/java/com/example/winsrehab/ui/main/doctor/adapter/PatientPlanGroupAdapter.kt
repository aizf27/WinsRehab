package com.example.winsrehab.ui.main.doctor.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.winsrehab.data.entity.Patient
import com.example.winsrehab.data.entity.RehabPlan
import com.example.winsrehab.databinding.ItemPatientPlanGroupBinding

// 患者计划分组适配器：按患者分组，组内计划可展开，头部可进创建计划页
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
        val binding = ItemPatientPlanGroupBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(
        private val binding: ItemPatientPlanGroupBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        // 组内复用计划条目适配器
        private val planAdapter = RehabPlanAdapter { plan -> onPlanClick(plan) }

        init {
            binding.rvPlans.layoutManager = LinearLayoutManager(binding.root.context)
            binding.rvPlans.adapter = planAdapter
        }

        fun bind(item: PatientPlanGroup) {
            binding.tvPatientName.text = item.patient.name
            binding.tvPlanCount.text = "${item.plans.size}个康复计划"

            // 展开状态直接映射到组内列表与箭头朝向
            binding.rvPlans.visibility = if (item.isExpanded) View.VISIBLE else View.GONE
            binding.ivExpand.rotation = if (item.isExpanded) 90f else 0f
            if (item.isExpanded) planAdapter.submitList(item.plans)

            // 点头部展开 / 收起
            binding.patientHeader.setOnClickListener {
                item.isExpanded = !item.isExpanded
                notifyItemChanged(adapterPosition)
            }

            // 分组头部的「+」直接给该患者建计划
            binding.btnCreatePlan.setOnClickListener {
                onCreatePlanClick(item.patient)
            }
        }
    }
}
