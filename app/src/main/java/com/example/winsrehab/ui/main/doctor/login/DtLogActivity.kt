package com.example.winsrehab.ui.main.doctor.login

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Html
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.winsrehab.databinding.ActivityDtLogBinding
import com.example.winsrehab.ui.main.doctor.home.DtHomeActivity
import com.example.winsrehab.ui.main.doctor.info.DtInfoActivity

class DtLogActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDtLogBinding
    private lateinit var preferences: SharedPreferences

    private val viewModel: DtLogVM by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDtLogBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()

        preferences = getSharedPreferences("doctor_prefs", MODE_PRIVATE)
        //给“医生登陆”设置下划线



        //恢复记住的账号密码
        val savedId = preferences.getString("doctor_id", "")
        val savedPassword = preferences.getString("doctor_password", "")
        val remember = preferences.getBoolean("remember", false)
        binding.etDoctorId.setText(savedId)
        if (remember) {
            binding.etDoctorPassword.setText(savedPassword)
            binding.cbRememberPassword.isChecked = true
        }

        //登录结果观察
        viewModel.loginResult.observe(this) { result ->
            when (result) {
                0 -> Toast.makeText(this, "账号不存在，请先注册", Toast.LENGTH_SHORT).show()
                1 -> {
                    Toast.makeText(this, "登录成功", Toast.LENGTH_SHORT).show()
                    saveLoginInfo()     //保存登录信息（要不要记住密码）
                    viewModel.checkInfoComplete(binding.etDoctorId.text.toString())
                }
                2 -> Toast.makeText(this, "密码错误", Toast.LENGTH_SHORT).show()
            }
        }

        //注册结果观察
        viewModel.registerResult.observe(this) { success ->
            if (success) Toast.makeText(this, "注册成功，请登录", Toast.LENGTH_SHORT).show()
            else Toast.makeText(this, "注册失败或账号已存在", Toast.LENGTH_SHORT).show()
        }

        //信息完整状态观察
        viewModel.infoComplete.observe(this) { complete ->
            val id = binding.etDoctorId.text.toString()
            val intent = if (complete) Intent(this, DtHomeActivity::class.java)
            else Intent(this, DtInfoActivity::class.java)
            intent.putExtra("doctorCode", id)
            startActivity(intent)
            finish()
        }

        //登录按钮
        binding.btnDoctorLogin.setOnClickListener {
            val id = binding.etDoctorId.text.toString().trim()
            val password = binding.etDoctorPassword.text.toString().trim()
            if (id.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "请输入工号和密码", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.loginDoctor(id, password)
        }

        //注册按钮
        binding.btnDoctorRegister.setOnClickListener {
            val id = binding.etDoctorId.text.toString().trim()
            val password = binding.etDoctorPassword.text.toString().trim()
            if (id.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "请输入工号和密码", Toast.LENGTH_SHORT).show()
                //标签返回，让函数从这返回
                return@setOnClickListener
            }
            viewModel.registerDoctor(id, password)
        }

        //EdgeToEdge
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun saveLoginInfo() {
        val editor = preferences.edit()
        val id = binding.etDoctorId.text.toString()
        val password = binding.etDoctorPassword.text.toString()
        val remember = binding.cbRememberPassword.isChecked
        editor.putString("doctor_id", id)
        editor.putBoolean("remember", remember)
        if (remember) editor.putString("doctor_password", password)
        else editor.remove("doctor_password")
        editor.apply()
    }
}
