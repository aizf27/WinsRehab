package com.example.winsrehab.ui.main.doctor.home

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.winsrehab.databinding.FragmentDtPtManageBinding
import com.example.winsrehab.ui.main.doctor.adapter.PatientAdapter
import com.example.winsrehab.R
import kotlin.getValue

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class dt_pt_manageFragment : Fragment() {

    private var _binding: FragmentDtPtManageBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DtHomeVM by viewModels()
    private val args by navArgs<dt_pt_manageFragmentArgs>()
    private var doctorCode : String=""
    private var ptCount: Int=0

    private lateinit var adapter: PatientAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        doctorCode = args.doctorCode
        Log.d("dt_pt_manageFragment", "doctorCode: $doctorCode")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentDtPtManageBinding.inflate(inflater, container, false)
        return  binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter= PatientAdapter { patient ->
            val bundle = bundleOf(
                "account" to patient.account,
                "mode" to "doctor"
            )
            findNavController().navigate(R.id.action_dt_pt_manageFragment_to_pt_infoFragment, bundle)
        }
//        binding.rvKeyPatients.layoutManager= LinearLayoutManager(requireContext())
//        binding.rvKeyPatients.adapter=adapter
//
//        viewModel.patients.observe(viewLifecycleOwner) { list ->
//            adapter.submitList(list)
//            ptCount=adapter.itemCount
//            binding.tvTotalValue.text = ptCount.toString()
//        }

        parentFragmentManager.setFragmentResultListener("doctor_info_result", viewLifecycleOwner) { _, result ->
            val isUpdated = result.getBoolean("doctor_info_updated", false)
            if (isUpdated) {
                viewModel.loadPatients(doctorCode)
                Log.d("dt_pt_manageFragment", "医生信息已更新，重新加载患者数据")
            }
        }

        viewModel.loadPatients(doctorCode)
//        binding.tvTotalValue.text = adapter.itemCount.toString()
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
