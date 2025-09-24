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
import com.example.winsrehab.ui.main.patient.adapter.DemoVideoAdapter

class TrainingVideoActivity: AppCompatActivity() {
    private lateinit var binding: ActivityTrainingVideoBinding
    private lateinit var viewModel: TrainingVideoViewModel
    private lateinit var adapter: DemoVideoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrainingVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 初始化 ViewModel
        viewModel = ViewModelProvider(this)[TrainingVideoViewModel::class.java]

        adapter = DemoVideoAdapter(emptyList()) { video ->
            val intent = Intent(this, VideoPlayerActivity::class.java).apply {
                putExtra("url", video.videoPath)
                putExtra("title", video.title)
            }
            startActivity(intent)
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        // 分类 Spinner
        val categories = arrayOf("全部", "肩部", "腿部", "全身")
        binding.spinnerCategory.adapter = ArrayAdapter(
            this, R.layout.simple_spinner_item, categories
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        binding.spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                viewModel.setCategory(categories[pos])
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // 观察数据变化
        viewModel.videos.observe(this) { adapter.submitList(it) }
    }

}