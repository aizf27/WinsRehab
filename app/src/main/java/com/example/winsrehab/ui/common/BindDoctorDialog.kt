package com.example.winsrehab.ui.common

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import com.example.winsrehab.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

// 绑定医生弹窗：登录页与等待绑定页共用，输入医生工号后回调出去提交
object BindDoctorDialog {

    private const val TAG = "BindDoctorDialog"

    // onBind 返回后由调用方发请求；onCancel 为空表示取消时只关闭弹窗
    fun show(
        context: Context,
        account: String,
        onBind: (account: String, doctorCode: String) -> Unit,
        onCancel: (() -> Unit)? = null
    ) {
        val field = TextField(context)
        field.setLabel(context.getString(R.string.field_doctor_id))
        field.setRequired(true)
        field.setHint(context.getString(R.string.bind_dialog_hint))

        // 弹窗内容统一留出边距，避免输入框贴边
        val container = FrameLayout(context)
        val padding = context.resources.getDimensionPixelSize(R.dimen.space_24)
        container.setPadding(padding, padding, padding, 0)
        container.addView(
            field,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(R.string.bind_dialog_title)
            .setMessage(R.string.bind_dialog_message)
            .setView(container)
            .setPositiveButton(R.string.bind_dialog_confirm, null)
            .setNegativeButton(R.string.bind_dialog_cancel) { _, _ -> onCancel?.invoke() }
            .setCancelable(false)
            .create()

        // 手动接管正按钮：工号为空时留在弹窗里提示，不关闭
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val doctorCode = field.text().trim()
                if (doctorCode.isEmpty()) {
                    field.setError(context.getString(R.string.error_login_fields_empty))
                } else {
                    Log.i(TAG, "提交绑定申请: account=$account, doctorCode=$doctorCode")
                    dialog.dismiss()
                    onBind(account, doctorCode)
                }
            }
        }
        dialog.show()
    }
}
