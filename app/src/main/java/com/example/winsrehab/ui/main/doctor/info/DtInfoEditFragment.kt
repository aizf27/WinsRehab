package com.example.winsrehab.ui.main.doctor.info

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
import com.example.winsrehab.data.entity.Doctor
import com.example.winsrehab.databinding.FragmentDtInfoEditBinding

/**
 * 医生信息编辑页面
 * 功能：编辑医生基本信息（姓名、性别、年龄、科室）
 * 只读：工号（系统分配）
 */
class DtInfoEditFragment : Fragment() {

    private var _binding: FragmentDtInfoEditBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: DtInfoVM by viewModels()
    private val args by navArgs<DtInfoEditFragmentArgs>()
    private var doctorCode: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        doctorCode = args.doctorCode
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDtInfoEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 加载医生数据
        viewModel.loadDoctorInfo(doctorCode)
        
        // 观察数据变化
        observeData()
        
        // 设置点击事件
        setupClickListeners()
    }

    /**
     * 观察数据变化，更新UI
     */
    private fun observeData() {
        viewModel.doctor.observe(viewLifecycleOwner) { doctor ->
            doctor?.let {
                // 更新基本信息
                binding.tvName.text = it.name.takeIf { name -> name != "未设置" } ?: "未设置"
                binding.tvGender.text = it.gender.takeIf { gender -> gender != "未设置" } ?: "未设置"
                binding.tvAge.text = if (it.age > 0) "${it.age}岁" else "未设置"
                
                // 更新执业信息
                binding.tvDepartment.text = it.department.takeIf { dept -> dept != "未设置" } ?: "未设置"
                binding.tvDoctorCode.text = it.doctorCode
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
            saveDoctorInfo()
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
        
        // 科室点击
        binding.layoutDepartment.setOnClickListener {
            showDepartmentDialog()
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
            maxLines = 1
            inputType = InputType.TYPE_CLASS_TEXT
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
     * 显示科室选择对话框
     */
    private fun showDepartmentDialog() {
        val departments = arrayOf("康复科", "骨科", "神经内科", "神经外科", "理疗科", "其他")
        val currentDept = binding.tvDepartment.text.toString()
        val checkedItem = departments.indexOf(currentDept).takeIf { it >= 0 } ?: -1

        AlertDialog.Builder(requireContext())
            .setTitle("选择科室")
            .setSingleChoiceItems(departments, checkedItem) { dialog, which ->
                if (which == departments.size - 1) {
                    // 选择"其他"，弹出输入框
                    dialog.dismiss()
                    showEditDialog("科室", "") { newValue ->
                        binding.tvDepartment.text = newValue
                    }
                } else {
                    binding.tvDepartment.text = departments[which]
                    dialog.dismiss()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 保存医生信息
     */
    private fun saveDoctorInfo() {
        val doctor = viewModel.doctor.value ?: run {
            Toast.makeText(requireContext(), "数据加载失败，请重试", Toast.LENGTH_SHORT).show()
            return
        }

        // 获取界面数据
        val name = binding.tvName.text.toString()
        val gender = binding.tvGender.text.toString()
        val ageStr = binding.tvAge.text.toString().replace("岁", "")
        val department = binding.tvDepartment.text.toString()

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
        if (department == "未设置" || department.isBlank()) {
            Toast.makeText(requireContext(), "请填写科室", Toast.LENGTH_SHORT).show()
            return
        }

        val age = ageStr.toIntOrNull() ?: 0

        // 创建更新后的医生对象
        val updatedDoctor = Doctor(
            doctorCode = doctorCode,
            password = doctor.password,
            name = name,
            gender = gender,
            age = age,
            department = department,
            patientCount = doctor.patientCount
        )

        // 调用 ViewModel 保存
        viewModel.saveDoctorInfo(updatedDoctor) { success ->
            if (success) {
                Toast.makeText(requireContext(), "保存成功", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            } else {
                Toast.makeText(requireContext(), "保存失败，请重试", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

