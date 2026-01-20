package com.example.winsrehab.ui.main.doctor.home

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.example.winsrehab.databinding.FragmentDtHomeBinding
import com.example.winsrehab.ui.main.doctor.application.PatientApplicationActivity
import java.util.Calendar

class dt_homeFragment : Fragment() {

    private var _binding: FragmentDtHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DtHomeViewModel by viewModels()

    private val args by navArgs<dt_homeFragmentArgs>()
    private var doctorCode: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        doctorCode = args.doctorCode
        Log.d("dt_homeFragment", "doctorCode: $doctorCode")
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
        setupNotification()

//        binding.cardPatientManage.setOnClickListener {
//            val activity = requireActivity() as? DtHomeActivity
//            activity?.binding?.bottomNav?.selectedItemId = com.example.winsrehab.R.id.dt_pt_manageFragment
//        }
//
//        binding.cardMyInfo.setOnClickListener {
//            val activity = requireActivity() as? DtHomeActivity
//            activity?.binding?.bottomNav?.selectedItemId = com.example.winsrehab.R.id.dt_info_Fragment
//        }
    }
    
    private fun setupNotification() {
        // 观察待处理申请数量
        viewModel.getPendingTaskCount(doctorCode)
        viewModel.pendingCount.observe(viewLifecycleOwner) { count ->
            if (count > 0) {
                binding.layoutNotification.visibility = View.VISIBLE
                binding.tvNotification.text = "您有 $count 个新患者申请"
            } else {
                binding.layoutNotification.visibility = View.GONE
            }
        }
        
        // 点击查看申请列表
        binding.btnViewApplications.setOnClickListener {
            val intent = Intent(requireContext(), PatientApplicationActivity::class.java)
            intent.putExtra("doctorCode", doctorCode)
            startActivity(intent)
        }
        
        binding.layoutNotification.setOnClickListener {
            val intent = Intent(requireContext(), PatientApplicationActivity::class.java)
            intent.putExtra("doctorCode", doctorCode)
            startActivity(intent)
        }
    }

    private fun setupGreeting() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = when {
            hour in 6..11 -> "早上好"
            hour in 12..13 -> "中午好"
            hour in 14..17 -> "下午好"
            else -> "晚上好"
        }
        binding.tvGreeting.text = greeting
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
