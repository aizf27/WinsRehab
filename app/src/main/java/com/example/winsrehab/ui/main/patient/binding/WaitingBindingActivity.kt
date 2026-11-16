package com.example.winsrehab.ui.main.patient.binding

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.ImageViewCompat
import android.content.res.ColorStateList
import com.example.winsrehab.R
import com.example.winsrehab.data.model.BindingStatus
import com.example.winsrehab.databinding.ActivityWaitingBindingBinding
import com.example.winsrehab.ui.common.BindDoctorDialog
import com.example.winsrehab.ui.common.StatusBadge
import com.example.winsrehab.ui.main.patient.login.PtLogActivity
import com.example.winsrehab.ui.main.patient.main.PtMainActivity

// 等待绑定页：同页展示「等待中 / 已通过 / 被拒绝」三态，出结果后由患者自己点按钮继续
class WaitingBindingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWaitingBindingBinding
    private val viewModel: WaitingBindingViewModel by viewModels()
    private val handler = Handler(Looper.getMainLooper())
    private var account: String = ""

    // 当前展示的状态，用来判断是否刚从等待中变成结果态（只提示一次）
    private var currentStatus: String = BindingStatus.PENDING

    private val refreshRunnable = object : Runnable {
        override fun run() {
            // 只有仍在等待时才继续轮询，出结果就停
            if (currentStatus == BindingStatus.PENDING) {
                viewModel.checkBindingStatus(account)
                handler.postDelayed(this, REFRESH_INTERVAL_MS)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWaitingBindingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        account = intent.getStringExtra("account") ?: ""
        Log.i("WaitingBindingActivity", "进入等待绑定页: account=$account")

        setupTopBar()
        setupButtons()
        renderState(currentStatus)

        // 观察绑定状态，任何一次返回都直接重绘三态
        viewModel.bindingStatus.observe(this) { status ->
            Log.i("WaitingBindingActivity", "绑定状态返回: $status")
            renderState(status)
        }

        // 被拒绝后重新申请的结果：成功后回到等待中并继续轮询
        viewModel.bindingResult.observe(this) { result ->
            when (result) {
                0 -> Toast.makeText(this, R.string.error_doctor_code_not_found, Toast.LENGTH_SHORT).show()
                1 -> {
                    Toast.makeText(this, R.string.toast_bind_sent, Toast.LENGTH_SHORT).show()
                    renderState(BindingStatus.PENDING)
                    startPolling()
                }
                2 -> Toast.makeText(this, R.string.toast_bind_failed, Toast.LENGTH_SHORT).show()
            }
        }

        // 启动定时刷新
        startPolling()

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    // 顶部栏：标题固定，返回按钮直接关闭本页
    private fun setupTopBar() {
        binding.topBar.tvTopBarTitle.setText(R.string.waiting_title)
        binding.topBar.btnTopBarBack.setOnClickListener { finish() }
    }

    // 主按钮与次按钮的具体动作随状态变化，这里统一分发
    private fun setupButtons() {
        binding.btnPrimary.setOnClickListener {
            when (currentStatus) {
                BindingStatus.PENDING -> {
                    viewModel.checkBindingStatus(account)
                    Toast.makeText(this, R.string.toast_refreshing, Toast.LENGTH_SHORT).show()
                }
                BindingStatus.ACTIVE -> gotoPatientHome()
                BindingStatus.REJECTED -> showBindDoctorDialog()
                else -> Log.e("WaitingBindingActivity", "未登记的绑定状态: $currentStatus")
            }
        }

        binding.btnSecondary.setOnClickListener { backToLogin() }
    }

    // 按状态切换图标、徽标、标题、说明、进度圈与按钮文案
    private fun renderState(status: String?) {
        val normalized = status ?: BindingStatus.PENDING
        val previous = currentStatus
        currentStatus = normalized

        StatusBadge.bindBinding(binding.statusBadge.tvStatusBadge, normalized)

        when (normalized) {
            BindingStatus.PENDING -> {
                bindVisual(R.drawable.ic_time, R.color.warning, R.color.warning_container)
                binding.tvStateTitle.setText(R.string.waiting_pending_title)
                binding.tvStateDesc.setText(R.string.waiting_pending_desc)
                binding.tvStateHint.setText(R.string.waiting_pending_hint)
                binding.tvStateHint.visibility = View.VISIBLE
                binding.progressBar.visibility = View.VISIBLE
                binding.btnPrimary.setText(R.string.action_refresh_status)
                binding.btnSecondary.visibility = View.VISIBLE
            }
            BindingStatus.ACTIVE -> {
                bindVisual(R.drawable.ic_check_circle, R.color.brand_patient_primary_deep, R.color.success_container)
                binding.tvStateTitle.setText(R.string.waiting_active_title)
                binding.tvStateDesc.setText(R.string.waiting_active_desc)
                binding.tvStateHint.visibility = View.GONE
                binding.progressBar.visibility = View.GONE
                binding.btnPrimary.setText(R.string.action_enter_home)
                binding.btnSecondary.visibility = View.GONE
                stopPolling()
                if (previous != BindingStatus.ACTIVE) {
                    Toast.makeText(this, R.string.toast_binding_approved, Toast.LENGTH_SHORT).show()
                }
            }
            BindingStatus.REJECTED -> {
                bindVisual(R.drawable.ic_circle_outline, R.color.warning, R.color.notice_container)
                binding.tvStateTitle.setText(R.string.waiting_rejected_title)
                binding.tvStateDesc.setText(R.string.waiting_rejected_desc)
                binding.tvStateHint.visibility = View.GONE
                binding.progressBar.visibility = View.GONE
                binding.btnPrimary.setText(R.string.action_apply_again)
                // 被拒绝时主按钮重新申请，次按钮回登录页
                binding.btnSecondary.setText(R.string.action_back_to_login)
                binding.btnSecondary.visibility = View.VISIBLE
                stopPolling()
                if (previous != BindingStatus.REJECTED) {
                    Toast.makeText(this, R.string.toast_binding_rejected, Toast.LENGTH_SHORT).show()
                }
            }
            else -> Log.e("WaitingBindingActivity", "未登记的绑定状态，按等待中渲染: $normalized")
        }
    }

    // 图标与其圆底配色一起换，保证三态视觉一致
    private fun bindVisual(iconRes: Int, tintRes: Int, containerRes: Int) {
        binding.ivStateIcon.setImageResource(iconRes)
        ImageViewCompat.setImageTintList(
            binding.ivStateIcon,
            ColorStateList.valueOf(ContextCompat.getColor(this, tintRes))
        )
        binding.ivStateIcon.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(this, containerRes))
    }

    // 已通过：带账号进患者主界面
    private fun gotoPatientHome() {
        Log.i("WaitingBindingActivity", "绑定已通过，进入患者主界面: account=$account")
        val intent = Intent(this, PtMainActivity::class.java)
        intent.putExtra("account", account)
        startActivity(intent)
        finish()
    }

    // 回登录页重新登录并申请绑定
    private fun backToLogin() {
        startActivity(Intent(this, PtLogActivity::class.java))
        finish()
    }

    // 被拒绝后重新申请：弹框输入医生工号，提交成功后回到等待中
    private fun showBindDoctorDialog() {
        BindDoctorDialog.show(
            context = this,
            account = account,
            onBind = { patientAccount, doctorCode ->
                viewModel.bindDoctor(patientAccount, doctorCode)
            }
        )
    }

    // 开始轮询：先清掉旧任务再挂，避免重复
    private fun startPolling() {
        handler.removeCallbacks(refreshRunnable)
        handler.post(refreshRunnable)
    }

    // 停止轮询，避免出结果后还在后台查库
    private fun stopPolling() {
        handler.removeCallbacks(refreshRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPolling()
    }

    private companion object {
        // 轮询间隔：30 秒
        const val REFRESH_INTERVAL_MS = 30000L
    }
}
