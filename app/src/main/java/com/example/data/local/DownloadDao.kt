package com.example.data.local

import androidx.room.*
import com.example.data.model.DownloadItem
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY timestamp DESC")
    fun getAllDownloads(): Flow<List<DownloadItem>>

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getDownloadById(id: String): DownloadItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadItem)

    @Update
    suspend fun updateDownload(download: DownloadItem)

    @Query("UPDATE downloads SET status = :status, progress = :progress, localFilePath = :localFilePath WHERE id = :id")
    suspend fun updateDownloadProgress(id: String, status: String, progress: Float, localFilePath: String?)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteDownloadById(id: String)

    @Query("SELECT * FROM downloads WHERE synced = 0")
    suspend fun getUnsyncedDownloads(): List<DownloadItem>

    @Query("UPDATE downloads SET synced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)
}
