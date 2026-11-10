package com.example.winsrehab.data.model

// 全局状态常量：数据库里的 status 是字符串，所有取值集中在这里，避免各处手写字符串拼错
// 任务：pending → completed / skipped；pending → waiting_review → completed（通过）/ rejected（驳回）→ pending（重录）
object TaskStatus {
    const val PENDING = "pending"                 // 待完成（驳回后也回到这里重录）
    const val COMPLETED = "completed"             // 已完成（含医生审核通过）
    const val SKIPPED = "skipped"                 // 患者主动跳过
    const val WAITING_REVIEW = "waiting_review"   // 已提交视频，等医生审核
    const val REJECTED = "rejected"               // 医生驳回，可重录

    // 所有合法状态，落库前校验用
    val ALL: Set<String> = setOf(PENDING, COMPLETED, SKIPPED, WAITING_REVIEW, REJECTED)

    // 判断一个字符串是否是合法的任务状态
    fun isValid(value: String): Boolean = value in ALL
}

// 康复计划：draft → active → completed / cancelled
object PlanStatus {
    const val DRAFT = "draft"          // 草稿，还没开始生成任务
    const val ACTIVE = "active"        // 进行中，会按频率生成任务
    const val COMPLETED = "completed"  // 已完成，不再生成新任务
    const val CANCELLED = "cancelled"  // 已取消

    // 所有合法状态，落库前校验用
    val ALL: Set<String> = setOf(DRAFT, ACTIVE, COMPLETED, CANCELLED)

    // 判断一个字符串是否是合法的计划状态
    fun isValid(value: String): Boolean = value in ALL
}

// 视频审核：pending_review → approved / rejected
object VideoReviewStatus {
    const val PENDING_REVIEW = "pending_review"  // 等医生看
    const val APPROVED = "approved"              // 审核通过
    const val REJECTED = "rejected"              // 审核驳回

    // 所有合法状态，落库前校验用
    val ALL: Set<String> = setOf(PENDING_REVIEW, APPROVED, REJECTED)

    // 判断一个字符串是否是合法的审核状态
    fun isValid(value: String): Boolean = value in ALL
}

// 视频上传：local → queued → uploaded / failed
object UploadStatus {
    const val LOCAL = "local"        // 只存在手机本地，还没进队列
    const val QUEUED = "queued"      // 已进上传队列，等待上传
    const val UPLOADED = "uploaded"  // 上传完成（本地模式下是复制到 filesDir/uploads）
    const val FAILED = "failed"      // 上传失败，等重试

    // 所有合法状态，落库前校验用
    val ALL: Set<String> = setOf(LOCAL, QUEUED, UPLOADED, FAILED)

    // 判断一个字符串是否是合法的上传状态
    fun isValid(value: String): Boolean = value in ALL
}

// 医患绑定：unbound → pending → active / rejected
object BindingStatus {
    const val UNBOUND = "unbound"  // 还没绑定医生
    const val PENDING = "pending"  // 已申请，等医生确认
    const val ACTIVE = "active"    // 绑定成功，医生能看到患者
    const val REJECTED = "rejected" // 医生拒绝绑定

    // 所有合法状态，落库前校验用
    val ALL: Set<String> = setOf(UNBOUND, PENDING, ACTIVE, REJECTED)

    // 判断一个字符串是否是合法的绑定状态
    fun isValid(value: String): Boolean = value in ALL
}
