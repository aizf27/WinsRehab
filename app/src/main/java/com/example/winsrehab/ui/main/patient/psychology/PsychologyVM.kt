package com.example.winsrehab.ui.main.patient.psychology

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.winsrehab.data.entity.ChatMessage
import com.example.winsrehab.data.repository.PsychologyRepository
import kotlinx.coroutines.launch

data class PsychologyUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val errorReason: String? = null
)

class PsychologyVM : ViewModel() {
    private val repository = PsychologyRepository()
    private val _uiState = MutableLiveData(PsychologyUiState())
    val uiState: LiveData<PsychologyUiState> = _uiState

    fun sendMessage(message: String) {
        val current = _uiState.value ?: return
        if (message.isBlank() || current.isLoading) return
        requestReply(current.messages + ChatMessage(message, ChatMessage.MessageType.User))
    }

    fun retry() {
        val current = _uiState.value ?: return
        if (current.messages.isEmpty() || current.isLoading) return
        requestReply(current.messages)
    }

    private fun requestReply(messages: List<ChatMessage>) {
        _uiState.value = PsychologyUiState(messages = messages, isLoading = true)
        Log.i(TAG, "请求心理助手回复: messageCount=${messages.size}")
        viewModelScope.launch {
            val reply = repository.requestAI(messages)
            if (reply.isBlank()) {
                Log.e(TAG, "心理助手回复为空")
                _uiState.value = PsychologyUiState(
                    messages = messages,
                    errorReason = "暂时无法获取回复"
                )
                return@launch
            }
            _uiState.value = PsychologyUiState(
                messages = messages + ChatMessage(reply, ChatMessage.MessageType.AI)
            )
        }
    }

    private companion object {
        const val TAG = "PsychologyVM"
    }
}
