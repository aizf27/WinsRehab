package com.example.winsrehab.ui.main.doctor.home

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.winsrehab.R
import com.example.winsrehab.databinding.FragmentDtHomeBinding
import com.example.winsrehab.ui.common.AccountArgs
import com.example.winsrehab.ui.common.UiTexts
import com.example.winsrehab.ui.main.doctor.adapter.HomeApplicationAdapter
import com.example.winsrehab.ui.main.doctor.application.PatientApplicationActivity
import java.util.Calendar

// 医生首页：欢迎卡接真实计数、待处理区展示最新患者申请、四宫格两个可用两个待开放
class dt_homeFragment : Fragment() {

    private var _binding: FragmentDtHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DtHomeVM by viewModels()

    private lateinit var applicationAdapter: HomeApplicationAdapter
    private var doctorCode: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 工号解析走 AccountArgs：导航参数优先，宿主 Intent 兜底
        doctorCode = AccountArgs.doctorCode(arguments, activity?.intent).orEmpty()
        Log.i("dt_homeFragment", "doctorCode: $doctorCode")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDtHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupGreeting()
        setupSummary()
        setupApplications()
        setupNotification()
        setupEntries()

        // 待审核视频暂无数据源，统一占位，不展示任何写死数字
        binding.tvReviewCount.text = UiTexts.placeholderText()

        viewModel.loadPatients(doctorCode)
        viewModel.loadPendingApplications(doctorCode)
    }

    // 欢迎卡三列统计：患者数接患者查询，待处理接待办计数
    private fun setupSummary() {
        viewModel.patients.observe(viewLifecycleOwner) { list ->
            binding.tvPatientCount.text = list.size.toString()
            binding.tvManagePatientsSubtitle.text =
                getString(R.string.doctor_home_manage_patients_subtitle, list.size)
        }
    }

    // 最新患者申请列表：有数据出列表，无数据出空态
    private fun setupApplications() {
        applicationAdapter = HomeApplicationAdapter { openApplicationPage() }
        binding.rvApplications.layoutManager = LinearLayoutManager(requireContext())
        binding.rvApplications.adapter = applicationAdapter
        binding.stateApplicationsEmpty.tvStateEmpty.setText(R.string.doctor_home_applications_empty)

        viewModel.pendingApplications.observe(viewLifecycleOwner) { list ->
            applicationAdapter.submitList(list)
            val hasApplications = list.isNotEmpty()
            binding.rvApplications.visibility = if (hasApplications) View.VISIBLE else View.GONE
            binding.stateApplicationsEmpty.root.visibility =
                if (hasApplications) View.GONE else View.VISIBLE
        }
    }

    // 横幅、欢迎卡摘要、待处理计数三者同源，都来自待办查询
    private fun setupNotification() {
        binding.layoutNotification.setOnClickListener { openApplicationPage() }
        binding.btnViewApplications.setOnClickListener { openApplicationPage() }

        viewModel.pendingCount.observe(viewLifecycleOwner) { count ->
            binding.tvPendingCount.text = count.toString()

            if (count > 0) {
                binding.layoutNotification.visibility = View.VISIBLE
                binding.tvNotification.text = getString(R.string.doctor_home_pending_notice, count)
                binding.tvBrief.visibility = View.VISIBLE
                binding.tvBrief.text = getString(R.string.doctor_home_pending_brief, count)
            } else {
                binding.layoutNotification.visibility = View.GONE
                binding.tvBrief.visibility = View.GONE
            }
        }
    }

    // 四宫格入口：管理患者切底部导航，制定计划进计划列表；另两个卡置灰不可点
    private fun setupEntries() {
        binding.cardPatientManage.setOnClickListener {
            val activity = requireActivity() as? DtHomeActivity
            activity?.binding?.bottomNav?.selectedItemId = R.id.dt_pt_manageFragment
        }

        binding.cardCreatePlan.setOnClickListener {
            Log.i("dt_homeFragment", "open rehab plan list: $doctorCode")
            findNavController().navigate(
                dt_homeFragmentDirections.actionDtHomeFragmentToRehabPlanList(doctorCode)
            )
        }
    }

    private fun openApplicationPage() {
        Log.i("dt_homeFragment", "open patient applications: $doctorCode")
        val intent = Intent(requireContext(), PatientApplicationActivity::class.java)
        intent.putExtra("doctorCode", doctorCode)
        startActivity(intent)
    }

    private fun setupGreeting() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = when {
            hour in 6..11 -> getString(R.string.doctor_home_greeting_morning)
            hour in 12..13 -> getString(R.string.doctor_home_greeting_noon)
            hour in 14..17 -> getString(R.string.doctor_home_greeting_afternoon)
            else -> getString(R.string.doctor_home_greeting_evening)
        }
        binding.tvGreeting.text = greeting
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
