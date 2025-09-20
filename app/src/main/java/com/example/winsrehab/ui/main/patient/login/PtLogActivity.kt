package com.example.winsrehab.ui.main.patient.login

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.winsrehab.databinding.ActivityPtLogBinding
import com.example.winsrehab.ui.main.patient.home.PtHomeActivity
import com.example.winsrehab.ui.main.patient.info.PtInfoActivity

class PtLogActivity: AppCompatActivity () {
    private lateinit var binding : ActivityPtLogBinding
    private lateinit var preferences: SharedPreferences
    private val viewModel: PtLogVM by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPtLogBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()

        preferences=getSharedPreferences("patient_prefs", MODE_PRIVATE)

        //给患者登陆设置下划线
        val titleText = "<u>医生登录</u>"
        binding.tvPtLoginTitle.text = Html.fromHtml(titleText, Html.FROM_HTML_MODE_LEGACY)

        //回复账号密码
        val saveAccount=preferences.getString("PtAccount", "")
        val savePassword=preferences.getString("PtPassword", "")
        val remember=preferences.getBoolean("remember", false)

        binding.etPtAccount.setText(saveAccount)
        if (remember){
            binding.etPtPassword.setText(savePassword)
            binding.cbRememberPassword.isChecked=true
        }

        //登陆结果观察
        viewModel.loginResult.observe(this){result->
            when(result){
                0-> Toast.makeText(this, "账号不存在，请先注册", Toast.LENGTH_SHORT).show()
                1->{
                    Toast.makeText(this, "登录成功", Toast.LENGTH_SHORT).show()
                    saveLogInfo()
                    viewModel.checkInfoComplete(binding.etPtAccount.text.toString())
                }
                2-> Toast.makeText(this, "密码错误", Toast.LENGTH_SHORT).show()
            }
        }
        //注册结果观察
        viewModel.registerResult.observe(this){success->
            if (success) Toast.makeText(this, "注册成功,请登录", Toast.LENGTH_SHORT).show()
            else Toast.makeText(this, "注册失败或账号已存在", Toast.LENGTH_SHORT).show()

        }
        //信息完善结果观察
        viewModel.infoComplete.observe(this){complete->
            val account=binding.etPtAccount.text.toString()

            val intent=if( complete) Intent(this, PtHomeActivity::class.java)
            else Intent(this, PtInfoActivity::class.java)

            intent.putExtra("account", account)
            Log.i("PtLogActivity", "account: $account")
            startActivity(intent)
            finish()
        }
        //登陆按钮
        binding.btnPtLogin.setOnClickListener {
            val account=binding.etPtAccount.text.toString()
            val password=binding.etPtPassword.text.toString()
            Log.i("PtLogActivity", "account: $account")
            Log.i("PtLogActivity", "password: $password")
            if (account.isEmpty() || password.isEmpty()){
                Toast.makeText(this, "手机号或密码不能为空", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
                viewModel.loginPatient(account, password)



        }
    //注册按钮
    binding.btnPtRegister.setOnClickListener {
        val account=binding.etPtAccount.text.toString()
        val password=binding.etPtPassword.text.toString()

        if (account.isEmpty() || password.isEmpty()){
            Toast.makeText(this, "手机号或密码不能为空", Toast.LENGTH_SHORT).show()
            return@setOnClickListener
        }
        viewModel.registerPatient(account, password)

    }
        //EdgeToEdge
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets

        }
    }
    private fun PtLogActivity.saveLogInfo() {
        val editor = preferences.edit()
        val account=binding.etPtAccount.text.toString()
        val password=binding.etPtPassword.text.toString()
        //获取是否记住密码
        val remember = binding.cbRememberPassword.isChecked

        editor.putString("PtAccount", account)
        editor.putString("PtPassword", password)

        if (remember) {editor.putString("PtPassword",password)}
        else {editor.remove("PtPassword")}
        editor.apply()
    }

}


