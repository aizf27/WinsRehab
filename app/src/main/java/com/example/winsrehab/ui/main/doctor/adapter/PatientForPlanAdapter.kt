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
import com.example.winsrehab.data.entity.Patient

/**
 * 患者列表适配器 - 用于选择患者创建康复计划
 */
class PatientForPlanAdapter(
    private val onItemClick: (Patient) -> Unit
) : ListAdapter<Patient, PatientForPlanAdapter.ViewHolder>(DiffCallback) {

    object DiffCallback : DiffUtil.ItemCallback<Patient>() {
        override fun areItemsTheSame(oldItem: Patient, newItem: Patient): Boolean =
            oldItem.patientId == newItem.patientId

        override fun areContentsTheSame(oldItem: Patient, newItem: Patient): Boolean =
            oldItem == newItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_patient_for_plan, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val patient = getItem(position)
        holder.bind(patient)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvPatientName: TextView = itemView.findViewById(R.id.tv_patient_name)
        private val tvPatientInfo: TextView = itemView.findViewById(R.id.tv_patient_info)
        private val tvRehabStage: TextView = itemView.findViewById(R.id.tv_rehab_stage)

        fun bind(patient: Patient) {
            tvPatientName.text = patient.name
            tvPatientInfo.text = patient.diagnosis
            tvRehabStage.text = "康复阶段：${patient.rehabStage}"

            itemView.setOnClickListener {
                onItemClick(patient)
            }
        }
    }
}

