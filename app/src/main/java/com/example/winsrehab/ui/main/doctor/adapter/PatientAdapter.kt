package com.example.winsrehab.ui.main.doctor.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.winsrehab.R
import com.example.winsrehab.data.entity.Patient
import com.example.winsrehab.ui.main.patient.info.PtInfoActivity

class PatientAdapter(
    private val onItemClick: (Patient) -> Unit = {}   // 点击回调，可选
) : ListAdapter<Patient, PatientAdapter.PatientViewHolder>(DiffCallback) {

    //高效对比新旧列表，只更新发生变化的 item
    object DiffCallback : DiffUtil.ItemCallback<Patient>() {
        override fun areItemsTheSame(oldItem: Patient, newItem: Patient): Boolean =
            oldItem.id == newItem.id     // 以主键 id 判断
        override fun areContentsTheSame(oldItem: Patient, newItem: Patient): Boolean =
            oldItem == newItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatientViewHolder {
        //把XML布局转成view
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_key_patient, parent, false)
        return PatientViewHolder(view)
    }

    override fun onBindViewHolder(holder: PatientViewHolder, position: Int) {
        //装数据到view
        val patient = getItem(position)
        holder.tvName.text = patient.name
        holder.tvStage.text = "康复阶段：" + (patient.stage ?: "")
        holder.progressTraining.progress = patient.progress
        holder.tvProgressValue.text = "${patient.progress}%"
        holder.tvAiResult.text = patient.aiResult ?: ""
        holder.imgAvatar.setImageResource(R.mipmap.img_mrtx)
        holder.imgAiEval.setImageResource(R.mipmap.img_ai)

        holder.imgAlert.visibility =
            if (patient.hasAlert) View.VISIBLE else View.GONE

        holder.itemView.setOnClickListener {
            onItemClick(patient)

            //直接跳转患者信息页
            val context = holder.itemView.context
            val intent = Intent(context, PtInfoActivity::class.java).apply {
                putExtra("account", patient.account)
                putExtra("mode", "doctor")
            }
            context.startActivity(intent)
        }
    }
        override fun getItemCount(): Int = currentList.size
    class PatientViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        //只在创建时初始化 findViewById
        val tvName: TextView = itemView.findViewById(R.id.tv_name)
        val tvStage: TextView = itemView.findViewById(R.id.tv_stage)
        val tvProgressValue: TextView = itemView.findViewById(R.id.tv_progress_value)
        val tvAiResult: TextView = itemView.findViewById(R.id.tv_ai_result)
        val progressTraining: ProgressBar = itemView.findViewById(R.id.progress_training)
        val imgAlert: ImageView = itemView.findViewById(R.id.img_alert)
        val imgAvatar: ImageView = itemView.findViewById(R.id.img_avatar)
        val imgAiEval: ImageView = itemView.findViewById(R.id.img_ai_eval)
    }
}
