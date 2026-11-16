package com.example.winsrehab.ui.main.doctor.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.winsrehab.R
import com.example.winsrehab.data.entity.Doctor
import com.example.winsrehab.databinding.FragmentDtInfoBinding
import com.example.winsrehab.databinding.ItemInfoRowBinding
import com.example.winsrehab.databinding.WidgetInfoRowBinding
import com.example.winsrehab.ui.common.AccountArgs
import com.example.winsrehab.ui.common.UiTexts
import com.example.winsrehab.ui.main.doctor.info.DtInfoVM

// 医生「我的」页：资料自己加载，患者数自己观察，没有数据来源的统计项统一退占位
class dt_info_Fragment : Fragment() {

    private var _binding: FragmentDtInfoBinding? = null
    private val binding get() = _binding!!

    // 工号可能解析不到，null 表示参数异常，不拿空串去查库
    private var doctorCode: String? = null

    private val viewModel: DtInfoVM by viewModels()

    // 与医生首页、患者管理页共用同一个 VM，患者数三处同源
    private val homeViewModel: DtHomeVM by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 工号解析走 AccountArgs（导航参数优先、宿主 Intent 兜底）
        doctorCode = AccountArgs.doctorCode(arguments, activity?.intent)
        Log.i(TAG, "doctorCode: $doctorCode")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDtInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupInfoRows()
        setupPatientCount()
        setupObservers()

        // 编辑按钮与资料卡都进编辑页
        binding.btnSave.setOnClickListener { navigateToEdit() }
        binding.profileCard.setOnClickListener { navigateToEdit() }

        val code = doctorCode
        if (code == null) {
            // 缺工号属于参数异常：不发起查询，患者数保持统一占位
            Log.e(TAG, "缺少医生工号，跳过资料加载")
            return
        }
        viewModel.loadDoctorInfo(code)
    }

    // 键值区改统一信息行：只有标签，值由 fillUI 写入，缺值统一显示占位符
    private fun setupInfoRows() {
        binding.rowLicense.tvInfoLabel.text = "医师资格证"
        binding.rowHospital.tvInfoLabel.text = "所属医院"
        binding.rowTitle.tvInfoLabel.text = "职称"
        binding.rowPatientCount.tvInfoLabel.text = "管理患者数"
        binding.rowPhone.tvInfoLabel.text = "手机号码"
        binding.rowEmail.tvInfoLabel.text = "电子邮箱"

        // 设置区仍是带图标的动作行，图标色跟随医生端品牌色
        setupActionRow(binding.rowReport, R.drawable.ic_file_text, "我的报告")
        setupActionRow(binding.rowSettings, R.drawable.ic_settings, "系统设置")
    }

    // 动作行：图标 + 标题，右侧值区收起，点整行后续接动作
    private fun setupActionRow(rowBinding: ItemInfoRowBinding, iconRes: Int, title: String) {
        rowBinding.rowIcon.setImageResource(iconRes)
        rowBinding.rowIcon.setColorFilter(
            ContextCompat.getColor(requireContext(), R.color.brand_doctor_primary)
        )
        rowBinding.rowTitle.text = title
        rowBinding.rowValue.visibility = View.GONE
    }

    // 医生资料：姓名/性别年龄科室/工号 + 执业与联系方式，缺值一律占位
    private fun fillUI(doctor: Doctor) {
        binding.tvDoctorName.text = doctor.name.ifBlank { UiTexts.placeholderText() }

        val info = buildString {
            if (doctor.gender != "未设置") append(doctor.gender)
            if (doctor.age > 0) {
                if (isNotEmpty()) append(" · ")
                append("${doctor.age}岁")
            }
            if (doctor.department != "未设置") {
                if (isNotEmpty()) append(" · ")
                append(doctor.department)
            }
            if (isEmpty()) append(UiTexts.placeholderText())
        }
        binding.tvDoctorInfo.text = info
        binding.tvDoctorCode.text = "工号：${doctor.doctorCode}"

        binding.rowLicense.tvInfoValue.text = textOrPlaceholder(doctor.licenseNumber)
        binding.rowHospital.tvInfoValue.text = textOrPlaceholder(doctor.hospital)
        binding.rowTitle.tvInfoValue.text = textOrPlaceholder(doctor.title)
        binding.rowPhone.tvInfoValue.text = textOrPlaceholder(doctor.phone)
        binding.rowEmail.tvInfoValue.text = textOrPlaceholder(doctor.email)

        // 工作统计没有数据来源，统一显示占位符
        binding.tvMonthlyPlans.text = UiTexts.placeholderText()
        binding.tvSatisfaction.text = UiTexts.placeholderText()
    }

    // 库里的「未设置」与空串等价，都按缺值处理
    private fun textOrPlaceholder(value: String?): String =
        value?.takeIf { it.isNotBlank() && it != "未设置" } ?: UiTexts.placeholderText()

    // 患者数不读 Doctor 冗余字段，直接数患者查询结果，与医生首页同一数据源
    private fun setupPatientCount() {
        binding.rowPatientCount.tvInfoValue.text = UiTexts.placeholderText()
        homeViewModel.patients.observe(viewLifecycleOwner) { list ->
            binding.rowPatientCount.tvInfoValue.text = "${list.size} 人"
            Log.i(TAG, "patient count: ${list.size}")
        }
    }

    private fun setupObservers() {
        viewModel.doctor.observe(viewLifecycleOwner) { doctor ->
            doctor?.let { fillUI(it) }
        }
    }

    private fun navigateToEdit() {
        val code = doctorCode ?: return
        val action = dt_info_FragmentDirections.actionDtInfoFragmentToDtInfoEditFragment(code)
        findNavController().navigate(action)
    }

    override fun onResume() {
        super.onResume()
        // 每次返回时重新加载资料，确保显示最新信息
        val code = doctorCode
        if (code == null) {
            Log.e(TAG, "缺少医生工号，跳过资料刷新")
            return
        }
        viewModel.loadDoctorInfo(code)
        homeViewModel.loadPatients(code)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val TAG = "dt_info_Fragment"
    }
}
