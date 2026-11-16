package com.example.winsrehab.ui.main.doctor.info

import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.winsrehab.R
import com.example.winsrehab.data.entity.Doctor
import com.example.winsrehab.databinding.FragmentDtInfoEditBinding
import com.example.winsrehab.databinding.WidgetInfoRowBinding
import com.example.winsrehab.ui.common.AccountArgs
import com.example.winsrehab.ui.common.TextField
import com.example.winsrehab.ui.common.UiTexts
import com.google.android.material.dialog.MaterialAlertDialogBuilder

// 医生信息编辑页：整页只读信息行 + 弹窗编辑，弹窗输入框与错误提示统一
class DtInfoEditFragment : Fragment() {

    private var _binding: FragmentDtInfoEditBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DtInfoVM by viewModels()

    // 工号可能解析不到，null 表示参数异常，不拿空串去查库
    private var doctorCode: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 工号解析走统一工具：导航参数优先、宿主 Intent 兜底
        doctorCode = AccountArgs.doctorCode(arguments, activity?.intent)
        Log.i(TAG, "doctorCode: $doctorCode")
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

        binding.topBar.tvTopBarTitle.text = "编辑个人信息"
        binding.topBar.btnTopBarBack.setOnClickListener {
            findNavController().navigateUp()
        }
        binding.btnSave.setOnClickListener { saveDoctorInfo() }

        setupRows()
        observeData()

        val code = doctorCode
        if (code == null) {
            // 缺工号属于参数异常：给明确空态，不发起查询
            Log.e(TAG, "缺少医生工号，进入空态")
            showMissingCodeState()
            return
        }
        viewModel.loadDoctorInfo(code)
    }

    // 逐行登记标签与点击编辑动作，工号只读不登记
    private fun setupRows() {
        bindRow(binding.layoutName, "姓名") {
            showInputDialog(binding.layoutName, "姓名", "请输入姓名", MAX_NAME_LENGTH)
        }
        bindRow(binding.layoutGender, "性别") { showGenderDialog() }
        bindRow(binding.layoutAge, "年龄") { showAgeDialog() }
        bindRow(binding.layoutDepartment, "科室") { showDepartmentDialog() }
        bindRow(binding.layoutTitle, "职称") { showTitleDialog() }
        bindRow(binding.layoutHospital, "所属医院") {
            showInputDialog(binding.layoutHospital, "所属医院", "请输入所属医院", MAX_HOSPITAL_LENGTH)
        }
        bindRow(binding.layoutLicenseNumber, "医师资格证号") {
            showInputDialog(
                binding.layoutLicenseNumber, "医师资格证号", "请输入医师资格证号", MAX_LICENSE_LENGTH
            )
        }
        bindRow(binding.layoutPhone, "手机号码") { showPhoneDialog() }
        bindRow(binding.layoutEmail, "电子邮箱") {
            showInputDialog(binding.layoutEmail, "电子邮箱", "请输入电子邮箱", MAX_EMAIL_LENGTH)
        }

        // 工号系统分配，只显示不可点
        binding.rowDoctorCode.tvInfoLabel.text = "工号"
        binding.rowDoctorCode.tvInfoValue.text = UiTexts.placeholderText()
    }

    // 信息行：写标签、值先占位、整行加点击反馈并绑定动作
    private fun bindRow(row: WidgetInfoRowBinding, label: String, onClick: () -> Unit) {
        row.tvInfoLabel.text = label
        row.tvInfoValue.text = UiTexts.placeholderText()
        // 可编辑行给出箭头提示，点整行弹编辑框
        row.ivInfoChevron.visibility = View.VISIBLE

        val outValue = TypedValue()
        requireContext().theme.resolveAttribute(
            android.R.attr.selectableItemBackground, outValue, true
        )
        row.root.setBackgroundResource(outValue.resourceId)
        row.root.isClickable = true
        row.root.isFocusable = true
        row.root.setOnClickListener { onClick() }
    }

    // 观察医生数据，逐个信息行回填，缺值统一占位
    private fun observeData() {
        viewModel.doctor.observe(viewLifecycleOwner) { doctor ->
            doctor?.let { fillUI(it) }
        }
    }

    private fun fillUI(doctor: Doctor) {
        setRowValue(binding.layoutName, doctor.name)
        setRowValue(binding.layoutGender, doctor.gender)
        setRowValue(binding.layoutAge, if (doctor.age > 0) "${doctor.age}岁" else null)
        setRowValue(binding.layoutDepartment, doctor.department)
        setRowValue(binding.layoutTitle, doctor.title)
        setRowValue(binding.layoutHospital, doctor.hospital)
        setRowValue(binding.layoutLicenseNumber, doctor.licenseNumber)
        setRowValue(binding.layoutPhone, doctor.phone)
        setRowValue(binding.layoutEmail, doctor.email)

        // 工号只读展示，没有工号时同样是占位
        binding.rowDoctorCode.tvInfoValue.text =
            doctor.doctorCode.takeIf { it.isNotBlank() } ?: UiTexts.placeholderText()
    }

    // 读信息行当前值，占位符视为「未设置」
    private fun rowValue(row: WidgetInfoRowBinding): String {
        val text = row.tvInfoValue.text?.toString().orEmpty()
        return text.takeIf { it.isNotBlank() && it != UiTexts.placeholderText() } ?: UNSET
    }

    private fun setRowValue(row: WidgetInfoRowBinding, value: String?) {
        row.tvInfoValue.text = value
            ?.takeIf { it.isNotBlank() && it != UNSET }
            ?: UiTexts.placeholderText()
    }

    // 通用文本输入弹窗：统一 TextField + 字段级错误提示，校验不过不关闭
    private fun showInputDialog(
        row: WidgetInfoRowBinding,
        title: String,
        hint: String,
        maxLength: Int,
        inputType: Int = InputType.TYPE_CLASS_TEXT,
        validator: ((String) -> String?)? = null,
        normalize: (String) -> String = { it },
        prefill: (String) -> String = { it }
    ) {
        val field = TextField(requireContext())
        field.setLabel(title)
        field.setRequired(true)
        field.setHint(hint)
        field.editText().inputType = inputType
        field.editText().maxLines = 1
        rowValue(row).takeIf { it != UNSET }?.let { field.editText().setText(prefill(it)) }

        val container = FrameLayout(requireContext()).apply {
            val pad = resources.getDimensionPixelSize(R.dimen.space_24)
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
                val value = field.text().trim()
                // 先判空再判长度，最后交给字段自己的规则
                val error = when {
                    value.isEmpty() -> getString(R.string.error_field_empty, title)
                    value.length > maxLength ->
                        getString(R.string.error_field_too_long, title, maxLength)
                    else -> validator?.invoke(value)
                }
                if (error != null) {
                    field.setError(error)
                    return@setOnClickListener
                }
                field.setError(null)
                setRowValue(row, normalize(value))
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    // 性别：单选即生效
    private fun showGenderDialog() {
        val genders = arrayOf("男", "女")
        val checkedItem = genders.indexOf(rowValue(binding.layoutGender))

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("选择性别")
            .setSingleChoiceItems(genders, checkedItem) { dialog, which ->
                setRowValue(binding.layoutGender, genders[which])
                dialog.dismiss()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // 年龄：数字键盘 + 1–120 范围校验
    private fun showAgeDialog() {
        showInputDialog(
            row = binding.layoutAge,
            title = "年龄",
            hint = "请输入年龄",
            maxLength = 3,
            inputType = InputType.TYPE_CLASS_NUMBER,
            validator = { value ->
                val age = value.toIntOrNull()
                if (age != null && age in 1..120) null else getString(R.string.error_age_invalid)
            },
            normalize = { "${it.toInt()}岁" },
            prefill = { it.replace("岁", "") }
        )
    }

    // 科室：预置选项，「其他」转自定义输入
    private fun showDepartmentDialog() {
        val departments = arrayOf("康复科", "骨科", "神经内科", "神经外科", "理疗科", "其他")
        val checkedItem = departments.indexOf(rowValue(binding.layoutDepartment))

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("选择科室")
            .setSingleChoiceItems(departments, checkedItem) { dialog, which ->
                dialog.dismiss()
                if (which == departments.lastIndex) {
                    // 选「其他」时改为手填科室名
                    showInputDialog(binding.layoutDepartment, "科室", "请输入科室名称", MAX_NAME_LENGTH)
                } else {
                    setRowValue(binding.layoutDepartment, departments[which])
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // 职称：预置选项，「其他」转自定义输入
    private fun showTitleDialog() {
        val titles = arrayOf("住院医师", "主治医师", "副主任医师", "主任医师", "其他")
        val checkedItem = titles.indexOf(rowValue(binding.layoutTitle))

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("选择职称")
            .setSingleChoiceItems(titles, checkedItem) { dialog, which ->
                dialog.dismiss()
                if (which == titles.lastIndex) {
                    showInputDialog(binding.layoutTitle, "职称", "请输入职称", MAX_NAME_LENGTH)
                } else {
                    setRowValue(binding.layoutTitle, titles[which])
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // 手机号：11 位大陆号码格式校验
    private fun showPhoneDialog() {
        showInputDialog(
            row = binding.layoutPhone,
            title = "手机号码",
            hint = "请输入手机号码",
            maxLength = PHONE_LENGTH,
            inputType = InputType.TYPE_CLASS_PHONE,
            validator = { value ->
                if (PHONE_REGEX.matches(value)) null else getString(R.string.error_phone_invalid)
            }
        )
    }

    // 保存：把信息行上的值组装回 Doctor，占位视为未设置
    private fun saveDoctorInfo() {
        val doctor = viewModel.doctor.value ?: run {
            Toast.makeText(requireContext(), "数据加载失败，请重试", Toast.LENGTH_SHORT).show()
            return
        }

        val name = rowValue(binding.layoutName)
        val gender = rowValue(binding.layoutGender)
        val ageText = rowValue(binding.layoutAge)

        // 姓名、性别、年龄是必填项
        if (name == UNSET) {
            Toast.makeText(
                requireContext(), getString(R.string.error_field_empty, "姓名"), Toast.LENGTH_SHORT
            ).show()
            return
        }
        if (gender == UNSET) {
            Toast.makeText(
                requireContext(), getString(R.string.error_field_empty, "性别"), Toast.LENGTH_SHORT
            ).show()
            return
        }
        if (ageText == UNSET) {
            Toast.makeText(
                requireContext(), getString(R.string.error_field_empty, "年龄"), Toast.LENGTH_SHORT
            ).show()
            return
        }

        val age = ageText.replace("岁", "").toIntOrNull() ?: 0

        val updatedDoctor = doctor.copy(
            name = name,
            gender = gender,
            age = age,
            department = rowValue(binding.layoutDepartment),
            title = rowValue(binding.layoutTitle),
            hospital = rowValue(binding.layoutHospital),
            licenseNumber = rowValue(binding.layoutLicenseNumber),
            phone = rowValue(binding.layoutPhone),
            email = rowValue(binding.layoutEmail),
            updatedAt = System.currentTimeMillis()
        )

        viewModel.saveDoctorInfo(updatedDoctor) { success ->
            if (success) {
                Log.i(TAG, "保存医生信息成功: ${updatedDoctor.doctorCode}")
                Toast.makeText(requireContext(), "保存成功", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            } else {
                Log.e(TAG, "保存医生信息失败: ${updatedDoctor.doctorCode}")
                Toast.makeText(requireContext(), "保存失败，请重试", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 缺工号：隐藏整页内容，给出可读的空态，保存按钮置灰
    private fun showMissingCodeState() {
        binding.contentScroll.visibility = View.GONE
        binding.btnSave.isEnabled = false
        binding.stateEmpty.root.visibility = View.VISIBLE
        binding.stateEmpty.tvStateEmpty.setText(R.string.doctor_patient_manage_missing_code)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val TAG = "DtInfoEditFragment"

        // 库用「未设置」表示空值，与统一占位符区分开
        const val UNSET = "未设置"

        const val MAX_NAME_LENGTH = 20
        const val MAX_HOSPITAL_LENGTH = 50
        const val MAX_LICENSE_LENGTH = 30
        const val MAX_EMAIL_LENGTH = 50
        const val PHONE_LENGTH = 11

        val PHONE_REGEX = Regex("^1[3-9]\\d{9}$")
    }
}
