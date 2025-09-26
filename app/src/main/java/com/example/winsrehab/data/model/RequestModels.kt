package com.example.winsrehab.data.model
//请求 Body
//                {
//                    "model": "deepseek-chat",
//                        "messages": [
//                    {"role": "system", "content": "You are a helpful assistant."},
//                    {"role": "user", "content": "Hello!"}
//        ],
//                    "stream": false
//                }'
data class MessageItem(
    val role: String,   // "user" / "assistant" / "system"
    val content: String
)

data class RequestBodyData(
    val model: String,
    val messages: List<MessageItem>,
    val stream: Boolean = false
)