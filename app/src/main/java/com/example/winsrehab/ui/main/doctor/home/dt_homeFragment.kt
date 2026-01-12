package com.example.winsrehab.ui.main.doctor.home

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import com.example.winsrehab.databinding.FragmentDtHomeBinding
import java.util.Calendar

class dt_homeFragment : Fragment() {

    private var _binding: FragmentDtHomeBinding? = null
    private val binding get() = _binding!!

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

        binding.cardPatientManage.setOnClickListener {
            val activity = requireActivity() as? DtHomeActivity
            activity?.binding?.bottomNav?.selectedItemId = com.example.winsrehab.R.id.dt_pt_manageFragment
        }

        binding.cardMyInfo.setOnClickListener {
            val activity = requireActivity() as? DtHomeActivity
            activity?.binding?.bottomNav?.selectedItemId = com.example.winsrehab.R.id.dt_info_Fragment
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
