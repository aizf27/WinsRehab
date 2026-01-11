package com.example.winsrehab.ui.main.patient.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import com.example.winsrehab.databinding.FragmentPtHomeBinding
import com.example.winsrehab.ui.main.patient.Video.TrainingVideoActivity
import com.example.winsrehab.ui.main.patient.info.PtInfoVM
import com.example.winsrehab.ui.main.patient.psychology.PsychologyActivity
import java.util.Calendar

class pt_homeFragment : Fragment() {
    private var _binding: FragmentPtHomeBinding? = null
    private val binding get() = _binding!!
    private val args by navArgs<pt_homeFragmentArgs>()
    
    // 使用 activityViewModels 共享 Activity 的 ViewModel
    private val infoViewModel: PtInfoVM by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPtHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 设置问候语（根据当前时间）
        setupGreeting()

        // 观察患者信息，更新欢迎卡片
        infoViewModel.patient.observe(viewLifecycleOwner) { patient ->
            patient?.let {
                binding.tvUserName.text = it.name ?: "患者"
            }
        }

        // 跳到示范视频页
        binding.cardTraining.setOnClickListener {
            val intent = Intent(requireContext(), TrainingVideoActivity::class.java)
            startActivity(intent)
        }

        // 跳到心理助手页
        binding.cardPsychology.setOnClickListener {
            val intent = Intent(requireContext(), PsychologyActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * 根据当前时间设置问候语
     */
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
}
