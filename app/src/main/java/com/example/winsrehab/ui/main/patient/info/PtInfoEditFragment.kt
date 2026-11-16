package com.example.winsrehab.ui.main.patient.info

import android.os.Bundle
import android.text.InputType
import android.widget.FrameLayout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.winsrehab.databinding.FragmentPtInfoEditBinding
import com.example.winsrehab.databinding.WidgetInfoRowBinding
import com.example.winsrehab.ui.common.AccountArgs
import com.example.winsrehab.ui.common.TextField
import com.example.winsrehab.ui.common.UiTexts
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * 患者信息编辑页面
 * 功能：编辑患者基本信息（姓名、性别、年龄、个性签名）
 * 只读：医生信息、康复信息
 */
class PtInfoEditFragment : Fragment() {

    private var _binding: FragmentPtInfoEditBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: PtInfoVM by activityViewModels()
    private var account: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        account = AccountArgs.account(arguments, requireActivity().intent)
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

        binding.topBar.tvTopBarTitle.text = "编辑个人信息"
        setupRows()
        
        // 缺少账号时不查询空串，保留明确错误文本
        account?.let { viewModel.loadPatient(it) } ?: run {
            binding.layoutName.tvInfoValue.text = UiTexts.errorText("患者信息缺失")
            binding.btnSave.isEnabled = false
        }
        
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
                setRowValue(binding.layoutName, it.name)
                setRowValue(binding.layoutGender, it.gender)
                setRowValue(binding.layoutAge, it.age.takeIf { age -> age > 0 }?.let { age -> "${age}岁" })
                setRowValue(binding.layoutNickname, it.nickname)
                setRowValue(binding.layoutSignature, it.signature.takeIf { sig -> sig != "这个人很懒，什么都没留下" })
                
                // 更新联系方式
                setRowValue(binding.layoutPhone, it.phone)
                setRowValue(binding.layoutEmail, it.email)
                setRowValue(binding.layoutDateOfBirth, it.dateOfBirth)
                setRowValue(binding.layoutAddress, it.address)
                
                // 更新医生信息（只读）
                // 主治医生显示医生名字
                binding.rowDoctorName.tvInfoValue.text = it.doctorName.takeIf { name -> name != "未设置" } ?: "未绑定"
                binding.rowDoctorCode.tvInfoValue.text = it.doctorCode.takeIf { code -> code != "未设置" } ?: "未绑定"
                
                // 更新康复信息（只读）
                binding.rowDiagnosis.tvInfoValue.text = it.diagnosis.takeIf { diag -> diag != "未设置" } ?: "由医生填写"
                binding.rowStage.tvInfoValue.text = it.rehabStage.takeIf { stage -> stage != "未设置" } ?: "由医生评估"
            }
        }
    }

    /**
     * 设置点击事件
     */
    private fun setupClickListeners() {
        // 返回按钮
        binding.topBar.btnTopBarBack.setOnClickListener {
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
        binding.layoutName.root.setOnClickListener {
            showEditDialog(binding.layoutName, "姓名", "请输入姓名") {
                setRowValue(binding.layoutName, it)
            }
        }
        
        // 性别点击
        binding.layoutGender.root.setOnClickListener {
            showGenderDialog()
        }
        
        // 年龄点击
        binding.layoutAge.root.setOnClickListener {
            showAgeDialog()
        }
        
        // 昵称点击
        binding.layoutNickname.root.setOnClickListener {
            showEditDialog(binding.layoutNickname, "昵称", "请输入昵称") {
                setRowValue(binding.layoutNickname, it)
            }
        }
        
        // 个性签名点击
        binding.layoutSignature.root.setOnClickListener {
            showEditDialog(binding.layoutSignature, "个性签名", "请输入个性签名", 100) {
                setRowValue(binding.layoutSignature, it)
            }
        }
        
        // 手机号码点击
        binding.layoutPhone.root.setOnClickListener {
            showPhoneDialog()
        }
        
        // 电子邮箱点击
        binding.layoutEmail.root.setOnClickListener {
            showEditDialog(binding.layoutEmail, "电子邮箱", "请输入电子邮箱", 50) {
                setRowValue(binding.layoutEmail, it)
            }
        }
        
        // 出生日期点击
        binding.layoutDateOfBirth.root.setOnClickListener {
            showDatePickerDialog()
        }
        
        // 地址点击
        binding.layoutAddress.root.setOnClickListener {
            showEditDialog(binding.layoutAddress, "地址", "请输入地址", 100) {
                setRowValue(binding.layoutAddress, it)
            }
        }
    }

    /**
     * 显示编辑对话框
     */
    private fun setupRows() {
        bindEditableRow(binding.layoutName, "姓名")
        bindEditableRow(binding.layoutGender, "性别")
        bindEditableRow(binding.layoutAge, "年龄")
        bindEditableRow(binding.layoutNickname, "昵称")
        bindEditableRow(binding.layoutSignature, "个性签名")
        bindEditableRow(binding.layoutPhone, "手机号码")
        bindEditableRow(binding.layoutEmail, "电子邮箱")
        bindEditableRow(binding.layoutDateOfBirth, "出生日期")
        bindEditableRow(binding.layoutAddress, "地址")
        bindReadOnlyRow(binding.rowDoctorName, "主治医生")
        bindReadOnlyRow(binding.rowDoctorCode, "医生工号")
        bindReadOnlyRow(binding.rowDiagnosis, "诊断信息")
        bindReadOnlyRow(binding.rowStage, "康复阶段")
    }

    private fun bindEditableRow(row: WidgetInfoRowBinding, label: String) {
        row.tvInfoLabel.text = label
        row.tvInfoValue.text = UiTexts.placeholderText()
        row.ivInfoChevron.visibility = View.VISIBLE
        row.root.isClickable = true
        row.root.isFocusable = true
    }

    private fun bindReadOnlyRow(row: WidgetInfoRowBinding, label: String) {
        row.tvInfoLabel.text = label
        row.tvInfoValue.text = UiTexts.placeholderText()
    }

    private fun rowValue(row: WidgetInfoRowBinding): String {
        val value = row.tvInfoValue.text?.toString().orEmpty()
        return value.takeIf { it.isNotBlank() && it != UiTexts.placeholderText() } ?: "未设置"
    }

    private fun setRowValue(row: WidgetInfoRowBinding, value: String?) {
        row.tvInfoValue.text = value
            ?.takeIf { it.isNotBlank() && it != "未设置" }
            ?: UiTexts.placeholderText()
    }

    // 通用文本输入：统一 TextField 与 Material 对话框，错误留在字段下方
    private fun showEditDialog(
        row: WidgetInfoRowBinding,
        title: String,
        hint: String,
        maxLength: Int = 20,
        inputType: Int = InputType.TYPE_CLASS_TEXT,
        validator: ((String) -> String?)? = null,
        normalize: (String) -> String = { it },
        prefill: (String) -> String = { it },
        onConfirm: (String) -> Unit
    ) {
        val field = TextField(requireContext()).apply {
            setLabel(title)
            setRequired(true)
            setHint(hint)
            editText().maxLines = if (maxLength > 50) 3 else 1
            editText().inputType = if (maxLength > 50) {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            } else {
                inputType
            }
            rowValue(row).takeIf { it != "未设置" }?.let { editText().setText(prefill(it)) }
        }

        val container = FrameLayout(requireContext()).apply {
            val pad = resources.getDimensionPixelSize(com.example.winsrehab.R.dimen.space_24)
            setPadding(pad, pad / 2, pad, 0)
            addView(field)
        }
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("编辑$title")
            .setView(container)
            .setPositiveButton("确定", null)
            .setNegativeButton("取消", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val newValue = field.text().trim()
                val error = when {
                    newValue.isEmpty() -> getString(com.example.winsrehab.R.string.error_field_empty, title)
                    newValue.length > maxLength -> getString(com.example.winsrehab.R.string.error_field_too_long, title, maxLength)
                    else -> validator?.invoke(newValue)
                }
                if (error != null) {
                    field.setError(error)
                    return@setOnClickListener
                }
                onConfirm(normalize(newValue))
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    /**
     * 显示性别选择对话框
     */
    private fun showGenderDialog() {
        val genders = arrayOf("男", "女")
        val currentGender = rowValue(binding.layoutGender)
        val checkedItem = when (currentGender) {
            "男" -> 0
            "女" -> 1
            else -> -1
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("选择性别")
            .setSingleChoiceItems(genders, checkedItem) { dialog, which ->
                setRowValue(binding.layoutGender, genders[which])
                dialog.dismiss()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 显示年龄输入对话框
     */
    private fun showAgeDialog() {
        showEditDialog(
            binding.layoutAge,
            "年龄",
            "请输入年龄",
            maxLength = 3,
            inputType = InputType.TYPE_CLASS_NUMBER,
            validator = { value ->
                if (value.toIntOrNull() in 1..120) null else getString(com.example.winsrehab.R.string.error_age_invalid)
            },
            normalize = { "${it.toInt()}岁" },
            prefill = { it.replace("岁", "") }
        ) { setRowValue(binding.layoutAge, it) }
    }

    /**
     * 显示手机号输入对话框
     */
    private fun showPhoneDialog() {
        showEditDialog(
            binding.layoutPhone,
            "手机号码",
            "请输入手机号码",
            maxLength = 11,
            inputType = InputType.TYPE_CLASS_PHONE,
            validator = { value ->
                if (value.matches(Regex("^1[3-9]\\d{9}$"))) null else getString(com.example.winsrehab.R.string.error_phone_invalid)
            }
        ) { setRowValue(binding.layoutPhone, it) }
    }

    /**
     * 显示日期选择对话框
     */
    private fun showDatePickerDialog() {
        val calendar = java.util.Calendar.getInstance()
        val currentDate = rowValue(binding.layoutDateOfBirth)
        
        // 解析当前日期
        if (currentDate != "未设置") {
            try {
                val parts = currentDate.split("-")
                if (parts.size == 3) {
                    calendar.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
                }
            } catch (e: Exception) {
                // 使用默认日期
            }
        }

        android.app.DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val dateStr = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                setRowValue(binding.layoutDateOfBirth, dateStr)
            },
            calendar.get(java.util.Calendar.YEAR),
            calendar.get(java.util.Calendar.MONTH),
            calendar.get(java.util.Calendar.DAY_OF_MONTH)
        ).show()
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
        val name = rowValue(binding.layoutName)
        val gender = rowValue(binding.layoutGender)
        val ageStr = rowValue(binding.layoutAge).replace("岁", "")
        val nickname = rowValue(binding.layoutNickname)
        val signature = rowValue(binding.layoutSignature)
        val phone = rowValue(binding.layoutPhone)
        val email = rowValue(binding.layoutEmail)
        val dateOfBirth = rowValue(binding.layoutDateOfBirth)
        val address = rowValue(binding.layoutAddress)

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

        // 创建更新后的患者对象
        val updatedPatient = patient.copy(
            name = name,
            gender = gender,
            age = age,
            nickname = if (nickname == "未设置") "未设置" else nickname,
            signature = if (signature == "未设置") "这个人很懒，什么都没留下" else signature,
            phone = if (phone == "未设置") "未设置" else phone,
            email = if (email == "未设置") "未设置" else email,
            dateOfBirth = if (dateOfBirth == "未设置") "未设置" else dateOfBirth,
            address = if (address == "未设置") "未设置" else address,
            updatedAt = System.currentTimeMillis()
        )

        // 调用 ViewModel 保存
        viewModel.savePatientInfo(updatedPatient) { success ->
            if (success) {
                // 显示成功提示
                Toast.makeText(requireContext(), "保存成功", Toast.LENGTH_SHORT).show()
                
                // 返回上一页
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

