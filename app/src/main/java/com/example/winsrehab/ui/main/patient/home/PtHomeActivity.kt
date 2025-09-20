package com.example.winsrehab.ui.main.patient.home

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.winsrehab.databinding.ActivityPtHomeBinding

class PtHomeActivity: AppCompatActivity() {
    private lateinit var binding: ActivityPtHomeBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPtHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}