package com.example.winsrehab.data.entity

class ChatMessage {
    enum class MessageType {
        User,
        AI
    }
        val content: String
        val type: MessageType
        constructor(content: String, type: MessageType) {
            this.content = content
            this.type = type
        }

}