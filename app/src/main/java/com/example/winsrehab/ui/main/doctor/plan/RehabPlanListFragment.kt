package com.example.winsrehab.ui.main.doctor.plan

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
import com.example.winsrehab.databinding.FragmentRehabPlanListBinding
import com.example.winsrehab.ui.common.AccountArgs
import com.example.winsrehab.ui.common.UiTexts
import com.example.winsrehab.ui.main.doctor.adapter.PatientPlanGroupAdapter

// 康复计划列表：按患者分组展示计划，加载 / 空 / 列表三态，条目可进创建计划页
class RehabPlanListFragment : Fragment() {

    private var _binding: FragmentRehabPlanListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RehabPlanListViewModel by viewModels()
    private lateinit var adapter: PatientPlanGroupAdapter

    // 工号可能解析不到，null 表示参数异常，不拿空串去查库
    private var doctorCode: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 工号解析走 AccountArgs：导航参数优先，宿主 Intent 兜底
        doctorCode = AccountArgs.doctorCode(arguments, activity?.intent)
        Log.i(TAG, "doctorCode: $doctorCode")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRehabPlanListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupStates()
        setupObservers()

        val code = doctorCode
        if (code == null) {
            // 缺工号属于参数异常：给明确空态，不发起查询
            Log.e(TAG, "缺少医生工号，进入空态")
            showMissingCodeState()
            return
        }
        viewModel.loadPatientPlans(code)
    }

    private fun setupViews() {
        binding.topBar.tvTopBarTitle.text = "康复计划"
        binding.topBar.btnTopBarBack.setOnClickListener {
            findNavController().navigateUp()
        }

        adapter = PatientPlanGroupAdapter(
            onCreatePlanClick = { patient ->
                // 创建计划页需要患者身份，点哪个患者就带哪个
                val intent = Intent(requireContext(), CreateRehabPlanActivity::class.java)
                intent.putExtra("patientId", patient.patientId)
                startActivity(intent)
            },
            onPlanClick = { plan ->
                // 计划详情页尚未实现，点击先留空
                Log.i(TAG, "点击计划: ${plan.planId}")
            }
        )

        binding.rvPatientPlans.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPatientPlans.adapter = adapter
    }

    // 三态文案统一从 UiTexts 取，初始停在加载态
    private fun setupStates() {
        binding.statePlansLoading.tvStateLoading.text = UiTexts.loadingText()
        binding.statePlansEmpty.tvStateEmpty.setText(R.string.doctor_plan_list_empty)
        binding.statePlansError.btnStateRetry.text = UiTexts.retryText()
        binding.statePlansError.btnStateRetry.setOnClickListener { retryLoad() }
        showLoading(true)
    }

    private fun setupObservers() {
        viewModel.patientPlanGroups.observe(viewLifecycleOwner) { groups ->
            // 出错时列表内容保持上一次结果，由错误态接管展示
            if (viewModel.loadError.value != null) return@observe
            adapter.submitList(groups)
            showLoading(false)
            val empty = groups.isEmpty()
            binding.statePlansEmpty.root.visibility = if (empty) View.VISIBLE else View.GONE
            binding.rvPatientPlans.visibility = if (empty) View.GONE else View.VISIBLE
        }

        viewModel.loadError.observe(viewLifecycleOwner) { reason ->
            if (reason != null) showError(reason)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) showLoading(true)
        }
    }

    // 只有一个加载中指示位，避免列表与加载态同时可见
    private fun showLoading(loading: Boolean) {
        binding.statePlansLoading.root.visibility = if (loading) View.VISIBLE else View.GONE
        if (loading) {
            binding.statePlansEmpty.root.visibility = View.GONE
            binding.statePlansError.root.visibility = View.GONE
            binding.rvPatientPlans.visibility = View.GONE
        }
    }

    // 查询失败：显示原因 + 重试，列表与其余状态收起
    private fun showError(reason: String) {
        showLoading(false)
        binding.statePlansEmpty.root.visibility = View.GONE
        binding.rvPatientPlans.visibility = View.GONE
        binding.statePlansError.tvStateError.text = UiTexts.errorText(reason)
        binding.statePlansError.root.visibility = View.VISIBLE
    }

    private fun retryLoad() {
        val code = doctorCode ?: return
        Log.i(TAG, "重试加载康复计划: doctorCode=$code")
        showLoading(true)
        viewModel.loadPatientPlans(code)
    }

    // 缺工号：空态换成明确的账号提示，列表与加载态都收起
    private fun showMissingCodeState() {
        showLoading(false)
        binding.rvPatientPlans.visibility = View.GONE
        binding.statePlansEmpty.tvStateEmpty.setText(R.string.doctor_patient_manage_missing_code)
        binding.statePlansEmpty.root.visibility = View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        // 从创建计划页返回时刷新分组
        val code = doctorCode ?: return
        viewModel.refresh(code)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val TAG = "RehabPlanListFragment"
    }
}
