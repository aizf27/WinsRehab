package com.example.winsrehab.ui.main.patient.Video

import android.net.Uri
import android.os.Bundle
import android.widget.MediaController
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.winsrehab.databinding.ActivityVideoPlayerBinding
import java.io.File
import java.io.FileOutputStream

class VideoPlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVideoPlayerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val videoPath = intent.getStringExtra("url") ?: return
        val videoTitle = intent.getStringExtra("title") ?: "视频播放"
        title = videoTitle

        val mediaController = MediaController(this)
        mediaController.setAnchorView(binding.videoView)
        binding.videoView.setMediaController(mediaController)

        try {
            when {
                videoPath.startsWith("file:///android_asset/") -> {
                    // 取出 assets 子路径
                    val assetFileName = videoPath.removePrefix("file:///android_asset/")

                    // 复制到 cacheDir
                    val outFile = File(cacheDir, assetFileName)
                    if (!outFile.exists()) {
                        copyAssetToCache(assetFileName, outFile)
                    }

                    // 播放缓存文件
                    binding.videoView.setVideoPath(outFile.absolutePath)
                    binding.videoView.requestFocus()
                    binding.videoView.start()
                }
                videoPath.startsWith("http") -> {
                    // 网络视频
                    binding.videoView.setVideoURI(Uri.parse(videoPath))
                    binding.videoView.requestFocus()
                    binding.videoView.start()
                }
                else -> {
                    Toast.makeText(this, "视频路径错误", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "视频播放失败", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 将 assets 文件复制到 cacheDir
     */
    private fun copyAssetToCache(assetName: String, outFile: File) {
        // 确保父目录存在
        outFile.parentFile?.mkdirs()
        assets.open(assetName).use { input ->
            FileOutputStream(outFile).use { output ->
                input.copyTo(output)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.videoView.stopPlayback()
    }
}
