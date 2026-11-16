package com.example.winsrehab.ui.main.doctor.plan

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.winsrehab.databinding.FragmentRehabPlanListBinding
import com.example.winsrehab.ui.common.AccountArgs
import com.example.winsrehab.ui.main.doctor.adapter.PatientPlanGroupAdapter

/**
 * 康复计划列表Fragment - 按患者分组显示
 */
class RehabPlanListFragment : Fragment() {

    private var _binding: FragmentRehabPlanListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RehabPlanListViewModel by viewModels()
    private lateinit var adapter: PatientPlanGroupAdapter
    private var doctorCode: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 工号解析走 AccountArgs：导航参数优先，宿主 Intent 兜底
        doctorCode = AccountArgs.doctorCode(arguments, activity?.intent).orEmpty()
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
        setupObservers()
        viewModel.loadPatientPlans(doctorCode)
    }

    private fun setupViews() {
        // 设置适配器
        adapter = PatientPlanGroupAdapter(
            onCreatePlanClick = { patient ->
                // 跳转到创建计划页面
                val intent = Intent(requireContext(), CreateRehabPlanActivity::class.java)
                intent.putExtra("patientId", patient.patientId)
                startActivity(intent)
            },
            onPlanClick = { plan ->
                // TODO: 跳转到计划详情页面
                // val intent = Intent(requireContext(), RehabPlanDetailActivity::class.java)
                // intent.putExtra("planId", plan.planId)
                // startActivity(intent)
            }
        )

        binding.rvPatientPlans.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPatientPlans.adapter = adapter
    }

    private fun setupObservers() {
        // 观察数据
        viewModel.patientPlanGroups.observe(viewLifecycleOwner) { groups ->
            if (groups.isEmpty()) {
                binding.emptyLayout.visibility = View.VISIBLE
                binding.rvPatientPlans.visibility = View.GONE
            } else {
                binding.emptyLayout.visibility = View.GONE
                binding.rvPatientPlans.visibility = View.VISIBLE
                adapter.submitList(groups)
            }
        }

        // 观察加载状态
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        // 刷新数据
        viewModel.refresh(doctorCode)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(doctorCode: String): RehabPlanListFragment {
            val fragment = RehabPlanListFragment()
            val args = Bundle()
            args.putString("doctorCode", doctorCode)
            fragment.arguments = args
            return fragment
        }
    }
}



