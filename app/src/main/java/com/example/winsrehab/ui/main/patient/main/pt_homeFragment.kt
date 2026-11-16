package com.example.winsrehab.ui.main.patient.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.winsrehab.databinding.FragmentPtHomeBinding
import com.example.winsrehab.ui.main.patient.Video.TrainingVideoActivity
import com.example.winsrehab.ui.main.patient.info.PtInfoVM
import com.example.winsrehab.ui.common.UiTexts
import com.example.winsrehab.ui.main.patient.adapter.TaskInstanceAdapter
import kotlinx.coroutines.launch
import com.example.winsrehab.ui.main.patient.psychology.PsychologyActivity
import java.util.Calendar

class pt_homeFragment : Fragment() {
    private var _binding: FragmentPtHomeBinding? = null
    private val binding get() = _binding!!
    // 使用 activityViewModels 共享 Activity 的 ViewModel
    private val infoViewModel: PtInfoVM by activityViewModels()
    private val rehabViewModel: PtRehabViewModel by activityViewModels()
    private lateinit var taskAdapter: TaskInstanceAdapter

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
        setupTaskList()
        observeRehabState()

        // 观察患者信息，更新欢迎卡片
        infoViewModel.patient.observe(viewLifecycleOwner) { patient ->
            patient?.let {
                binding.tvUserName.text = it.name
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

        binding.tvViewAllTasks.setOnClickListener { openRehab() }
        binding.cardRehab.setOnClickListener { openRehab() }
    }

    private fun setupTaskList() {
        taskAdapter = TaskInstanceAdapter { openRehab() }
        binding.rvHomeTasks.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHomeTasks.adapter = taskAdapter
    }

    private fun observeRehabState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                rehabViewModel.uiState.collect { renderRehab(it) }
            }
        }
    }

    // 首页与康复中心消费同一个宿主 ViewModel，数字与自然周任务保持同源
    private fun renderRehab(state: RehabUiState) {
        when (state) {
            is RehabUiState.Content -> {
                binding.tvWeekPercent.text = "${RehabFormatter.percent(state.weekCompleted, state.weekTotal)}%"
                binding.tvTotalDays.text = UiTexts.placeholderText()
                binding.tvTodayTasks.text = "${state.todayCompleted}/${state.todayTotal}"
                binding.tvWeekProgressPercent.text = RehabFormatter.progressPercentText(state.weekCompleted, state.weekTotal)
                binding.progressWeek.progress = RehabFormatter.percent(state.weekCompleted, state.weekTotal)
                binding.tvWeekCount.text = RehabFormatter.progressCountText(state.weekCompleted, state.weekTotal)
                taskAdapter.submitList(state.todayTasks.take(2))
                binding.tvHomeEmpty.text = UiTexts.emptyText()
                binding.tvHomeEmpty.visibility = if (state.todayTasks.isEmpty()) View.VISIBLE else View.GONE
                renderWeekBars(state)
            }
            is RehabUiState.Error -> {
                binding.tvWeekPercent.text = UiTexts.placeholderText()
                binding.tvTotalDays.text = UiTexts.placeholderText()
                binding.tvTodayTasks.text = UiTexts.placeholderText()
                binding.tvWeekProgressPercent.text = UiTexts.placeholderText()
                binding.progressWeek.progress = 0
                binding.tvWeekCount.text = UiTexts.errorText(state.message)
                taskAdapter.submitList(emptyList())
                binding.tvHomeEmpty.text = UiTexts.errorText(state.message)
                binding.tvHomeEmpty.visibility = View.VISIBLE
            }
            RehabUiState.Loading -> {
                binding.tvWeekPercent.text = UiTexts.placeholderText()
                binding.tvTotalDays.text = UiTexts.placeholderText()
                binding.tvTodayTasks.text = UiTexts.placeholderText()
                binding.tvWeekProgressPercent.text = UiTexts.placeholderText()
                binding.tvWeekCount.text = UiTexts.loadingText()
                binding.tvHomeEmpty.visibility = View.GONE
            }
        }
    }

    private fun renderWeekBars(state: RehabUiState.Content) {
        val bars = listOf(binding.homeWeekBar1, binding.homeWeekBar2, binding.homeWeekBar3, binding.homeWeekBar4, binding.homeWeekBar5, binding.homeWeekBar6, binding.homeWeekBar7)
        state.weekBars.forEachIndexed { index, bar ->
            val item = bars[index]
            item.tvDay.text = RehabFormatter.dayLabel(bar)
            item.tvCount.text = RehabFormatter.weekCountText(bar)
            val params = item.viewBarFill.layoutParams as FrameLayout.LayoutParams
            params.height = (RehabFormatter.barHeightDp(bar.completed, bar.total) * resources.displayMetrics.density).toInt()
            item.viewBarFill.layoutParams = params
        }
    }

    private fun openRehab() {
        findNavController().navigate(com.example.winsrehab.R.id.pt_rehabFragment)
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
        binding.tvRehabDay.text = UiTexts.placeholderText()
    }
}
