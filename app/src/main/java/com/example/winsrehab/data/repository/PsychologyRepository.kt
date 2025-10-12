package com.example.winsrehab.data.repository

import android.util.Log
import com.example.winsrehab.data.entity.ChatMessage
import com.example.winsrehab.data.model.MessageItem
import com.example.winsrehab.data.model.RequestBodyData
import com.example.winsrehab.data.model.ResponseBody
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody

class PsychologyRepository {

    private val client = OkHttpClient()
    private val gson = Gson()

    suspend fun requestAI(messages: List<ChatMessage>, apiKey: String): String =
        //放协程里，不然卡爆
        withContext(Dispatchers.IO){
        try {

            val reqMessages = mutableListOf<MessageItem>()
            //1.加入系统提示
                reqMessages.add(MessageItem("system", "You are a helpful assistant."))
            //2.把本地的历史消息（用户+AI）转换成 API 能识别的格式
            messages.forEach { msg ->
                val role = if (msg.type == ChatMessage.MessageType.User) "user" else "assistant"
                reqMessages.add(MessageItem(role, msg.content))
            }
            //构建请求体
            val requestBodyData = RequestBodyData(
                model = "deepseek-chat",
                messages = reqMessages
            )
            val jsonBody = gson.toJson(requestBodyData)
//            val body: RequestBody =
//                RequestBody.create("application/json".toMediaTypeOrNull(), jsonBody)
            val body = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), jsonBody)

            Log.d("PsychologyRepository", "requestAI请求体: $jsonBody")

            val request = Request.Builder()
                .url("https://api.deepseek.com/chat/completions")
                .header("Authorization", "Bearer $apiKey")
                .post(body)
                .build()
            //开始处理response
            val response=client.newCall(request).execute()

            val responseBody = response.body
            val reStr = responseBody?.string()
            responseBody?.close()


            Log.d("PsychologyRepository", "requestAI响应码: ${response.code}, 响应内容: $reStr")

            if (response.isSuccessful && !reStr.isNullOrEmpty()) {

                val responseBody = gson.fromJson(reStr, ResponseBody::class.java)
                val aiReply = responseBody.choices.firstOrNull()?.message?.content.orEmpty()

                if (aiReply.isNotEmpty()) {
                    Log.d("PsychologyRepository", "requestAI成功回复: $aiReply")
                    aiReply
                } else {
                    "" //没有AI回复就返回空字符串
                }
            } else {
                "" //请求不成功就返回空字符串
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d("PsychologyRepository", "requestAI异常: ${e.message}")
            "" //异常返回空字符串
        }


    }




}