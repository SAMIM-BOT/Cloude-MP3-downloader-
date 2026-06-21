package com.example.util

import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

object AudioPlayerManager {
    private const val TAG = "AudioPlayerManager"
    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private var progressJob: Job? = null

    // UI Reactivity Flows
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPlayingId = MutableStateFlow<String?>(null)
    val currentPlayingId: StateFlow<String?> = _currentPlayingId.asStateFlow()

    private val _currentPlayingTitle = MutableStateFlow<String?>(null)
    val currentPlayingTitle: StateFlow<String?> = _currentPlayingTitle.asStateFlow()

    private val _playbackProgress = MutableStateFlow(0f) // 0.0f to 1.0f
    val playbackProgress: StateFlow<Float> = _playbackProgress.asStateFlow()

    private val _currentTimeText = MutableStateFlow("0:00")
    val currentTimeText: StateFlow<String> = _currentTimeText.asStateFlow()

    private val _totalTimeText = MutableStateFlow("0:00")
    val totalTimeText: StateFlow<String> = _totalTimeText.asStateFlow()

    fun playFile(songId: String, title: String, filePath: String) {
        try {
            // Stop existing playback
            stopPlayback()

            val file = File(filePath)
            if (!file.exists()) {
                Log.e(TAG, "File does not exist: $filePath")
                return
            }

            mediaPlayer = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                start()
                
                setOnCompletionListener {
                    stopPlayback()
                }
            }

            _isPlaying.value = true
            _currentPlayingId.value = songId
            _currentPlayingTitle.value = title
            _totalTimeText.value = formatDuration(mediaPlayer?.duration ?: 0)

            // Track progress updates dynamically
            startTimer()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start Media Player for $filePath", e)
            stopPlayback()
        }
    }

    fun togglePlayPause() {
        val player = mediaPlayer ?: return
        if (player.isPlaying) {
            player.pause()
            _isPlaying.value = false
            progressJob?.cancel()
        } else {
            player.start()
            _isPlaying.value = true
            startTimer()
        }
    }

    fun stopPlayback() {
        try {
            progressJob?.cancel()
            progressJob = null
            
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping media player safely", e)
        } finally {
            mediaPlayer = null
            _isPlaying.value = false
            _currentPlayingId.value = null
            _currentPlayingTitle.value = null
            _playbackProgress.value = 0f
            _currentTimeText.value = "0:00"
            _totalTimeText.value = "0:00"
        }
    }

    private fun startTimer() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (mediaPlayer != null && _isPlaying.value) {
                mediaPlayer?.let { player ->
                    try {
                        val current = player.currentPosition
                        val duration = player.duration
                        if (duration > 0) {
                            _playbackProgress.value = current.toFloat() / duration.toFloat()
                            _currentTimeText.value = formatDuration(current)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Playback calculation error", e)
                    }
                }
                delay(400)
            }
        }
    }

    private fun formatDuration(millis: Int): String {
        val totalSeconds = millis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }
}
