package com.example.winsrehab.ui.main.doctor.home

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.winsrehab.R
import com.example.winsrehab.data.entity.Doctor
import com.example.winsrehab.databinding.FragmentDtInfoBinding
import com.example.winsrehab.ui.main.doctor.info.DtInfoVM


private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [dt_info_Fragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class dt_info_Fragment : Fragment() {

    private var param1: String? = null
    private var param2: String? = null
    //binding
    private var _binding: FragmentDtInfoBinding?=null
    private val binding get()=_binding!!
    //参数
    private val args by navArgs<dt_info_FragmentArgs>()
    private var doctorCode: String=""
    private var totalCount:Int =0
    //VM
    private val viewModel: DtInfoVM by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 直接使用导航参数
        doctorCode = args.doctorCode
        totalCount = args.totalCount

        Log.d("dt_info_Fragment", "doctorCode: $doctorCode")
        Log.d("dt_info_Fragment", "totalCount: $totalCount")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding= FragmentDtInfoBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 观察医生数据
        viewModel.doctor.observe(viewLifecycleOwner) { doctor ->
            doctor?.let {
                fillUI(it)
            }
        }

        // 加载医生信息
        viewModel.loadDoctorInfo(doctorCode)

        // 编辑按钮 - 跳转到编辑页面
        binding.btnSave.setOnClickListener {
            val action = dt_info_FragmentDirections.actionDtInfoFragmentToDtInfoEditFragment(doctorCode)
            findNavController().navigate(action)
        }

        // 点击卡片 - 跳转到编辑页面
        binding.profileCard.setOnClickListener {
            val action = dt_info_FragmentDirections.actionDtInfoFragmentToDtInfoEditFragment(doctorCode)
            findNavController().navigate(action)
        }
    }
    
    /**
     * 填充UI数据
     */
    private fun fillUI(doctor: Doctor) {
        // 顶部卡片信息
        binding.tvDoctorName.text = doctor.name.takeIf { it != "未设置" } ?: "未设置"
        
        // 性别、年龄、科室
        val info = buildString {
            if (doctor.gender != "未设置") {
                append(doctor.gender)
            }
            if (doctor.age > 0) {
                if (isNotEmpty()) append(" · ")
                append("${doctor.age}岁")
            }
            if (doctor.department != "未设置") {
                if (isNotEmpty()) append(" · ")
                append(doctor.department)
            }
            if (isEmpty()) append("未设置")
        }
        binding.tvDoctorInfo.text = info
        
        // 工号
        binding.tvDoctorCode.text = "工号：${doctor.doctorCode}"
        
        // 执业信息
        binding.tvLicenseNumber.text = doctor.licenseNumber.takeIf { it != "未设置" } ?: "未设置"
        binding.tvHospital.text = doctor.hospital.takeIf { it != "未设置" } ?: "未设置"
        binding.tvTitle.text = doctor.title.takeIf { it != "未设置" } ?: "未设置"
        binding.tvPatientCount.text = "${doctor.patientCount} 人"
        
        // 联系方式
        binding.tvPhone.text = doctor.phone.takeIf { it != "未设置" } ?: "未设置"
        binding.tvEmail.text = doctor.email.takeIf { it != "未设置" } ?: "未设置"
        
        // 工作统计
        binding.tvMonthlyPlans.text = doctor.monthlyCompletedPlans.toString()
        binding.tvSatisfaction.text = "${doctor.satisfactionRate.toInt()}%"
    }
    
    override fun onResume() {
        super.onResume()
        // 每次返回时重新加载数据，确保显示最新信息
        viewModel.loadDoctorInfo(doctorCode)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        _binding=null
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment dt_info_Fragment.
         */

        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            dt_info_Fragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}