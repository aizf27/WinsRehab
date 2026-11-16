package com.example.winsrehab.ui.main.selectLog

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.example.winsrehab.databinding.ActivitySelectLogBinding
import com.example.winsrehab.ui.main.doctor.login.DtLogActivity
import com.example.winsrehab.ui.main.patient.login.PtLogActivity
import com.example.winsrehab.ui.main.selectLog.SelectLogVM.NavigationDestination

class SelectLogActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySelectLogBinding
    private val viewModel: SelectLogVM by viewModels {
        ViewModelProvider.AndroidViewModelFactory.getInstance(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelectLogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        // 医生与患者两张入口卡片，整卡可点
        binding.cardDoctorLogin.setOnClickListener {
            viewModel.onDoctorLoginClicked()
        }

        binding.cardPatientLogin.setOnClickListener {
            viewModel.onPatientLoginClicked()
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun observeViewModel() {
        viewModel.navigationEvent.observe(this) { destination ->
            when (destination) {
                NavigationDestination.DOCTOR_LOGIN -> {

                     startActivity(Intent(this, DtLogActivity::class.java))
                }
                NavigationDestination.PATIENT_LOGIN -> {

                     startActivity(Intent(this, PtLogActivity::class.java))
                }
            }
        }
    }
}
