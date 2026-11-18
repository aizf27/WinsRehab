package com.example.winsrehab.ui.main.patient.Video

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.winsrehab.R
import com.example.winsrehab.databinding.ActivityVideoPlayerBinding
import com.example.winsrehab.ui.common.UiTexts
import java.io.File
import java.io.FileOutputStream

class VideoPlayerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVideoPlayerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.topBar.tvTopBarTitle.text = intent.getStringExtra("title") ?: getString(R.string.page_video_player_title)
        binding.topBar.btnTopBarBack.setOnClickListener { finish() }
        binding.statePlayerError.btnStateRetry.setText(R.string.action_back)
        binding.statePlayerError.btnStateRetry.setOnClickListener { finish() }
        binding.btnPlayPause.setOnClickListener { togglePlayback() }

        val videoPath = intent.getStringExtra("url")
        if (videoPath.isNullOrBlank()) {
            showError(getString(R.string.video_path_missing))
            return
        }
        binding.videoView.setOnPreparedListener {
            binding.btnPlayPause.setText(R.string.action_pause)
            binding.videoView.start()
        }
        binding.videoView.setOnCompletionListener {
            binding.btnPlayPause.setText(R.string.action_play)
        }
        binding.videoView.setOnErrorListener { _, _, _ ->
            showError(getString(R.string.video_playback_failed))
            true
        }

        try {
            when {
                videoPath.startsWith("file:///android_asset/") -> {
                    val assetFileName = videoPath.removePrefix("file:///android_asset/")
                    val outFile = File(cacheDir, assetFileName)
                    if (!outFile.exists()) copyAssetToCache(assetFileName, outFile)
                    binding.videoView.setVideoPath(outFile.absolutePath)
                }
                videoPath.startsWith("http") -> binding.videoView.setVideoURI(Uri.parse(videoPath))
                else -> showError(getString(R.string.video_path_invalid))
            }
        } catch (e: Exception) {
            showError(getString(R.string.video_playback_failed))
        }
    }

    private fun togglePlayback() {
        if (binding.videoView.isPlaying) {
            binding.videoView.pause()
            binding.btnPlayPause.setText(R.string.action_play)
        } else {
            binding.videoView.start()
            binding.btnPlayPause.setText(R.string.action_pause)
        }
    }

    private fun showError(reason: String) {
        binding.videoView.stopPlayback()
        binding.videoView.visibility = View.GONE
        binding.btnPlayPause.visibility = View.GONE
        binding.statePlayerError.tvStateError.text = UiTexts.errorText(reason)
        binding.statePlayerError.root.visibility = View.VISIBLE
    }

    private fun copyAssetToCache(assetName: String, outFile: File) {
        outFile.parentFile?.mkdirs()
        assets.open(assetName).use { input ->
            FileOutputStream(outFile).use { output -> input.copyTo(output) }
        }
    }

    override fun onDestroy() {
        binding.videoView.stopPlayback()
        super.onDestroy()
    }
}
