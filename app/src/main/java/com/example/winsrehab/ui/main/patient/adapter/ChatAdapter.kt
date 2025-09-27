package com.example.winsrehab.ui.main.patient.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.winsrehab.R
import com.example.winsrehab.data.entity.ChatMessage

class ChatAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    val chatList: MutableList<ChatMessage> = mutableListOf()

    fun addMessage(message: ChatMessage) {
        chatList.add(message)
        notifyItemInserted(chatList.size - 1)
    }
    companion object {
        const val type_user = 0
        const val type_bai = 1
    }

    //创建视图,绑item
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {

            type_user -> UserViewHolder(inflater, parent)
            type_bai -> BaiViewHolder(inflater, parent)
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }
    class UserViewHolder(inflater: LayoutInflater, parent: ViewGroup) :
        RecyclerView.ViewHolder(inflater.inflate(R.layout.item_chat_user, parent, false)){
        val tvMessage = itemView.findViewById<TextView>(R.id.tvMessage)
        }
    class BaiViewHolder(inflater: LayoutInflater, parent: ViewGroup) :
        RecyclerView.ViewHolder(inflater.inflate(R.layout.item_chat_ai, parent, false)){
            val tvMessage = itemView.findViewById<TextView>(R.id.tvMessage)
        }

    override fun getItemViewType(position: Int): Int {
        return when (chatList[position].type) {
            ChatMessage.MessageType.User -> type_user
            ChatMessage.MessageType.AI -> type_bai
        }
    }
    //绑定数据
    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        val context=chatList[ position].content
        when(holder){
            is UserViewHolder->holder.tvMessage.text=context
            is BaiViewHolder->holder.tvMessage.text=context
        }
    }
    //获取数据个数
    override fun getItemCount(): Int = chatList.size







}