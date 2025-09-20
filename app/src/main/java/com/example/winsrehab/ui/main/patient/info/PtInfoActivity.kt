package com.example.winsrehab.ui.main.patient.info

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class PtInfoActivity: AppCompatActivity () {
    private lateinit var binding : ActivityPtInfoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPtInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

    }
}