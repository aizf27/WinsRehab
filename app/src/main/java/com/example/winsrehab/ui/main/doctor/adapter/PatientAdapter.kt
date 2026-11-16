package com.example.winsrehab.ui.main.doctor.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.winsrehab.R
import com.example.winsrehab.data.entity.Patient
import com.example.winsrehab.databinding.ItemKeyPatientBinding
import com.example.winsrehab.ui.common.PatientStageStyle
import com.example.winsrehab.ui.common.StatusBadge

// 患者列表适配器：视图绑定装配真实字段，不再有写死的阶段与进度
class PatientAdapter(
    private val onItemClick: (Patient) -> Unit = {}   // 点击回调，可选
) : ListAdapter<Patient, PatientAdapter.PatientViewHolder>(DiffCallback) {

    //高效对比新旧列表，只更新发生变化的 item
    object DiffCallback : DiffUtil.ItemCallback<Patient>() {
        override fun areItemsTheSame(oldItem: Patient, newItem: Patient): Boolean =
            oldItem.patientId == newItem.patientId     //以主键 id 判断

        override fun areContentsTheSame(oldItem: Patient, newItem: Patient): Boolean =
            oldItem == newItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatientViewHolder {
        val binding = ItemKeyPatientBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PatientViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PatientViewHolder, position: Int) {
        val patient = getItem(position)
        holder.bind(patient)
        holder.itemView.setOnClickListener { onItemClick(patient) }
    }

    class PatientViewHolder(
        private val binding: ItemKeyPatientBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        // 一次绑定六个真实字段：姓名、诊断、阶段徽标、进度、进度文案、异常提醒
        fun bind(patient: Patient) {
            binding.tvName.text = patient.name
            binding.tvDiagnosis.text = patient.diagnosis
            StatusBadge.bindStage(binding.badgeStage.tvStatusBadge, patient.rehabStage)
            binding.progressTraining.progress = patient.overallProgress
            binding.tvProgressValue.text = binding.root.context.getString(
                R.string.patient_card_progress_value,
                patient.overallProgress
            )
            binding.imgAlert.visibility = if (patient.hasAlert) View.VISIBLE else View.GONE
        }
    }
}
