package com.example.winsrehab.ui.main.patient.psychology

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.winsrehab.databinding.ActivityDtHomeBinding
import com.example.winsrehab.databinding.ActivityPsychologyBinding

class PsychologyActivity: AppCompatActivity () {
    private lateinit var binding: ActivityPsychologyBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPsychologyBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}