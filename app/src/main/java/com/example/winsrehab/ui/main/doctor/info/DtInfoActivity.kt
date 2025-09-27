package com.example.winsrehab.ui.main.doctor.info

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.winsrehab.data.entity.Doctor
import com.example.winsrehab.databinding.ActivityDtInfoBinding
import com.example.winsrehab.ui.main.doctor.home.DtHomeActivity

class DtInfoActivity : AppCompatActivity() {
    private lateinit var  binding: ActivityDtInfoBinding
    private lateinit var doctorCode: String

    private var patientCount: Int = 0

    private val viewModel: DtInfoVM by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDtInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

            doctorCode = intent.getStringExtra("doctorCode") ?: ""
            patientCount = intent.getIntExtra("totalCount", 0)
        Log.d("DtInfoActivity", "doctorCode: $doctorCode")
        Log.d("DtInfoActivity", "patientCount: $patientCount")

        viewModel.doctor.observe(this) { doctor ->
            if (doctor != null) {
                binding.tvCode.text = "工号：${doctor.id}"
                binding.etName.setText(doctor.name)
                binding.etAge.setText(doctor.age?.toString() ?: "")
                binding.etPtNum.setText(
                    if ( doctor.patientCount == 0)
                    patientCount.toString()
                else
                    doctor.patientCount.toString())
                binding.etGender.setText(doctor.gender)
                binding.etDepartment.setText(doctor.department)
            } else {
                binding.tvCode.text = "工号：$doctorCode"
                binding.etPtNum.setText(
                    if (doctor?.patientCount == 0)
                    patientCount.toString()
                else
                    doctor?.patientCount.toString() )
            }
        }

        viewModel.loadDoctorInfo(doctorCode)

        binding.btnSave.setOnClickListener { saveDoctorInfo() }
    }

    private fun saveDoctorInfo() {
        val name = binding.etName.text.toString().trim()
        val ageStr = binding.etAge.text.toString().trim()
        val ptNumStr = binding.etPtNum.text.toString().trim()
        val gender = binding.etGender.text.toString().trim()
        val department = binding.etDepartment.text.toString().trim()

        if (name.isEmpty() || ageStr.isEmpty() || ptNumStr.isEmpty() ||
            gender.isEmpty() || department.isEmpty()
        ) {
            Toast.makeText(this, "请填写完整信息", Toast.LENGTH_SHORT).show()
            return
        }

        val age = ageStr.toIntOrNull()
        val ptNum = ptNumStr.toIntOrNull()
        if (age == null || ptNum == null) {
            Toast.makeText(this, "年龄和患者数必须是数字", Toast.LENGTH_SHORT).show()
            return
        }
        val oldPassword = viewModel.doctor.value?.password ?: ""
        // 创建医生对象
        val doctor = Doctor(
            id = doctorCode,//别搞错
            password = oldPassword,
            name = name,
            gender = gender,
            age = age,
            department = department,
            patientCount = patientCount
        )

        viewModel.saveDoctorInfo(doctor) { success ->
            if (success) {
                Toast.makeText(this, "信息保存成功", Toast.LENGTH_SHORT).show()
                startActivity(
                    Intent(this, DtHomeActivity::class.java)
                                .putExtra("doctorCode", doctorCode)

                )
                finish()
            } else {
                Toast.makeText(this, "保存失败，请重试", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
