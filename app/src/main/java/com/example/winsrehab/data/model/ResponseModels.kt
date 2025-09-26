package com.example.winsrehab.data.model
//响应Body
//                {
//                    "choices": [
//                    {
//                        "message": {
//                        "role": "assistant",
//                        "content": "Hello! How can I help you today? 😊"
//                        }
//                    }
//  ]
////                }
data class ResponseBody(
    val choices: List<Choice>
)

data class Choice(
    val message: MessageItem
)