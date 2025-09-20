package com.example.winsrehab.ui.main.doctor.home

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.winsrehab.databinding.ActivityDtHomeBinding

class DtHomeActivity: AppCompatActivity () {
    private lateinit var binding: ActivityDtHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDtHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

}