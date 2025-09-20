package com.example.winsrehab.ui.main.patient.Video

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.winsrehab.databinding.ActivityVideoPlayerBinding

class VideoPlayerActivity: AppCompatActivity() {
    private lateinit var binding: ActivityVideoPlayerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)


    }
}