package com.example.winsrehab.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import com.example.winsrehab.R
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

// 统一输入框组件：把「标签 + 必填标记 + 输入控件 + 错误/辅助文案」四部分封在一个视图里，页面只传业务文案
class TextField @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val tvLabel: TextView
    private val tvRequired: TextView
    private val tilField: TextInputLayout
    private val etField: TextInputEditText

    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.widget_text_field, this, true)
        tvLabel = findViewById(R.id.tvFieldLabel)
        tvRequired = findViewById(R.id.tvFieldRequired)
        tilField = findViewById(R.id.tilField)
        etField = findViewById(R.id.etField)
    }

    // 标签文案；传空就整行隐藏，避免页面上留一条空标签
    fun setLabel(text: CharSequence?) {
        tvLabel.text = text ?: ""
        tvLabel.visibility = if (text.isNullOrEmpty()) GONE else VISIBLE
    }

    // 必填标记：红星常驻，哪个字段必填由页面声明
    fun setRequired(required: Boolean) {
        tvRequired.visibility = if (required) VISIBLE else GONE
    }

    // 辅助文案：常态提示（如「6 位数字」），出现错误时被错误文案顶掉
    fun setHelperText(text: CharSequence?) {
        tilField.helperText = text
    }

    // 错误文案：传 null 清除错误并恢复辅助文案
    fun setError(text: CharSequence?) {
        tilField.error = text
    }

    // 输入控件本体：inputType、密码可见性这类细节由页面直接拿控件设置
    fun editText(): TextInputEditText = etField

    // 当前输入值
    fun text(): String = etField.text?.toString().orEmpty()
}
