package com.example.winsrehab.ui.main.patient.home

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.winsrehab.databinding.ActivityPtHomeBinding
import com.example.winsrehab.ui.main.patient.Video.TrainingVideoActivity
import com.example.winsrehab.ui.main.patient.info.PtInfoActivity
import com.example.winsrehab.ui.main.patient.psychology.PsychologyActivity

class PtHomeActivity: AppCompatActivity() {
    private lateinit var binding: ActivityPtHomeBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPtHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnViewInfo.setOnClickListener({ view ->
            val intent: Intent = Intent(this, PtInfoActivity::class.java)
            //把 account 传过去
            val account = getIntent().getStringExtra("account")
            intent.putExtra("mode", "patient")
            intent.putExtra("account", account)
            startActivity(intent)
        })
        binding.cardTraining.setOnClickListener({ view ->
            val intent: Intent = Intent(this, TrainingVideoActivity::class.java)
            startActivity(intent)
        })
        binding.cardPsychology.setOnClickListener({ view ->
            val intent: Intent = Intent(this, PsychologyActivity::class.java)
            startActivity(intent)
        })
    }
}