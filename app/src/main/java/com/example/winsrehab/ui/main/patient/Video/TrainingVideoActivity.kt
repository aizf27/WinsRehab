package com.example.winsrehab.ui.main.patient.Video

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.winsrehab.databinding.ActivityTrainingVideoBinding

class TrainingVideoActivity: AppCompatActivity() {
    private lateinit var binding: ActivityTrainingVideoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrainingVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}