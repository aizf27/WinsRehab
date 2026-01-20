package com.example.winsrehab.ui.main.patient.login

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.winsrehab.databinding.ActivityPtLogBinding
import com.example.winsrehab.ui.main.patient.binding.WaitingBindingActivity
import com.example.winsrehab.ui.main.patient.main.PtMainActivity

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
        val titleText = "<u>患者登录</u>"
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
                    // 检查绑定状态
                    viewModel.checkBindingStatus(binding.etPtAccount.text.toString())
                }
                2-> Toast.makeText(this, "密码错误", Toast.LENGTH_SHORT).show()
            }
        }
        
        //绑定状态观察
        viewModel.bindingStatus.observe(this) { status ->
            val account = binding.etPtAccount.text.toString()
            when(status) {
                "unbound" -> {
                    // 未绑定，显示绑定对话框
                    showBindDoctorDialog(account)
                }
                "pending" -> {
                    // 待确认，跳转到等待页面
                    val intent = Intent(this, WaitingBindingActivity::class.java)
                    intent.putExtra("account", account)
                    startActivity(intent)
                    finish()
                }
                "active" -> {
                    // 已绑定，检查信息完整性
                    viewModel.checkInfoComplete(account)
                }
                "rejected" -> {
                    // 被拒绝，提示并允许重新绑定
                    AlertDialog.Builder(this)
                        .setTitle("绑定被拒绝")
                        .setMessage("医生已拒绝您的绑定申请，请重新绑定其他医生")
                        .setPositiveButton("重新绑定") { _, _ ->
                            showBindDoctorDialog(account)
                        }
                        .setCancelable(false)
                        .show()
                }
            }
        }
        
        //绑定结果观察
        viewModel.bindingResult.observe(this) { result ->
            when(result) {
                0 -> Toast.makeText(this, "医生工号不存在，请检查后重试", Toast.LENGTH_SHORT).show()
                1 -> {
                    Toast.makeText(this, "绑定申请已发送，等待医生确认", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, WaitingBindingActivity::class.java)
                    intent.putExtra("account", binding.etPtAccount.text.toString())
                    startActivity(intent)
                    finish()
                }
                2 -> Toast.makeText(this, "绑定失败，请稍后重试", Toast.LENGTH_SHORT).show()
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

            // 统一跳转到新的主界面
            val intent = Intent(this, PtMainActivity::class.java)
            intent.putExtra("account", account) //传递账号

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
        editor.putBoolean("remember", remember)

        if (remember) {editor.putString("PtPassword",password)}
        else {editor.remove("PtPassword")}
        editor.apply()
    }
    
    private fun showBindDoctorDialog(account: String) {
        val input = EditText(this)
        input.hint = "请输入医生工号"
        input.setPadding(50, 30, 50, 30)
        
        AlertDialog.Builder(this)
            .setTitle("绑定医生")
            .setMessage("首次登录需要绑定医生才能使用")
            .setView(input)
            .setPositiveButton("确定") { _, _ ->
                val doctorCode = input.text.toString().trim()
                if (doctorCode.isEmpty()) {
                    Toast.makeText(this, "医生工号不能为空", Toast.LENGTH_SHORT).show()
                    showBindDoctorDialog(account) // 重新显示对话框
                } else {
                    viewModel.bindDoctor(account, doctorCode)
                }
            }
            .setNegativeButton("取消") { _, _ ->
                Toast.makeText(this, "必须绑定医生才能使用", Toast.LENGTH_SHORT).show()
                finish()
            }
            .setCancelable(false)
            .show()
    }

}


