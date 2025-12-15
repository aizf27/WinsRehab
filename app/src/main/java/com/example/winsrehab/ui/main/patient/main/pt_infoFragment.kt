package com.example.winsrehab.ui.main.patient.main

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.example.winsrehab.data.entity.Patient
import com.example.winsrehab.databinding.FragmentPtInfoBinding
import com.example.winsrehab.ui.main.patient.info.PtInfoVM

class pt_infoFragment : Fragment() {

    private var _binding: FragmentPtInfoBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PtInfoVM by viewModels()
    private val args by navArgs<pt_infoFragmentArgs>()
    private var account: String = ""
    private val mode: String = "patient" // 患者端固定为patient模式

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        account = args.account
        Log.d("pt_infoFragment", "account: $account")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPtInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //观察患者数据
        viewModel.patient.observe(viewLifecycleOwner) { patient ->
            patient?.let { fillUI(it) }
        }

        //设置UI模式（患者端只能编辑基础信息）
        setModeUI(mode)

        //加载患者信息
        viewModel.loadPatient(account)

        //头像按钮点击事件（占位，待实现）
        binding.btnAvatar.setOnClickListener {
            Toast.makeText(requireContext(), "更换头像功能待实现", Toast.LENGTH_SHORT).show()
        }

        //保存按钮点击事件
        binding.btnSave.setOnClickListener { saveInfo() }
    }

    private fun fillUI(p: Patient) = with(binding) {
        etName.setText(p.name ?: "")
        etGender.setText(p.gender ?: "")
        etAge.setText(if (p.age > 0) p.age.toString() else "")
        etDoctorName.setText(p.physicianName ?: "")
        etDoctorCode.setText(p.physicianCode ?: "")
        etDiagnosis.setText(p.diagnosis ?: "")
        etStage.setText(p.stage ?: "")
        etProgress.setText(if (p.progress > 0) p.progress.toString() else "")
        etAiResult.setText(p.aiResult ?: "")
        etLastTraining.setText(p.lastTrainingDate ?: "")
    }

    private fun saveInfo() {
        val name = binding.etName.text.toString().trim()
        val gender = binding.etGender.text.toString().trim()
        val age = binding.etAge.text.toString().toIntOrNull() ?: 0
        val doctor = binding.etDoctorName.text.toString().trim()
        val code = binding.etDoctorCode.text.toString().trim()

        if (name.isEmpty() || gender.isEmpty() || age == 0) {
            Toast.makeText(requireContext(), "请填写完整的基础信息", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.saveBasicInfo(account, name, gender, age, doctor, code)

        Toast.makeText(requireContext(), "信息保存成功", Toast.LENGTH_SHORT).show()

        //通知主Activity更新顶部栏信息
        val result = Bundle()
        result.putBoolean("patient_info_updated", true)
        parentFragmentManager.setFragmentResult("patient_info_result", result)
    }

    private fun setModeUI(mode: String) = with(binding) {
        val patientEditable = (mode == "patient")
        // 患者端只能编辑基础信息
        etName.isEnabled = patientEditable
        etGender.isEnabled = patientEditable
        etAge.isEnabled = patientEditable
        etDoctorName.isEnabled = patientEditable
        etDoctorCode.isEnabled = patientEditable

        // 康复信息只读
        etDiagnosis.isEnabled = false
        etStage.isEnabled = false
        etProgress.isEnabled = false
        etAiResult.isEnabled = false
        etLastTraining.isEnabled = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}