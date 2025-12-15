package com.example.winsrehab.ui.main.doctor.home

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels

import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.winsrehab.databinding.FragmentDtHomeBinding
import com.example.winsrehab.ui.main.doctor.adapter.PatientAdapter
import kotlin.getValue

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [dt_homeFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class dt_homeFragment : Fragment() {

    private var _binding: FragmentDtHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DtHomeVM by viewModels()
    private val args by navArgs<dt_homeFragmentArgs>() // 注意使用小写开头的Fragment类名
    private var doctorCode : String=""
    private var ptCount: Int=0

    private lateinit var adapter: PatientAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        doctorCode = args.doctorCode
        Log.d("dt_homeFragment", "doctorCode: $doctorCode")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentDtHomeBinding.inflate(inflater, container, false)
        // Inflate the layout for this fragment
        return  binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //初始化RecyclerView和Adapter
        adapter= PatientAdapter()
        binding.rvKeyPatients.layoutManager= LinearLayoutManager(requireContext())
        binding.rvKeyPatients.adapter=adapter

        //观察数据变化并更新UI
        viewModel.patients.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)  //把最新的患者列表交给adapter，触发DiffUtil检查变化并刷新
            //更新患者总数
            ptCount=adapter.itemCount
            binding.tvTotalValue.text = ptCount.toString()
            Log.d("DtHomeActivity", "PtCount: $ptCount")
        }

        // 监听医生信息更新通知
        parentFragmentManager.setFragmentResultListener("doctor_info_result", viewLifecycleOwner) { _, result ->
            val isUpdated = result.getBoolean("doctor_info_updated", false)
            if (isUpdated) {
                // 重新加载患者数据，确保显示最新信息
                viewModel.loadPatients(doctorCode)
                Log.d("dt_homeFragment", "医生信息已更新，重新加载患者数据")
            }
        }

        // 加载患者数据
        viewModel.loadPatients(doctorCode)
        binding.tvTotalValue.text = adapter.itemCount.toString()
    }

    // 优化：在页面可见时自动刷新数据，确保显示最新信息
    override fun onResume() {
        super.onResume()
        viewModel.loadPatients(doctorCode)
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null  // 释放ViewBinding，避免内存泄漏
    }

}