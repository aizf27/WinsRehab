package com.example.winsrehab.ui.main.patient.info

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.winsrehab.data.entity.Patient
import com.example.winsrehab.databinding.ActivityPtInfoBinding
import com.example.winsrehab.ui.main.patient.home.PtHomeActivity

class PtInfoActivity: AppCompatActivity () {
    private lateinit var binding : ActivityPtInfoBinding
    private val viewModel: PtInfoVM by viewModels()
    private var account: String = ""
    private var mode:String = ""
   // private var doctorCode: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPtInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        account=getIntent().getStringExtra("account")?:return
        mode=getIntent().getStringExtra("mode")?:return
        //doctorCode = intent.getStringExtra("doctorCode")


        viewModel.patient.observe(this) { patient ->
            patient?.let { fillUI(it) }
        }
        setModeUI(mode)
        viewModel.loadPatient(account)

        binding.btnSave.setOnClickListener { saveInfo() }

    }
    private fun fillUI(p: Patient) = with(binding) {
        etName.setText(p.name)
        etGender.setText(p.gender)
        etAge.setText(p.age.toString())
        etDoctorName.setText(p.physicianName)
        etDoctorCode.setText(p.physicianCode)

        etDiagnosis.setText(p.diagnosis)
        etStage.setText(p.stage)
        etProgress.setText(p.progress.toString())
        etAiResult.setText(p.aiResult)
        etLastTraining.setText(p.lastTrainingDate)
    }

    private fun saveInfo() {
        if (mode == "patient") {
            val name = binding.etName.text.toString().trim()
            val gender = binding.etGender.text.toString().trim()
            val age = binding.etAge.text.toString().toIntOrNull() ?: 0
            val doctor = binding.etDoctorName.text.toString().trim()
            val code = binding.etDoctorCode.text.toString().trim()

            viewModel.saveBasicInfo(account, name, gender, age, doctor, code)
        } else {
            val diagnosis = binding.etDiagnosis.text.toString().trim()
            val stage = binding.etStage.text.toString().trim()
            val progress = binding.etProgress.text.toString().toIntOrNull() ?: 0
            val aiResult = binding.etAiResult.text.toString().trim()
            val lastTraining = binding.etLastTraining.text.toString().trim()

            viewModel.saveRehabInfo(account, diagnosis, stage, progress, aiResult, lastTraining)
        }
        Toast.makeText(this, "信息保存成功", Toast.LENGTH_SHORT).show()
        if (mode == "patient"){
            val intent: Intent = Intent(this@PtInfoActivity, PtHomeActivity::class.java)
            intent.putExtra("account", account)
            startActivity(intent)
            finish()
        }

    }

    private fun setModeUI(mode: String) = with(binding) {
        val patientEditable = (mode == "patient")
        etName.isEnabled = patientEditable
        etGender.isEnabled = patientEditable
        etAge.isEnabled = patientEditable
        etDoctorName.isEnabled = patientEditable
        etDoctorCode.isEnabled = patientEditable

        val doctorEditable = (mode == "doctor")
        etDiagnosis.isEnabled = doctorEditable
        etStage.isEnabled = doctorEditable
        etProgress.isEnabled = doctorEditable
        etAiResult.isEnabled = doctorEditable
        etLastTraining.isEnabled = doctorEditable
    }
}