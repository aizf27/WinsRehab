package com.example.winsrehab.ui.main.doctor.login

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.winsrehab.R
import com.example.winsrehab.databinding.ActivityDtLogBinding
import com.example.winsrehab.ui.common.TextField
import com.example.winsrehab.ui.main.doctor.home.DtHomeActivity

// 医生登录页：工号 + 密码登录，失败提示统一走输入框内联错误
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

        setupFields()
        restoreSavedAccount()
        observeResults()
        setupButtons()

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    // 两个输入项的标签、必填标记、占位与密码可见性都在这里一次性配好
    private fun setupFields() {
        binding.fieldDoctorId.setLabel(getString(R.string.field_doctor_id))
        binding.fieldDoctorId.setRequired(true)
        binding.fieldDoctorId.setHint(getString(R.string.hint_doctor_id))

        binding.fieldDoctorPassword.setLabel(getString(R.string.field_password))
        binding.fieldDoctorPassword.setRequired(true)
        binding.fieldDoctorPassword.setHint(getString(R.string.hint_password))
        binding.fieldDoctorPassword.setPasswordToggle(true)
        binding.fieldDoctorPassword.editText().inputType =
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

        // 用户一开始输入就清掉上一次的校验错误，避免红字一直挂着
        clearErrorWhileTyping(binding.fieldDoctorId)
        clearErrorWhileTyping(binding.fieldDoctorPassword)
    }

    // 恢复上次记住的工号与密码
    private fun restoreSavedAccount() {
        val savedId = preferences.getString("doctor_id", "")
        val savedPassword = preferences.getString("doctor_password", "")
        val remember = preferences.getBoolean("remember", false)

        binding.fieldDoctorId.editText().setText(savedId)
        if (remember) {
            binding.fieldDoctorPassword.editText().setText(savedPassword)
            binding.cbRememberPassword.isChecked = true
        }
    }

    // 登录 / 注册 / 信息完整三类结果统一在这里观察
    private fun observeResults() {
        viewModel.loginResult.observe(this) { result ->
            when (result) {
                0 -> binding.fieldDoctorId.setError(getString(R.string.error_account_not_found))
                1 -> {
                    Toast.makeText(this, R.string.toast_login_success, Toast.LENGTH_SHORT).show()
                    // 保存登录信息（要不要记住密码）
                    saveLoginInfo()
                    viewModel.checkInfoComplete(binding.fieldDoctorId.text())
                }
                2 -> binding.fieldDoctorPassword.setError(getString(R.string.error_password_wrong))
            }
        }

        viewModel.registerResult.observe(this) { success ->
            val message = if (success) R.string.toast_register_success else R.string.toast_register_failed
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }

        viewModel.infoComplete.observe(this) {
            val id = binding.fieldDoctorId.text()
            Log.i("DtLogActivity", "登录成功，跳转医生主界面: doctorCode=$id")
            val intent = Intent(this, DtHomeActivity::class.java)
            intent.putExtra("doctorCode", id)
            startActivity(intent)
            finish()
        }
    }

    // 登录与注册共用同一套校验：字段为空就在对应输入框上标红
    private fun setupButtons() {
        binding.btnDoctorLogin.setOnClickListener {
            if (!validateFields()) return@setOnClickListener
            viewModel.loginDoctor(binding.fieldDoctorId.text(), binding.fieldDoctorPassword.text())
        }

        binding.btnDoctorRegister.setOnClickListener {
            if (!validateFields()) return@setOnClickListener
            viewModel.registerDoctor(binding.fieldDoctorId.text(), binding.fieldDoctorPassword.text())
        }
    }

    // 工号与密码都必填，缺哪个标哪个，文案统一
    private fun validateFields(): Boolean {
        val idEmpty = binding.fieldDoctorId.text().isBlank()
        val passwordEmpty = binding.fieldDoctorPassword.text().isBlank()
        val message = getString(R.string.error_login_fields_empty)

        binding.fieldDoctorId.setError(if (idEmpty) message else null)
        binding.fieldDoctorPassword.setError(if (passwordEmpty) message else null)
        return !idEmpty && !passwordEmpty
    }

    // 输入变化即清除该字段的错误提示
    private fun clearErrorWhileTyping(field: TextField) {
        field.editText().addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                field.setError(null)
            }
        })
    }

    // 记住密码开关与工号一起落到本地偏好里
    private fun saveLoginInfo() {
        val editor = preferences.edit()
        val id = binding.fieldDoctorId.text()
        val password = binding.fieldDoctorPassword.text()
        val remember = binding.cbRememberPassword.isChecked

        editor.putString("doctor_id", id)
        editor.putBoolean("remember", remember)

        if (remember) editor.putString("doctor_password", password) else editor.remove("doctor_password")
        editor.apply()
    }
}
