package com.example.winsrehab.ui.main.patient.Video

import android.R
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.winsrehab.databinding.ActivityTrainingVideoBinding
import com.example.winsrehab.ui.common.UiTexts
import com.example.winsrehab.ui.main.patient.adapter.DemoVideoAdapter

class TrainingVideoActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTrainingVideoBinding
    private lateinit var viewModel: TrainingVideoViewModel
    private lateinit var adapter: DemoVideoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrainingVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.topBar.tvTopBarTitle.setText(com.example.winsrehab.R.string.page_training_videos_title)
        binding.topBar.btnTopBarBack.setOnClickListener { finish() }
        binding.stateVideosLoading.tvStateLoading.text = UiTexts.loadingText()
        binding.stateVideosEmpty.tvStateEmpty.setText(com.example.winsrehab.R.string.training_videos_empty)
        binding.stateVideosError.btnStateRetry.text = UiTexts.retryText()

        viewModel = ViewModelProvider(this)[TrainingVideoViewModel::class.java]
        binding.stateVideosError.btnStateRetry.setOnClickListener { viewModel.retry() }
        adapter = DemoVideoAdapter(emptyList()) { video ->
            startActivity(Intent(this, VideoPlayerActivity::class.java).apply {
                putExtra("url", video.videoPath)
                putExtra("title", video.title)
            })
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        val categories = arrayOf("全部", "肩部", "腿部", "全身")
        binding.spinnerCategory.adapter = ArrayAdapter(this, R.layout.simple_spinner_item, categories).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                viewModel.setCategory(categories[position])
            }

            override fun onNothingSelected(parent: AdapterView<*>) = Unit
        }
        viewModel.videos.observe(this) { videos ->
            adapter.submitList(videos)
            showVideos(videos.isEmpty())
        }
        viewModel.loadError.observe(this) { reason ->
            if (reason != null) showError(reason)
        }
        binding.stateVideosLoading.root.visibility = View.VISIBLE
    }

    private fun showVideos(empty: Boolean) {
        binding.stateVideosLoading.root.visibility = View.GONE
        binding.stateVideosError.root.visibility = View.GONE
        binding.stateVideosEmpty.root.visibility = if (empty) View.VISIBLE else View.GONE
        binding.recyclerView.visibility = if (empty) View.GONE else View.VISIBLE
    }

    private fun showError(reason: String) {
        binding.stateVideosLoading.root.visibility = View.GONE
        binding.stateVideosEmpty.root.visibility = View.GONE
        binding.recyclerView.visibility = View.GONE
        binding.stateVideosError.tvStateError.text = UiTexts.errorText(reason)
        binding.stateVideosError.root.visibility = View.VISIBLE
    }
}
