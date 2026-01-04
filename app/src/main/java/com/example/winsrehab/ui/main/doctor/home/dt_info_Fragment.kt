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

        viewModel.doctor.observe(viewLifecycleOwner) { doctor ->
            Log.d("dt_info_Fragment", "doctor: $doctor")
            if (doctor != null) {
                binding.tvCode.text = "工号：${doctor.id}"
                binding.etName.setText(doctor.name)
                binding.etAge.setText(doctor.age?.toString() ?: "")
                binding.etPtNum.setText(
                    if (doctor.patientCount == 0)
                        totalCount.toString()
                    else
                        doctor.patientCount.toString())
                binding.etGender.setText(doctor.gender)
                binding.etDepartment.setText(doctor.department)
            } else {
                Log.d("dt_info_Fragment", "doctor is null, using doctorCode: $doctorCode")
                binding.tvCode.text = "工号：$doctorCode"
                binding.etPtNum.setText(totalCount.toString())
            }
        }

        Log.d("dt_info_Fragment", "calling loadDoctorInfo with doctorCode: $doctorCode")
        viewModel.loadDoctorInfo(doctorCode)
        binding.btnSave.setOnClickListener { saveDoctorInfo() }


    }
    private fun saveDoctorInfo() {
        val name = binding.etName.text.toString().trim()
        val ageStr = binding.etAge.text.toString().trim()
        val ptNumStr = binding.etPtNum.text.toString().trim()
        val gender = binding.etGender.text.toString().trim()
        val department = binding.etDepartment.text.toString().trim()

        if (name.isEmpty() || ageStr.isEmpty() || ptNumStr.isEmpty() ||
            gender.isEmpty() || department.isEmpty()
        ) {
            Toast.makeText(requireContext(), "请填写完整信息", Toast.LENGTH_SHORT).show()
            return
        }

        val age = ageStr.toIntOrNull()
        val ptNum = ptNumStr.toIntOrNull()
        if (age == null || ptNum == null) {
            Toast.makeText(requireContext(), "年龄和患者数必须是数字", Toast.LENGTH_SHORT).show()
            return
        }
        val oldPassword = viewModel.doctor.value?.password ?: ""
        // 创建医生对象
        val doctor = Doctor(
            id = doctorCode,//别搞错
            password = oldPassword,
            name = name,
            gender = gender,
            age = age,
            department = department,
            patientCount = ptNum
        )

        viewModel.saveDoctorInfo(doctor) { success ->
            if (success) {
                Toast.makeText(requireContext(), "信息保存成功", Toast.LENGTH_SHORT).show()
                
                // 通过底部导航栏切换回主页，确保参数正确传递
                // 获取 Activity 中的底部导航栏并选中主页
                val activity = requireActivity() as? DtHomeActivity
                activity?.let {
                    it.binding.bottomNav.selectedItemId = R.id.dt_homeFragment
                }
            } else {
                Toast.makeText(requireContext(), "保存失败，请重试", Toast.LENGTH_SHORT).show()
            }
        }
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
        // TODO: Rename and change types and number of parameters
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