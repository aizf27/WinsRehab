package com.example.winsrehab.ui.main.patient.login

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
import com.example.winsrehab.data.model.BindingStatus
import com.example.winsrehab.databinding.ActivityPtLogBinding
import com.example.winsrehab.ui.common.BindDoctorDialog
import com.example.winsrehab.ui.common.TextField
import com.example.winsrehab.ui.main.patient.binding.WaitingBindingActivity
import com.example.winsrehab.ui.main.patient.main.PtMainActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder

// 患者登录页：手机号 + 密码登录，登录后按绑定状态分流（未绑定 / 待确认 / 已绑定 / 被拒绝）
class PtLogActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPtLogBinding
    private lateinit var preferences: SharedPreferences

    private val viewModel: PtLogVM by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPtLogBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()

        preferences = getSharedPreferences("patient_prefs", MODE_PRIVATE)

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

    // 输入项的标签、必填标记、占位与密码可见性统一配置
    private fun setupFields() {
        binding.fieldAccount.setLabel(getString(R.string.field_account))
        binding.fieldAccount.setRequired(true)
        binding.fieldAccount.setHint(getString(R.string.hint_account))
        binding.fieldAccount.editText().inputType = InputType.TYPE_CLASS_PHONE

        binding.fieldPassword.setLabel(getString(R.string.field_password))
        binding.fieldPassword.setRequired(true)
        binding.fieldPassword.setHint(getString(R.string.hint_password))
        binding.fieldPassword.setPasswordToggle(true)
        binding.fieldPassword.editText().inputType =
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

        // 输入即清除上一次的错误提示
        clearErrorWhileTyping(binding.fieldAccount)
        clearErrorWhileTyping(binding.fieldPassword)
    }

    // 恢复上次记住的账号密码
    private fun restoreSavedAccount() {
        val saveAccount = preferences.getString("PtAccount", "")
        val savePassword = preferences.getString("PtPassword", "")
        val remember = preferences.getBoolean("remember", false)

        binding.fieldAccount.editText().setText(saveAccount)
        if (remember) {
            binding.fieldPassword.editText().setText(savePassword)
            binding.cbRememberPassword.isChecked = true
        }
    }

    // 登录 / 绑定状态 / 绑定结果 / 注册 / 信息完整五类结果统一观察
    private fun observeResults() {
        viewModel.loginResult.observe(this) { result ->
            when (result) {
                0 -> binding.fieldAccount.setError(getString(R.string.error_account_not_found))
                1 -> {
                    Toast.makeText(this, R.string.toast_login_success, Toast.LENGTH_SHORT).show()
                    saveLogInfo()
                    // 登录成功后先查绑定状态，再决定下一步去哪
                    viewModel.checkBindingStatus(binding.fieldAccount.text())
                }
                2 -> binding.fieldPassword.setError(getString(R.string.error_password_wrong))
            }
        }

        viewModel.bindingStatus.observe(this) { status ->
            val account = binding.fieldAccount.text()
            Log.i("PtLogActivity", "绑定状态返回: status=$status, account=$account")
            when (status) {
                // 未绑定：弹框让患者输入医生工号
                BindingStatus.UNBOUND -> showBindDoctorDialog(account)
                // 待确认：进等待页，等待页自己轮询并展示三态
                BindingStatus.PENDING -> gotoWaiting(account)
                // 已绑定：检查资料是否完整后进主界面
                BindingStatus.ACTIVE -> viewModel.checkInfoComplete(account)
                // 被拒绝：提示后可重新申请
                BindingStatus.REJECTED -> showRejectedDialog(account)
                else -> Log.e("PtLogActivity", "未登记的绑定状态: $status")
            }
        }

        viewModel.bindingResult.observe(this) { result ->
            when (result) {
                0 -> Toast.makeText(this, R.string.error_doctor_code_not_found, Toast.LENGTH_SHORT).show()
                1 -> {
                    Toast.makeText(this, R.string.toast_bind_sent, Toast.LENGTH_SHORT).show()
                    gotoWaiting(binding.fieldAccount.text())
                }
                2 -> Toast.makeText(this, R.string.toast_bind_failed, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.registerResult.observe(this) { success ->
            val message = if (success) R.string.toast_register_success else R.string.toast_register_failed
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }

        viewModel.infoComplete.observe(this) {
            val account = binding.fieldAccount.text()
            Log.i("PtLogActivity", "资料完整，跳转患者主界面: account=$account")
            val intent = Intent(this, PtMainActivity::class.java)
            intent.putExtra("account", account)
            startActivity(intent)
            finish()
        }
    }

    // 登录与注册共用同一套校验，失败统一标红在输入框上
    private fun setupButtons() {
        binding.btnPtLogin.setOnClickListener {
            if (!validateFields()) return@setOnClickListener
            viewModel.loginPatient(binding.fieldAccount.text(), binding.fieldPassword.text())
        }

        binding.btnPtRegister.setOnClickListener {
            if (!validateFields()) return@setOnClickListener
            viewModel.registerPatient(binding.fieldAccount.text(), binding.fieldPassword.text())
        }
    }

    // 手机号与密码都必填，缺哪个标哪个，文案统一
    private fun validateFields(): Boolean {
        val accountEmpty = binding.fieldAccount.text().isBlank()
        val passwordEmpty = binding.fieldPassword.text().isBlank()
        val message = getString(R.string.error_login_fields_empty)

        binding.fieldAccount.setError(if (accountEmpty) message else null)
        binding.fieldPassword.setError(if (passwordEmpty) message else null)
        return !accountEmpty && !passwordEmpty
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

    // 记住密码与账号一起落到本地偏好里
    private fun saveLogInfo() {
        val editor = preferences.edit()
        val account = binding.fieldAccount.text()
        val password = binding.fieldPassword.text()
        val remember = binding.cbRememberPassword.isChecked

        editor.putString("PtAccount", account)
        editor.putBoolean("remember", remember)

        if (remember) editor.putString("PtPassword", password) else editor.remove("PtPassword")
        editor.apply()
    }

    // 进等待页并把账号带过去，等待页负责轮询与三态展示
    private fun gotoWaiting(account: String) {
        val intent = Intent(this, WaitingBindingActivity::class.java)
        intent.putExtra("account", account)
        startActivity(intent)
        finish()
    }

    // 绑定申请对话框：输入医生工号，必填校验失败不关闭弹窗
    private fun showBindDoctorDialog(account: String) {
        BindDoctorDialog.show(
            context = this,
            account = account,
            onBind = { patientAccount, doctorCode ->
                viewModel.bindDoctor(patientAccount, doctorCode)
            },
            onCancel = {
                Toast.makeText(this, R.string.bind_required_message, Toast.LENGTH_SHORT).show()
                finish()
            }
        )
    }

    // 绑定被拒绝提示：可以直接重新申请
    private fun showRejectedDialog(account: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.bind_rejected_title)
            .setMessage(R.string.bind_rejected_message)
            .setPositiveButton(R.string.bind_rejected_action) { _, _ ->
                showBindDoctorDialog(account)
            }
            .setCancelable(false)
            .show()
    }
}
