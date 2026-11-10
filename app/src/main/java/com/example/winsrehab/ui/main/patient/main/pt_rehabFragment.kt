package com.example.winsrehab.ui.main.patient.main

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.winsrehab.data.model.TaskInstanceWithTask
import com.example.winsrehab.databinding.FragmentPtRehabBinding
import com.example.winsrehab.ui.main.patient.adapter.TaskInstanceAdapter
import kotlinx.coroutines.launch

/**
 * 患者康复页：全部数据来自 ViewModel，不再写死模拟数据
 */
class pt_rehabFragment : Fragment() {

    private var _binding: FragmentPtRehabBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PtRehabViewModel by viewModels()
    private lateinit var adapter: TaskInstanceAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPtRehabBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecycler()
        binding.btnRetry.setOnClickListener { viewModel.retry() }

        // BottomNav 切页不带 nav 参数，account 由 resolveAccount 回落宿主 Intent
        viewModel.load(resolveAccount())
        observeState()
    }

    private fun setupRecycler() {
        adapter = TaskInstanceAdapter { item -> showTaskDialog(item) }
        binding.rvTodayTasks.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTodayTasks.adapter = adapter
    }

    // 与 pt_infoFragment.resolveAccount 同款兜底链
    private fun resolveAccount(): String {
        val navAccount = arguments?.getString("account") ?: ""
        val intentAccount = requireActivity().intent.getStringExtra("account") ?: ""
        return navAccount.ifEmpty { intentAccount }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 重新进入 STARTED 会重新订阅，today/weekRange 随之重算
                viewModel.uiState.collect { render(it) }
            }
        }
    }

    private fun render(state: RehabUiState) {
        when (state) {
            is RehabUiState.Loading -> showLoading()
            is RehabUiState.Content -> showContent(state)
            is RehabUiState.Error -> showError(state.message)
        }
    }

    private fun showLoading() {
        binding.loadingView.visibility = View.VISIBLE
        binding.rvTodayTasks.visibility = View.GONE
        binding.tvEmpty.visibility = View.GONE
        binding.errorView.visibility = View.GONE
    }

    private fun showContent(state: RehabUiState.Content) {
        binding.loadingView.visibility = View.GONE
        binding.errorView.visibility = View.GONE
        binding.rvTodayTasks.visibility = View.VISIBLE
        binding.tvEmpty.text = RehabFormatter.emptyText()
        binding.tvEmpty.visibility = if (state.todayTasks.isEmpty()) View.VISIBLE else View.GONE

        adapter.submitList(state.todayTasks)

        binding.tvProgressCount.text = RehabFormatter.progressCountText(state.todayCompleted, state.todayTotal)
        binding.tvProgressPercent.text = RehabFormatter.progressPercentText(state.todayCompleted, state.todayTotal)
        binding.progressBarToday.progress = RehabFormatter.percent(state.todayCompleted, state.todayTotal)

        renderWeekBars(state)
        renderStatCards(state)
    }

    private fun renderWeekBars(state: RehabUiState.Content) {
        val bars = listOf(
            binding.weekBar1, binding.weekBar2, binding.weekBar3, binding.weekBar4,
            binding.weekBar5, binding.weekBar6, binding.weekBar7
        )
        state.weekBars.forEachIndexed { index, bar ->
            if (index >= bars.size) return@forEachIndexed
            val item = bars[index]
            item.tvDay.text = RehabFormatter.dayLabel(bar)
            item.tvCount.text = RehabFormatter.weekCountText(bar)
            val heightPx = (RehabFormatter.barHeightDp(bar.completed, bar.total) * resources.displayMetrics.density).toInt()
            val params = item.viewBarFill.layoutParams as FrameLayout.LayoutParams
            params.height = heightPx
            item.viewBarFill.layoutParams = params
        }
    }

    private fun renderStatCards(state: RehabUiState.Content) {
        binding.statCard1.tvStatLabel.text = "累计训练"
        binding.statCard1.tvStatValue.text = RehabFormatter.placeholderText()
        binding.statCard2.tvStatLabel.text = "本周完成"
        binding.statCard2.tvStatValue.text = "${state.weekCompleted}/${state.weekTotal}"
        binding.statCard3.tvStatLabel.text = "连续打卡"
        binding.statCard3.tvStatValue.text = RehabFormatter.placeholderText()
    }

    private fun showError(message: String) {
        binding.loadingView.visibility = View.GONE
        binding.rvTodayTasks.visibility = View.GONE
        binding.tvEmpty.visibility = View.GONE
        binding.errorView.visibility = View.VISIBLE
        binding.tvError.text = RehabFormatter.errorText(message)
    }

    private fun showTaskDialog(item: TaskInstanceWithTask) {
        AlertDialog.Builder(requireContext())
            .setTitle(item.taskName)
            .setMessage("请选择本次操作")
            .setPositiveButton("完成") { _, _ -> viewModel.completeTask(item.instance.instanceId) }
            .setNegativeButton("跳过") { _, _ -> viewModel.skipTask(item.instance.instanceId) }
            .setNeutralButton("取消", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
