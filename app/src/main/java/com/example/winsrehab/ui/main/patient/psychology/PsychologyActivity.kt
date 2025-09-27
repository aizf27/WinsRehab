package com.example.winsrehab.ui.main.patient.psychology

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.winsrehab.databinding.ActivityDtHomeBinding
import com.example.winsrehab.databinding.ActivityPsychologyBinding
import com.example.winsrehab.ui.main.patient.adapter.ChatAdapter

class PsychologyActivity: AppCompatActivity () {
    private lateinit var binding: ActivityPsychologyBinding
    private val adapter = ChatAdapter()
    private val viewModle = PsychologyVM()
    object key {
        const val API_KEY = "sk-d6e708da4ac740d89600089d911f9a65"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPsychologyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //绑adapter
        binding.rvChat.layoutManager = LinearLayoutManager(this)
        binding.rvChat.adapter = adapter

        //观察rv数据
        viewModle.chatList.observe(this) {
            adapter.chatList.clear()
            adapter.chatList.addAll(it)
                adapter.notifyDataSetChanged()
        }


        binding.btnSend.setOnClickListener {
            val message = binding.etMessage.text.toString()
            if (message.isNotEmpty()) {
               binding.etMessage.text.clear()

                viewModle.addUserMessage(message)
                viewModle.requestAI(key.API_KEY)


            }
        }
    }
}