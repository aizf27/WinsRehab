package com.example.winsrehab.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.winsrehab.data.model.UploadStatus
import com.example.winsrehab.data.model.VideoReviewStatus

// 患者训练视频：一次任务执行对应一条视频，医生看完写审核结果
// 状态流转：local → queued → uploaded（上传状态）；pending_review → approved / rejected（审核状态）
@Entity(
    tableName = "ptrehab_video",
    indices = [
        Index(value = ["patientId"]),
        Index(value = ["taskInstanceId"]),
        Index(value = ["reviewStatus"])
    ]
)
data class PtRehabVideo(
    @PrimaryKey(autoGenerate = true)
    val videoId: Long = 0,                                  // 主键，自增

    val patientId: String = "",                             // 对应 Patient.patientId

    val taskInstanceId: Long? = null,                       // 对应的任务实例；手工上传的视频可为空

    val localPath: String = "",                             // 手机本地文件路径（filesDir/videos/xxx.mp4）

    val remoteUrl: String = "",                             // 上传后的远端地址；本地模式为空

    val uploadDate: String = "",                            // 上传日期（yyyy-MM-dd HH:mm）

    val description: String? = null,                        // 备注/说明

    val reviewStatus: String = VideoReviewStatus.PENDING_REVIEW,  // 审核状态：pending_review/approved/rejected

    val reviewComment: String? = null,                      // 医生评语（驳回时写原因）

    val reviewedAt: Long? = null,                           // 审核时间戳

    val reviewerCode: String? = null,                       // 审核医生工号

    val uploadStatus: String = UploadStatus.LOCAL,          // 上传状态：local/queued/uploaded/failed

    val durationMs: Long = 0L,                              // 视频时长（毫秒）

    val sizeBytes: Long = 0L                                // 文件大小（字节）
)
