package com.example.winsrehab.ui.main.patient.main

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.winsrehab.MyApp
import com.example.winsrehab.data.model.RehabWeekData
import com.example.winsrehab.data.model.TaskStatus
import com.example.winsrehab.data.repository.TaskExecutionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * 患者康复页 ViewModel：只经 Repository 取数，动作入口也在这里
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PtRehabViewModel : ViewModel() {

    private val repository = TaskExecutionRepository(MyApp.instance.database)

    private val patientId = MutableStateFlow<String?>(null)
    private val refreshVersion = MutableStateFlow(0)
    val uiState = combine(patientId, refreshVersion) { id, _ -> id }
        .flatMapLatest { id ->
            if (id.isNullOrBlank()) {
                flowOf(RehabUiState.Error("\u60A3\u8005\u4FE1\u606F\u7F3A\u5931"))
            } else {
                repository.observeRehabWeek(id)
                    .map<RehabWeekData, RehabUiState> { buildContent(it) }
                    .catch { emit(RehabUiState.Error(it.message ?: "\u52A0\u8F7D\u5931\u8D25")) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RehabUiState.Loading)

    // 由 Fragment 解析 account 后调用；空值直接进 Error 态，避免用空 id 查出「合法空态」
    fun load(patientId: String) {
        if (patientId.isBlank()) {
            this.patientId.value = null
        } else if (this.patientId.value != patientId) {
            this.patientId.value = patientId
        }
    }

    fun retry() {
        refreshVersion.value++
    }

    fun completeTask(instanceId: Long) {
        viewModelScope.launch {
            try {
                repository.completeTask(instanceId)
            } catch (e: Exception) {
                Log.e("PtRehabViewModel", "complete task failed: $instanceId", e)
            }
        }
    }

    fun skipTask(instanceId: Long) {
        viewModelScope.launch {
            try {
                repository.skipTask(instanceId)
            } catch (e: Exception) {
                Log.e("PtRehabViewModel", "skip task failed: $instanceId", e)
            }
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
