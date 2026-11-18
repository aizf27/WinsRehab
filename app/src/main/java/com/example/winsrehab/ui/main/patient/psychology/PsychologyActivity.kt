package com.example.winsrehab.ui.main.patient.psychology

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.winsrehab.R
import com.example.winsrehab.databinding.ActivityPsychologyBinding
import com.example.winsrehab.ui.common.UiTexts
import com.example.winsrehab.ui.main.patient.adapter.ChatAdapter

class PsychologyActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPsychologyBinding
    private val adapter = ChatAdapter()
    private val viewModel = PsychologyVM()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPsychologyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.topBar.tvTopBarTitle.setText(R.string.page_psychology_title)
        binding.topBar.btnTopBarBack.setOnClickListener { finish() }
        binding.rvChat.layoutManager = LinearLayoutManager(this)
        binding.rvChat.adapter = adapter
        binding.stateChatLoading.tvStateLoading.text = UiTexts.loadingText()
        binding.stateChatEmpty.tvStateEmpty.setText(R.string.psychology_empty)
        binding.stateChatError.btnStateRetry.text = UiTexts.retryText()
        binding.stateChatError.btnStateRetry.setOnClickListener { viewModel.retry() }

        viewModel.uiState.observe(this) { state ->
            adapter.submitList(state.messages)
            renderState(state)
        }
        binding.btnSend.setOnClickListener {
            val message = binding.etMessage.text.toString()
            if (message.isNotBlank()) {
                binding.etMessage.text?.clear()
                viewModel.sendMessage(message)
            }
        }
    }

    private fun renderState(state: PsychologyUiState) {
        binding.etMessage.isEnabled = !state.isLoading
        binding.btnSend.isEnabled = !state.isLoading
        binding.stateChatLoading.root.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        binding.stateChatError.root.visibility = if (state.errorReason != null) View.VISIBLE else View.GONE
        val showEmpty = !state.isLoading && state.errorReason == null && state.messages.isEmpty()
        binding.stateChatEmpty.root.visibility = if (showEmpty) View.VISIBLE else View.GONE
        val showList = !state.isLoading && state.errorReason == null && state.messages.isNotEmpty()
        binding.rvChat.visibility = if (showList) View.VISIBLE else View.GONE
        if (state.errorReason != null) {
            binding.stateChatError.tvStateError.text = UiTexts.errorText(state.errorReason)
        }
        if (showList) binding.rvChat.scrollToPosition(state.messages.lastIndex)
    }
}
