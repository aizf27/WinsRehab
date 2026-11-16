package com.example.winsrehab.ui.main.doctor.home

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.winsrehab.databinding.FragmentDtPtManageBinding
import com.example.winsrehab.ui.common.AccountArgs
import com.example.winsrehab.ui.main.doctor.adapter.PatientAdapter
import com.example.winsrehab.ui.main.doctor.plan.CreateRehabPlanActivity
import com.example.winsrehab.R

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class dt_pt_manageFragment : Fragment() {

    private var _binding: FragmentDtPtManageBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DtHomeVM by viewModels()
    private var doctorCode : String=""
    private var ptCount: Int=0

    private lateinit var adapter: PatientAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 工号解析走 AccountArgs：导航参数优先，宿主 Intent 兜底
        doctorCode = AccountArgs.doctorCode(arguments, activity?.intent).orEmpty()
        Log.i("dt_pt_manageFragment", "doctorCode: $doctorCode")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDtPtManageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()
        setupObservers()
        viewModel.loadPatients(doctorCode)
    }

    private fun setupViews() {
        // 设置患者列表适配器
        adapter = PatientAdapter { patient ->
            // 点击患者卡片，跳转到创建康复计划页面
            val intent = Intent(requireContext(), CreateRehabPlanActivity::class.java)
            intent.putExtra("patientId", patient.patientId)
            startActivity(intent)
        }
        
        binding.rvPatients.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPatients.adapter = adapter
    }

    private fun setupObservers() {
        // 观察患者列表数据
        viewModel.patients.observe(viewLifecycleOwner) { list ->
            if (list.isEmpty()) {
                binding.emptyLayout.visibility = View.VISIBLE
                binding.rvPatients.visibility = View.GONE
            } else {
                binding.emptyLayout.visibility = View.GONE
                binding.rvPatients.visibility = View.VISIBLE
                adapter.submitList(list)
            }
            ptCount = list.size
            Log.d("dt_pt_manageFragment", "患者数量: $ptCount")
        }

        // 监听医生信息更新
        parentFragmentManager.setFragmentResultListener("doctor_info_result", viewLifecycleOwner) { _, result ->
            val isUpdated = result.getBoolean("doctor_info_updated", false)
            if (isUpdated) {
                viewModel.loadPatients(doctorCode)
                Log.d("dt_pt_manageFragment", "医生信息已更新，重新加载患者数据")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadPatients(doctorCode)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            dt_pt_manageFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
