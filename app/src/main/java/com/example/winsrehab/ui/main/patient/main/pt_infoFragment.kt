package com.example.winsrehab.ui.main.patient.main

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.winsrehab.R
import com.example.winsrehab.data.entity.Patient
import com.example.winsrehab.databinding.FragmentPtInfoBinding
import com.example.winsrehab.ui.main.patient.info.PtInfoVM

class pt_infoFragment : Fragment() {

    private var _binding: FragmentPtInfoBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PtInfoVM by activityViewModels()
    private val args by navArgs<pt_infoFragmentArgs>()
    private var account: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        account = resolveAccount()
        Log.d("pt_infoFragment", "account: $account")
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

        // 只在数据为空时才拉取患者信息（避免重复查询数据库）
        if (viewModel.patient.value == null) {
            viewModel.loadPatient(account)
        }

        // 设置点击事件
        setupClickListeners()
    }

    /**
     * 初始化各行的图标和标题
     */
    private fun setupInfoRows() {
        // 基本信息板块
        setupRow(binding.rowPhone.root, R.drawable.ic_phone, "电话")
        setupRow(binding.rowEmail.root, R.drawable.ic_email, "邮箱")
        setupRow(binding.rowDob.root, R.drawable.ic_calendar, "出生日期")
        setupRow(binding.rowAddress.root, R.drawable.ic_location, "地址")

        // 康复信息板块
        setupRow(binding.rowRecord.root, R.drawable.ic_description, "康复记录")
        setupRow(binding.rowTraining.root, R.drawable.ic_calendar, "训练计划")
        setupRow(binding.rowDoctor.root, R.drawable.ic_user, "主治医生")

        // 设置板块
        setupRow(binding.rowSystem.root, R.drawable.ic_settings, "系统设置")
    }

    /**
     * 设置单行的图标和标题
     */
    private fun setupRow(rowView: View, iconRes: Int, title: String) {
        rowView.findViewById<ImageView>(R.id.rowIcon).setImageResource(iconRes)
        rowView.findViewById<TextView>(R.id.rowTitle).text = title
    }

    /**
     * 设置单行的值
     */
    private fun setRowValue(rowView: View, value: String?) {
        rowView.findViewById<TextView>(R.id.rowValue).text = value?.takeIf { it.isNotBlank() } ?: "未设置"
    }

    /**
     * 填充UI数据
     */
    private fun fillUI(patient: Patient) {
        // 顶部卡片信息
        binding.tvPatientName.text = patient.name ?: "患者"
        binding.tvPatientId.text = "患者 ID: ${patient.id}"
        binding.tvRehabCenter.text = "康复中心：XX医院康复科"

        // 基本信息
        setRowValue(binding.rowPhone.root, patient.account)  // 电话用account
        setRowValue(binding.rowEmail.root, null)  // 邮箱暂无，显示未设置
        setRowValue(binding.rowDob.root, null)    // 出生日期暂无，显示未设置
        setRowValue(binding.rowAddress.root, null) // 地址暂无，显示未设置

        // 康复信息
        setRowValue(binding.rowRecord.root, "查看详情")
        setRowValue(binding.rowTraining.root, "查看详情")
        setRowValue(binding.rowDoctor.root, patient.physicianCode)  // 显示主治医生工号

        // 设置板块
        setRowValue(binding.rowSystem.root, "")
    }

    /**
     * 设置点击事件
     */
    private fun setupClickListeners() {
        // 康复记录 - 跳转到康复中心页
        binding.rowRecord.root.setOnClickListener {
            findNavController().navigate(R.id.pt_rehabFragment)
        }

        // 训练计划 - 暂不设置点击事件
        // binding.rowTraining.root.setOnClickListener { }

        // 主治医生 - 暂不设置点击事件
        // binding.rowDoctor.root.setOnClickListener { }

        // 系统设置 - 暂不设置点击事件
        // binding.rowSystem.root.setOnClickListener { }

        // 切换账户 - 暂不设置点击事件
        // binding.btnLogout.setOnClickListener { }
    }

    /**
     * BottomNav 未传递 account 时，从宿主 Activity 的 Intent 兜底获取
     */
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
