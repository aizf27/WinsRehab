package com.example.winsrehab.ui.main.patient.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.winsrehab.MyApp
import com.example.winsrehab.data.model.RehabWeekData
import com.example.winsrehab.data.model.TaskStatus
import com.example.winsrehab.data.repository.TaskExecutionRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * 患者康复页 ViewModel：只经 Repository 取数，动作入口也在这里
 */
class PtRehabViewModel : ViewModel() {

    private val repository = TaskExecutionRepository(MyApp.instance.database)

    private val _uiState = MutableStateFlow<RehabUiState>(RehabUiState.Loading)
    val uiState: StateFlow<RehabUiState> = _uiState.asStateFlow()

    private var patientId: String = ""
    private var observeJob: Job? = null

    // 由 Fragment 解析 account 后调用；空值直接进 Error 态，避免用空 id 查出「合法空态」
    fun load(patientId: String) {
        this.patientId = patientId
        observe()
    }

    fun retry() = observe()

    fun completeTask(instanceId: Long) {
        viewModelScope.launch {
            try {
                repository.completeTask(instanceId)
            } catch (e: Exception) {
                _uiState.value = RehabUiState.Error(e.message ?: "操作失败")
            }
        }
    }

    fun skipTask(instanceId: Long) {
        viewModelScope.launch {
            try {
                repository.skipTask(instanceId)
            } catch (e: Exception) {
                _uiState.value = RehabUiState.Error(e.message ?: "操作失败")
            }
        }
    }

    private fun observe() {
        observeJob?.cancel()
        if (patientId.isEmpty()) {
            _uiState.value = RehabUiState.Error("患者信息缺失")
            return
        }
        observeJob = viewModelScope.launch {
            _uiState.value = RehabUiState.Loading
            repository.observeRehabWeek(patientId)
                .catch { e -> _uiState.value = RehabUiState.Error(e.message ?: "加载失败") }
                .collect { data -> _uiState.value = buildContent(data) }
        }
    }

    // 今日列表 / 今日进度 / 7 桶 / 周汇总全部由同一份本周任务派生
    private fun buildContent(data: RehabWeekData): RehabUiState.Content {
        val todayTasks = data.tasks.filter { it.instance.scheduledDate == data.today }
        val todayCompleted = todayTasks.count { it.instance.status == TaskStatus.COMPLETED }

        val byDate = data.tasks.groupBy { it.instance.scheduledDate }
        val weekStart = LocalDate.parse(data.weekStart)
        val bars = (0..6).map { offset ->
            val date = weekStart.plusDays(offset.toLong()).toString()
            val dayTasks = byDate[date].orEmpty()
            WeekBarData(
                date = date,
                completed = dayTasks.count { it.instance.status == TaskStatus.COMPLETED },
                total = dayTasks.size,
                isToday = date == data.today
            )
        }

        return RehabUiState.Content(
            todayTasks = todayTasks,
            todayCompleted = todayCompleted,
            todayTotal = todayTasks.size,
            weekBars = bars,
            weekCompleted = bars.sumOf { it.completed },
            weekTotal = bars.sumOf { it.total }
        )
    }
}
