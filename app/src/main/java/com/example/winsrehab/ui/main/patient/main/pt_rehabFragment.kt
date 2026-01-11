package com.example.winsrehab.ui.main.patient.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import com.example.winsrehab.databinding.FragmentPtRehabBinding
import com.example.winsrehab.ui.main.patient.info.PtInfoVM

class pt_rehabFragment : Fragment() {
    private var _binding: FragmentPtRehabBinding? = null
    private val binding get() = _binding!!
    private val args by navArgs<pt_rehabFragmentArgs>()

    // 使用 activityViewModels 共享 Activity 的 ViewModel
    private val infoViewModel: PtInfoVM by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPtRehabBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 观察患者信息，更新顶部栏
        infoViewModel.patient.observe(viewLifecycleOwner) { patient ->
            patient?.let {
                renderTopBar(it.name, it.signature, it.progress)
            }
        }

        // 监听患者信息更新事件（从 pt_infoFragment 保存后触发）
        requireActivity().supportFragmentManager.setFragmentResultListener(
            "patient_info_result",
            viewLifecycleOwner
        ) { _, result ->
            val updatedName = result.getString("patient_name")
            val updatedSignature = result.getString("patient_signature")
            renderTopBar(
                updatedName ?: infoViewModel.patient.value?.name,
                updatedSignature ?: infoViewModel.patient.value?.signature,
                infoViewModel.patient.value?.progress
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun renderTopBar(name: String?, signature: String?, progress: Int?) {
        binding.tvNickname.text = name ?: "患者"
        //只展示个性签名，没有就给一条默认提示
        binding.tvSignature.text = signature
            ?.takeIf { it.isNotBlank() }
            ?: "这个人还没有写个性签名"

        // 可以在这里设置头像（如果有的话）
        // binding.ivAvatar.setImageResource(...)
    }
}