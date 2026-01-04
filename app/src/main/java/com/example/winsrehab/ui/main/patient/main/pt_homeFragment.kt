package com.example.winsrehab.ui.main.patient.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.example.winsrehab.databinding.FragmentPtHomeBinding
import com.example.winsrehab.ui.main.patient.Video.TrainingVideoActivity
import com.example.winsrehab.ui.main.patient.psychology.PsychologyActivity

class pt_homeFragment : Fragment() {
    private var _binding: FragmentPtHomeBinding? = null
    private val binding get() = _binding!!
    private val args by navArgs<pt_homeFragmentArgs>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPtHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //跳到示范视频页
        binding.cardTraining.setOnClickListener {
            val intent = Intent(requireContext(), TrainingVideoActivity::class.java)
            startActivity(intent)
        }

        //跳到心理助手页
        binding.cardPsychology.setOnClickListener {
            val intent = Intent(requireContext(), PsychologyActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}