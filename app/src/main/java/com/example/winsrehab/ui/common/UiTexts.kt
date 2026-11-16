package com.example.winsrehab.ui.common

// 三态与占位文案的唯一来源：全项目只在这里写这些句子，页面按需取用，不再各写一套
object UiTexts {

    // 全项目唯一占位符：没有数据来源的统计项统一显示它
    fun placeholderText(): String = "--"

    // 空态默认文案：沿用康复页原文案，上收后展示行为不变
    fun emptyText(): String = "今天还没有训练任务"

    // 错误态文案：必须带上失败原因，页面只传原因
    fun errorText(reason: String): String = "加载失败：$reason"

    // 加载态文案
    fun loadingText(): String = "加载中…"

    // 错误态重试入口文案
    fun retryText(): String = "重试"
}
