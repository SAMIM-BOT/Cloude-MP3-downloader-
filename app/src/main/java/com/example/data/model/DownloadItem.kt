package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadItem(
    @PrimaryKey val id: String, // Youtube videoId or custom UUID
    val title: String,
    val artist: String,
    val durationSeconds: Long,
    val thumbnailUrl: String,
    val downloadUrl: String,
    val localFilePath: String? = null,
    val status: String = "PENDING", // PENDING, DOWNLOADING, COMPLETED, FAILED
    val progress: Float = 0f,
    val fileSize: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val synced: Boolean = false
)
