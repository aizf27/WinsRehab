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
    private val mode: String
        get() = args.mode

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        account = resolveAccount()
        Log.d(
            "pt_infoFragment",
            "account: $account, mode: $mode"
        )
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

        //观察患者数据,变化就更新
        viewModel.patient.observe(viewLifecycleOwner) { patient ->
            patient?.let { fillUI(it) }
        }

        //设置UI模式（患者/医生区分可编辑字段）
        setModeUI(mode)

        //拉取患者信息
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
        etSignature.setText(p.signature ?: "")
        etAiResult.setText(p.aiResult ?: "")
        etLastTraining.setText(p.lastTrainingDate ?: "")

    }

    private fun saveInfo() {
        val name = binding.etName.text.toString().trim()
        val gender = binding.etGender.text.toString().trim()
        val age = binding.etAge.text.toString().toIntOrNull() ?: 0
        val doctor = binding.etDoctorName.text.toString().trim()
        val code = binding.etDoctorCode.text.toString().trim()
        val signature = binding.etSignature.text.toString().trim()

        val diagnosis = binding.etDiagnosis.text.toString().trim()
        val stage = binding.etStage.text.toString().trim()
        val progress = binding.etProgress.text.toString().toIntOrNull() ?: 0
        val aiResult = binding.etAiResult.text.toString().trim()
        val lastTraining = binding.etLastTraining.text.toString().trim()



        if (mode == "patient") {
            if (name.isEmpty() || gender.isEmpty() || age == 0) {
                Toast.makeText(requireContext(), "请填写完整的基础信息", Toast.LENGTH_SHORT).show()
                return
            }
            viewModel.saveBasicInfo(account, name, gender, age, doctor, code, signature)
        } else { // doctor
            // 可选校验：诊断或进度
            viewModel.saveRehabInfo(account, diagnosis, stage, progress, aiResult, lastTraining)
        }

        Toast.makeText(requireContext(), "信息保存成功", Toast.LENGTH_SHORT).show()

        //通知主Activity更新顶部栏信息
        val result = Bundle()
        result.putBoolean("patient_info_updated", true)
        result.putString("patient_name", name)
        result.putString("patient_signature", signature)
        Log.d("pt_infoFragment","result:$result")
        // 直接发给宿主 Activity 所在的 FragmentManager，避免 NavHost 嵌套导致收不到
        requireActivity().supportFragmentManager.setFragmentResult("patient_info_result", result)
    }

    private fun setModeUI(mode: String) = with(binding) {
        val patientEditable = (mode == "patient")
        val doctorEditable = (mode == "doctor")

        // 患者：基础信息可写，康复信息只读
        etName.isEnabled = patientEditable
        etGender.isEnabled = patientEditable
        etAge.isEnabled = patientEditable
        etDoctorName.isEnabled = patientEditable
        etDoctorCode.isEnabled = patientEditable
        etSignature.isEnabled = patientEditable

        // 医生：基础信息只读，康复信息可写
        etDiagnosis.isEnabled = doctorEditable
        etStage.isEnabled = doctorEditable
        etProgress.isEnabled = doctorEditable
        etAiResult.isEnabled = doctorEditable
        etLastTraining.isEnabled = doctorEditable
    }


     //BottomNav 未传递 account 时，从宿主 Activity 的 Intent 兜底获取。
    private fun resolveAccount(): String {
        val navAccount = args.account ?: ""
        val intentAccount = requireActivity().intent.getStringExtra("account") ?: ""
        Log.d("pt_infoFragment", "args.account=$navAccount, intent.account=$intentAccount")
        if (navAccount.isNotEmpty()) return navAccount
        return intentAccount
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}