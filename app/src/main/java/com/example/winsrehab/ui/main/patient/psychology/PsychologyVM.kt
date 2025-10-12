package com.example.winsrehab.ui.main.patient.psychology

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.winsrehab.data.entity.ChatMessage
import com.example.winsrehab.data.repository.PsychologyRepository
import kotlinx.coroutines.launch

class PsychologyVM: ViewModel () {
    private val repository = PsychologyRepository()

    private val _chatList = MutableLiveData<MutableList<ChatMessage>>(mutableListOf())
    val chatList: LiveData<MutableList<ChatMessage>> get() = _chatList

    fun addUserMessage(message: String) {
        val chatMessage = ChatMessage(message, ChatMessage.MessageType.User)
        _chatList.value?.add(chatMessage)
        _chatList.value = _chatList.value
    }

    fun addAIMessage(message: String) {
        val chatMessage = ChatMessage(message, ChatMessage.MessageType.AI)
        _chatList.value?.add(chatMessage)
        _chatList.value = _chatList.value   //触发LiveData更新
    }

    fun buildRequestMessages(): List<ChatMessage> = _chatList.value?.toList() ?: emptyList()


    fun requestAI(apiKey: String) {
        val messages = buildRequestMessages()
        viewModelScope.launch {
            val reply = repository.requestAI(messages, apiKey)
            addAIMessage(reply)
        }
    }






}


