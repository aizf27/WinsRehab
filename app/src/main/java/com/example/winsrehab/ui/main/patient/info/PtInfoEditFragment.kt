package com.example.winsrehab.ui.main.patient.info

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.winsrehab.databinding.FragmentPtInfoEditBinding

/**
 * 患者信息编辑页面
 * 功能：编辑患者基本信息（姓名、性别、年龄、个性签名）
 * 只读：医生信息、康复信息
 */
class PtInfoEditFragment : Fragment() {

    private var _binding: FragmentPtInfoEditBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: PtInfoVM by viewModels()
    private val args by navArgs<PtInfoEditFragmentArgs>()
    private var account: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        account = args.account
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPtInfoEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 加载患者数据
        viewModel.loadPatient(account)
        
        // 观察数据变化
        observeData()
        
        // 设置点击事件
        setupClickListeners()
    }

    /**
     * 观察数据变化，更新UI
     */
    private fun observeData() {
        viewModel.patient.observe(viewLifecycleOwner) { patient ->
            patient?.let {
                // 更新基本信息
                binding.tvName.text = it.name.takeIf { name -> name != "未设置" } ?: "未设置"
                binding.tvGender.text = it.gender.takeIf { gender -> gender != "未设置" } ?: "未设置"
                binding.tvAge.text = if (it.age > 0) "${it.age}岁" else "未设置"
                binding.tvSignature.text = it.signature.takeIf { sig -> sig != "未设置" && sig != "这个人很懒，什么都没留下" } ?: "未设置"
                
                // 更新医生信息（只读）
                binding.tvDoctorName.text = it.doctorName.takeIf { name -> name != "未设置" } ?: "未绑定"
                binding.tvDoctorCode.text = it.doctorCode.takeIf { code -> code != "未设置" } ?: "未绑定"
                
                // 更新康复信息（只读）
                binding.tvDiagnosis.text = it.diagnosis.takeIf { diag -> diag != "未设置" } ?: "由医生填写"
                binding.tvStage.text = it.rehabStage.takeIf { stage -> stage != "未设置" } ?: "由医生评估"
            }
        }
    }

    /**
     * 设置点击事件
     */
    private fun setupClickListeners() {
        // 返回按钮
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
        
        // 保存按钮
        binding.btnSave.setOnClickListener {
            savePatientInfo()
        }
        
        // 头像点击（暂不实现）
        binding.layoutAvatar.setOnClickListener {
            Toast.makeText(requireContext(), "头像上传功能开发中", Toast.LENGTH_SHORT).show()
        }
        
        // 姓名点击
        binding.layoutName.setOnClickListener {
            showEditDialog("姓名", binding.tvName.text.toString()) { newValue ->
                binding.tvName.text = newValue
            }
        }
        
        // 性别点击
        binding.layoutGender.setOnClickListener {
            showGenderDialog()
        }
        
        // 年龄点击
        binding.layoutAge.setOnClickListener {
            showAgeDialog()
        }
        
        // 个性签名点击
        binding.layoutSignature.setOnClickListener {
            showEditDialog("个性签名", binding.tvSignature.text.toString(), maxLength = 100) { newValue ->
                binding.tvSignature.text = newValue
            }
        }
    }

    /**
     * 显示编辑对话框
     */
    private fun showEditDialog(
        title: String,
        currentValue: String,
        maxLength: Int = 20,
        onConfirm: (String) -> Unit
    ) {
        val editText = EditText(requireContext()).apply {
            setText(if (currentValue == "未设置") "" else currentValue)
            hint = "请输入$title"
            maxLines = if (maxLength > 50) 3 else 1
            inputType = if (maxLength > 50) {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            } else {
                InputType.TYPE_CLASS_TEXT
            }
            setPadding(50, 30, 50, 30)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("编辑$title")
            .setView(editText)
            .setPositiveButton("确定") { _, _ ->
                val newValue = editText.text.toString().trim()
                if (newValue.isEmpty()) {
                    Toast.makeText(requireContext(), "${title}不能为空", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (newValue.length > maxLength) {
                    Toast.makeText(requireContext(), "${title}长度不能超过${maxLength}个字符", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                onConfirm(newValue)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 显示性别选择对话框
     */
    private fun showGenderDialog() {
        val genders = arrayOf("男", "女")
        val currentGender = binding.tvGender.text.toString()
        val checkedItem = when (currentGender) {
            "男" -> 0
            "女" -> 1
            else -> -1
        }

        AlertDialog.Builder(requireContext())
            .setTitle("选择性别")
            .setSingleChoiceItems(genders, checkedItem) { dialog, which ->
                binding.tvGender.text = genders[which]
                dialog.dismiss()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 显示年龄输入对话框
     */
    private fun showAgeDialog() {
        val editText = EditText(requireContext()).apply {
            val currentAge = binding.tvAge.text.toString().replace("岁", "")
            setText(if (currentAge == "未设置") "" else currentAge)
            hint = "请输入年龄"
            inputType = InputType.TYPE_CLASS_NUMBER
            setPadding(50, 30, 50, 30)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("编辑年龄")
            .setView(editText)
            .setPositiveButton("确定") { _, _ ->
                val ageStr = editText.text.toString().trim()
                if (ageStr.isEmpty()) {
                    Toast.makeText(requireContext(), "年龄不能为空", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val age = ageStr.toIntOrNull()
                if (age == null || age < 1 || age > 120) {
                    Toast.makeText(requireContext(), "请输入有效的年龄（1-120）", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                binding.tvAge.text = "${age}岁"
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 保存患者信息
     */
    private fun savePatientInfo() {
        val patient = viewModel.patient.value ?: run {
            Toast.makeText(requireContext(), "数据加载失败，请重试", Toast.LENGTH_SHORT).show()
            return
        }

        // 获取界面数据
        val name = binding.tvName.text.toString()
        val gender = binding.tvGender.text.toString()
        val ageStr = binding.tvAge.text.toString().replace("岁", "")
        val signature = binding.tvSignature.text.toString()

        // 验证必填项
        if (name == "未设置" || name.isBlank()) {
            Toast.makeText(requireContext(), "请填写姓名", Toast.LENGTH_SHORT).show()
            return
        }
        if (gender == "未设置" || gender.isBlank()) {
            Toast.makeText(requireContext(), "请选择性别", Toast.LENGTH_SHORT).show()
            return
        }
        if (ageStr == "未设置" || ageStr.isBlank()) {
            Toast.makeText(requireContext(), "请填写年龄", Toast.LENGTH_SHORT).show()
            return
        }

        val age = ageStr.toIntOrNull() ?: 0

        // 调用 ViewModel 保存
        viewModel.saveBasicInfo(
            account = account,
            name = name,
            gender = gender,
            age = age,
            doctor = patient.doctorName,
            doctorCode = patient.doctorCode,
            signature = if (signature == "未设置") "" else signature
        )

        // 显示成功提示
        Toast.makeText(requireContext(), "保存成功", Toast.LENGTH_SHORT).show()

        // 返回上一页
        findNavController().navigateUp()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

