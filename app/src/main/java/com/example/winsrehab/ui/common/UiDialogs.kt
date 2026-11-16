package com.example.winsrehab.ui.common

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.example.winsrehab.R
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder

// 三种对话框的统一封装：确认、单选、底部弹层；外观全部由 Widget.WinsRehab.Dialog 与主题渲染
object UiDialogs {

    // 确认式：标题 + 说明 + 主/次两个动作，只有点主动作才回调
    fun confirm(
        context: Context,
        title: CharSequence,
        message: CharSequence,
        confirmText: CharSequence = "确定",
        cancelText: CharSequence = "取消",
        onConfirm: () -> Unit
    ): AlertDialog = MaterialAlertDialogBuilder(context)
        .setTitle(title)
        .setMessage(message)
        .setPositiveButton(confirmText) { _, _ -> onConfirm() }
        .setNegativeButton(cancelText, null)
        .show()

    // 单选式：选项列表里挑一个，点中即回传下标与文案并关闭
    fun choose(
        context: Context,
        title: CharSequence,
        options: List<String>,
        checkedIndex: Int = -1,
        onChosen: (index: Int, text: String) -> Unit
    ): AlertDialog = MaterialAlertDialogBuilder(context)
        .setTitle(title)
        .setSingleChoiceItems(options.toTypedArray(), checkedIndex) { dialog, which ->
            onChosen(which, options[which])
            dialog.dismiss()
        }
        .setNegativeButton("取消", null)
        .show()

    // 底部弹层：统一标题区 + 内容容器，页面把自己的操作行塞进容器
    fun bottomSheet(
        context: Context,
        title: CharSequence?,
        content: View
    ): BottomSheetDialog {
        val dialog = BottomSheetDialog(context, R.style.ThemeOverlay_WinsRehab_BottomSheet)
        // 挂一个临时父容器再 inflate，避免根布局参数丢失
        val parent = FrameLayout(context)
        val sheet = LayoutInflater.from(context).inflate(R.layout.widget_dialog_sheet, parent, false)
        val tvTitle = sheet.findViewById<TextView>(R.id.tvSheetTitle)
        tvTitle.text = title
        tvTitle.visibility = if (title.isNullOrEmpty()) View.GONE else View.VISIBLE
        sheet.findViewById<FrameLayout>(R.id.sheetContent).addView(content)
        dialog.setContentView(sheet)
        return dialog.also { it.show() }
    }
}
