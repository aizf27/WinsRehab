package com.example.winsrehab.ui.main.patient.main

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.winsrehab.R
import com.example.winsrehab.data.entity.Patient
import com.example.winsrehab.databinding.FragmentPtInfoBinding
import com.example.winsrehab.ui.common.AccountArgs
import com.example.winsrehab.ui.common.UiTexts
import com.example.winsrehab.ui.main.patient.info.PtInfoVM

class pt_infoFragment : Fragment() {

    private var _binding: FragmentPtInfoBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PtInfoVM by activityViewModels()
    private var account: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        account = resolveAccount()
        Log.i("pt_infoFragment", "account: ${account.orEmpty()}")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPtInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 初始化各行的图标和标题
        setupInfoRows()

        // 观察患者数据，变化就更新
        viewModel.patient.observe(viewLifecycleOwner) { patient ->
            patient?.let {
                fillUI(it)
            }
        }

        // 缺少账号时停在明确错误态，不能把空串传进查询
        if (account == null) {
            binding.tvPatientName.text = UiTexts.errorText("患者信息缺失")
        } else if (viewModel.patient.value == null) {
            viewModel.loadPatient(account!!)
        }

        // 设置点击事件
        setupClickListeners()
    }

    /**
     * 初始化各行的图标和标题
     */
    private fun setupInfoRows() {
        // 基本信息板块
        setupRow(binding.rowPhone, R.drawable.ic_phone, "电话")
        setupRow(binding.rowEmail, R.drawable.ic_email, "邮箱")
        setupRow(binding.rowDob, R.drawable.ic_calendar, "出生日期")
        setupRow(binding.rowAddress, R.drawable.ic_location, "地址")

        // 康复信息板块
        setupRow(binding.rowDoctor, R.drawable.ic_user, "主治医生")
        setupRow(binding.rowRecord, R.drawable.ic_description, "康复记录", true)
        setupRow(binding.rowTraining, R.drawable.ic_calendar, "训练计划")


        // 设置板块
        setupRow(binding.rowSystem, R.drawable.ic_settings, "系统设置")
    }

    /**
     * 设置单行的图标和标题
     */
    private fun setupRow(row: com.example.winsrehab.databinding.WidgetInfoRowBinding, iconRes: Int, title: String, clickable: Boolean = false) {
        row.ivInfoIcon.setImageResource(iconRes)
        row.ivInfoIcon.visibility = View.VISIBLE
        row.tvInfoLabel.text = title
        row.ivInfoChevron.visibility = if (clickable) View.VISIBLE else View.GONE
    }

    /**
     * 设置单行的值
     */
    private fun setRowValue(row: com.example.winsrehab.databinding.WidgetInfoRowBinding, value: String?) {
        row.tvInfoValue.text = value?.takeIf { it.isNotBlank() } ?: UiTexts.placeholderText()
    }

    /**
     * 填充UI数据
     */
    private fun fillUI(patient: Patient) {
        // 顶部卡片信息（与数据库同步）
        binding.tvPatientName.text = patient.name.takeIf { it != "未设置" } ?: "未设置"
        
        // 性别和年龄
        val genderAge = buildString {
            if (patient.gender != "未设置") {
                append(patient.gender)
            }
            if (patient.age > 0) {
                if (isNotEmpty()) append(" · ")
                append("${patient.age}岁")
            }
            if (isEmpty()) append("未设置")
        }
        binding.tvPatientGender.text = genderAge
        
        // 个性签名
        binding.tvPatientSignature.text = patient.signature.takeIf { it != "未设置" } ?: "这个人很懒，什么都没留下"

        // 基本信息
        setRowValue(binding.rowPhone, patient.phone)
        setRowValue(binding.rowEmail, patient.email)
        setRowValue(binding.rowDob, patient.dateOfBirth)
        setRowValue(binding.rowAddress, patient.address)

        // 康复信息
        setRowValue(binding.rowRecord, "查看详情")
        setRowValue(binding.rowTraining, UiTexts.placeholderText())
        
        // 主治医生：有工号就显示工号，医生还没填姓名时只显示工号，别把工号一起吞掉
        val hasDoctorCode = patient.doctorCode.isNotBlank() && patient.doctorCode != "未设置"
        val doctorName = patient.doctorName
        val doctorInfo = when {
            !hasDoctorCode -> "未绑定"
            doctorName.isBlank() || doctorName == "未设置" -> patient.doctorCode
            else -> "${patient.doctorCode}  $doctorName"
        }
        setRowValue(binding.rowDoctor, doctorInfo)

        // 设置板块
        setRowValue(binding.rowSystem, UiTexts.placeholderText())
    }

    /**
     * 设置点击事件
     */
    private fun setupClickListeners() {
        // 编辑按钮 - 跳转到编辑页面
        binding.btnEdit.setOnClickListener {
            val action = pt_infoFragmentDirections.actionPtInfoFragmentToPtInfoEditFragment(account)
            findNavController().navigate(action)
        }

        // 点击卡片 - 跳转到编辑页面
        binding.profileCard.setOnClickListener {
            val action = pt_infoFragmentDirections.actionPtInfoFragmentToPtInfoEditFragment(account)
            findNavController().navigate(action)
        }

        // 康复记录 - 跳转到康复中心页
        binding.rowRecord.root.setOnClickListener {
            findNavController().navigate(R.id.pt_rehabFragment)
        }

        // 训练计划 - 暂不设置点击事件
        // binding.rowTraining.root.setOnClickListener { }

        // 主治医生 - 暂不设置点击事件
        // binding.rowDoctor.root.setOnClickListener { }

        // 系统���置 - 暂不设置点击事件
        // binding.rowSystem.root.setOnClickListener { }

        // 切换账户 - 暂不设置点击事件
        // binding.btnLogout.setOnClickListener { }
    }

    // 账号解析统一走 AccountArgs：导航参数优先，宿主 Intent 兜底
    private fun resolveAccount(): String? {
        val resolved = AccountArgs.account(arguments, requireActivity().intent)
        Log.i("pt_infoFragment", "解析到账号: account=${resolved.orEmpty()}")
        return resolved
    }

    override fun onResume() {
        super.onResume()
        // 每次返回时重新加载数据，确保显示最新信息
        account?.let { viewModel.loadPatient(it) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
