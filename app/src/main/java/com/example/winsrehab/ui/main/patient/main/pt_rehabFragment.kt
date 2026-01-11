package com.example.winsrehab.ui.main.patient.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.winsrehab.R
import com.example.winsrehab.databinding.FragmentPtRehabBinding

class pt_rehabFragment : Fragment() {
    private var _binding: FragmentPtRehabBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPtRehabBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 初始化模拟数据
        setupTodayProgress()
        setupExerciseTasks()
        setupWeekBars()
        setupStatCards()
    }

    /**
     * 设置今日进度卡片数据
     */
    private fun setupTodayProgress() {
        binding.tvProgressCount.text = "2/4 项已完成"
        binding.tvProgressPercent.text = "完成率: 50%"
        binding.progressBarToday.progress = 50
    }

    /**
     * 设置训练任务列表数据
     */
    private fun setupExerciseTasks() {
        // 模拟数据
        data class TaskData(
            val name: String,
            val duration: String,
            val sets: String,
            val isCompleted: Boolean
        )

        val tasks = listOf(
            TaskData("上肢伸展运动", "10 分钟", "3 组 x 10 次", true),
            TaskData("手指灵活训练", "15 分钟", "2 组 x 15 次", true),
            TaskData("肩部旋转练习", "10 分钟", "3 组 x 8 次", false),
            TaskData("握力训练", "5 分钟", "4 组 x 12 次", false)
        )

        val taskViews = listOf(
            binding.task1.root,
            binding.task2.root,
            binding.task3.root,
            binding.task4.root
        )

        tasks.forEachIndexed { index, task ->
            setupTaskItem(taskViews[index], task.name, task.duration, task.sets, task.isCompleted)
        }
    }

    /**
     * 设置单个任务项
     */
    private fun setupTaskItem(
        taskView: View,
        name: String,
        duration: String,
        sets: String,
        isCompleted: Boolean
    ) {
        taskView.findViewById<TextView>(R.id.tvTaskName).text = name
        taskView.findViewById<TextView>(R.id.tvDuration).text = duration
        taskView.findViewById<TextView>(R.id.tvSets).text = sets

        val imgStatus = taskView.findViewById<ImageView>(R.id.imgStatus)
        val btnStart = taskView.findViewById<TextView>(R.id.btnStartTask)

        if (isCompleted) {
            imgStatus.setImageResource(R.drawable.ic_check_circle)
            imgStatus.setColorFilter(ContextCompat.getColor(requireContext(), R.color.green_600))
            btnStart.visibility = View.GONE
        } else {
            imgStatus.setImageResource(R.drawable.ic_circle_outline)
            imgStatus.setColorFilter(ContextCompat.getColor(requireContext(), R.color.gray_300))
            btnStart.visibility = View.VISIBLE
        }
    }

    /**
     * 设置本周柱状图数据
     */
    private fun setupWeekBars() {
        // 模拟数据：周一到周日的完成情况
        data class WeekData(val day: String, val completed: Int, val total: Int)

        val weekData = listOf(
            WeekData("周一", 4, 4),
            WeekData("周二", 3, 4),
            WeekData("周三", 4, 4),
            WeekData("周四", 2, 4),
            WeekData("周五", 3, 4),
            WeekData("周六", 1, 4),
            WeekData("周日", 2, 4)  // 今天
        )

        val barViews = listOf(
            binding.weekBar1.root,
            binding.weekBar2.root,
            binding.weekBar3.root,
            binding.weekBar4.root,
            binding.weekBar5.root,
            binding.weekBar6.root,
            binding.weekBar7.root
        )

        weekData.forEachIndexed { index, data ->
            setupWeekBar(barViews[index], data.day, data.completed, data.total)
        }
    }

    /**
     * 设置单个柱状图
     */
    private fun setupWeekBar(barView: View, day: String, completed: Int, total: Int) {
        barView.findViewById<TextView>(R.id.tvDay).text = day
        barView.findViewById<TextView>(R.id.tvCount).text = "$completed/$total"

        // 计算柱子高度（最大100dp）
        val maxHeight = 100
        val fillHeight = if (total > 0) (completed.toFloat() / total * maxHeight).toInt() else 0

        val viewBarFill = barView.findViewById<View>(R.id.viewBarFill)
        val params = viewBarFill.layoutParams as FrameLayout.LayoutParams
        params.height = (fillHeight * resources.displayMetrics.density).toInt()
        viewBarFill.layoutParams = params
    }

    /**
     * 设置底部统计卡片数据
     */
    private fun setupStatCards() {
        // 统计卡片1：累计训练
        setupStatCard(binding.statCard1.root, "累计训练", "28 天")

        // 统计卡片2：本周完成
        setupStatCard(binding.statCard2.root, "本周完成", "19/28")

        // 统计卡片3：连续打卡
        setupStatCard(binding.statCard3.root, "连续打卡", "7 天")
    }

    /**
     * 设置单个统计卡片
     */
    private fun setupStatCard(cardView: View, label: String, value: String) {
        cardView.findViewById<TextView>(R.id.tvStatLabel).text = label
        cardView.findViewById<TextView>(R.id.tvStatValue).text = value
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
