package com.example.winsrehab.ui.main.doctor.info

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.winsrehab.databinding.ActivityDtInfoBinding

class DtInfoActivity : AppCompatActivity() {
    private lateinit var  binding: ActivityDtInfoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDtInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

}