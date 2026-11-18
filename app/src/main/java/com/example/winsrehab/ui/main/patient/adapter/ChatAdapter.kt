package com.example.winsrehab.ui.main.patient.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.winsrehab.data.entity.ChatMessage
import com.example.winsrehab.databinding.ItemChatAiBinding
import com.example.winsrehab.databinding.ItemChatUserBinding

class ChatAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var messages: List<ChatMessage> = emptyList()

    fun submitList(newMessages: List<ChatMessage>) {
        messages = newMessages
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_USER -> UserViewHolder(ItemChatUserBinding.inflate(inflater, parent, false))
            TYPE_AI -> AiViewHolder(ItemChatAiBinding.inflate(inflater, parent, false))
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun getItemViewType(position: Int): Int = when (messages[position].type) {
        ChatMessage.MessageType.User -> TYPE_USER
        ChatMessage.MessageType.AI -> TYPE_AI
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is UserViewHolder -> holder.binding.tvMessage.text = messages[position].content
            is AiViewHolder -> holder.binding.tvMessage.text = messages[position].content
        }
    }

    override fun getItemCount(): Int = messages.size

    class UserViewHolder(val binding: ItemChatUserBinding) : RecyclerView.ViewHolder(binding.root)

    class AiViewHolder(val binding: ItemChatAiBinding) : RecyclerView.ViewHolder(binding.root)

    private companion object {
        const val TYPE_USER = 0
        const val TYPE_AI = 1
    }
}
