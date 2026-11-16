package com.example.winsrehab.ui.main.doctor.home

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.winsrehab.R
import com.example.winsrehab.databinding.FragmentDtPtManageBinding
import com.example.winsrehab.ui.common.AccountArgs
import com.example.winsrehab.ui.common.UiTexts
import com.example.winsrehab.ui.main.doctor.adapter.PatientAdapter
import com.example.winsrehab.ui.main.doctor.plan.CreateRehabPlanActivity

// 患者管理页：只列已绑定患者，点卡片进创建康复计划；患者数与医生首页同源同口径
class dt_pt_manageFragment : Fragment() {

    private var _binding: FragmentDtPtManageBinding? = null
    private val binding get() = _binding!!

    // 与医生首页共用同一个 VM：一次查询同时喂首页统计和本页列表，两处数字不会各算各的
    private val viewModel: DtHomeVM by activityViewModels()

    // 工号可能解析不到，null 表示参数异常，不拿空串去查库
    private var doctorCode: String? = null
    private lateinit var adapter: PatientAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 工号解析走 AccountArgs：导航参数优先，宿主 Intent 兜底
        doctorCode = AccountArgs.doctorCode(arguments, activity?.intent)
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

        setupList()
        setupStates()
        setupObservers()

        val code = doctorCode
        if (code == null) {
            // 缺工号属于参数异常：直接进明确空态，不发起任何查询
            Log.e("dt_pt_manageFragment", "缺少医生工号，进入空态")
            showMissingCodeState()
            return
        }
        viewModel.loadPatients(code)
    }

    // 列表骨架：点卡片把患者 id 带进创建计划页
    private fun setupList() {
        adapter = PatientAdapter { patient ->
            val intent = Intent(requireContext(), CreateRehabPlanActivity::class.java)
            intent.putExtra("patientId", patient.patientId)
            startActivity(intent)
        }
        binding.rvPatients.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPatients.adapter = adapter
    }

    // 三态文案：加载中与空态都用统一组件，文案走统一出口
    private fun setupStates() {
        binding.statePatientsLoading.tvStateLoading.text = UiTexts.loadingText()
        binding.statePatientsEmpty.tvStateEmpty.setText(R.string.doctor_patient_manage_empty)
    }

    // 患者列表：条数写进标题下方，与首页「管理患者」同一个数
    private fun setupObservers() {
        viewModel.activePatients.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            binding.tvPatientTotal.text = getString(R.string.doctor_patient_manage_total, list.size)
            binding.statePatientsLoading.root.visibility = View.GONE

            val hasPatients = list.isNotEmpty()
            binding.rvPatients.visibility = if (hasPatients) View.VISIBLE else View.GONE
            binding.statePatientsEmpty.root.visibility =
                if (hasPatients) View.GONE else View.VISIBLE
        }
    }

    // 拿不到工号时的兜底态：列表与加载态都收起来，只留一句原因
    private fun showMissingCodeState() {
        binding.tvPatientTotal.text = UiTexts.placeholderText()
        binding.statePatientsLoading.root.visibility = View.GONE
        binding.rvPatients.visibility = View.GONE
        binding.statePatientsEmpty.tvStateEmpty.setText(R.string.doctor_patient_manage_missing_code)
        binding.statePatientsEmpty.root.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
