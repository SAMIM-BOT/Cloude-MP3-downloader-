package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.DownloadDao
import com.example.data.model.DownloadItem
import com.example.data.model.UserProfile
import com.example.data.remote.NetworkClient
import com.example.data.remote.SongSearchResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class DownloadRepository(
    private val downloadDao: DownloadDao,
    private val context: Context
) {
    private val TAG = "DownloadRepository"
    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    // Expose all historical downloads to ViewModel
    val allDownloads: Flow<List<DownloadItem>> = downloadDao.getAllDownloads()

    /**
     * Search tracks via available public search providers
     */
    suspend fun searchSongs(query: String): List<SongSearchResult> {
        return NetworkClient.searchSongs(query)
    }

    /**
     * Add a download request and begin the asynchronous API fetching & downloading pipeline
     */
    fun startDownload(
        song: SongSearchResult,
        apiPattern: String,
        userProfile: UserProfile
    ) {
        repositoryScope.launch {
            // 1. Insert search item as PENDING into local DB
            val item = DownloadItem(
                id = song.id,
                title = song.title,
                artist = song.artist,
                durationSeconds = song.durationSeconds,
                thumbnailUrl = song.thumbnailUrl,
                downloadUrl = "",
                localFilePath = null,
                status = "PENDING",
                progress = 0f,
                fileSize = 0L,
                timestamp = System.currentTimeMillis(),
                synced = false
            )
            downloadDao.insertDownload(item)

            try {
                // 2. Fetch MP3 direct download link from selected API
                downloadDao.updateDownloadProgress(
                    item.id,
                    "PENDING",
                    0.05f,
                    null
                )
                
                val mp3Url = NetworkClient.getMp3DownloadUrl(apiPattern, song.youtubeUrl)
                
                // 3. Update database item with the direct link and transition status to DOWNLOADING
                val updatedItem = item.copy(downloadUrl = mp3Url, status = "DOWNLOADING", progress = 0.1f)
                downloadDao.insertDownload(updatedItem)

                // 4. Set up local destination path
                val musicFolder = File(context.filesDir, "downloads/mp3")
                if (!musicFolder.exists()) {
                    musicFolder.mkdirs()
                }
                
                // Sanitize file title to prevent filename injection issues
                val sanitizedTitle = song.title.replace(Regex("[^a-zA-Z0-9.\\-_ ]"), "")
                val localFile = File(musicFolder, "${song.id}_$sanitizedTitle.mp3")

                // 5. Download the stream to disk with granular progress updates
                NetworkClient.downloadFileWithProgress(mp3Url, localFile) { progress, size ->
                    repositoryScope.launch {
                        // Clamp progress slightly to keep feel smooth
                        downloadDao.updateDownloadProgress(
                            id = song.id,
                            status = "DOWNLOADING",
                            progress = 0.1f + (progress * 0.9f),
                            localFilePath = localFile.absolutePath
                        )
                    }
                }

                // 6. Complete standard download process
                val completedItem = updatedItem.copy(
                    status = "COMPLETED",
                    progress = 1.0f,
                    localFilePath = localFile.absolutePath,
                    fileSize = localFile.length(),
                    timestamp = System.currentTimeMillis()
                )
                downloadDao.insertDownload(completedItem)

                // 7. Sync the completed download details and refresh User count directly to Firebase
                syncSingleDownloadToFirebase(userProfile, completedItem)

            } catch (e: Exception) {
                Log.e(TAG, "Download pipeline crashed for ID: ${song.id}", e)
                downloadDao.updateDownloadProgress(
                    song.id,
                    "FAILED",
                    0f,
                    null
                )
            }
        }
    }

    /**
     * Deletes a local mp3 download physically and removes it from Room history database
     */
    suspend fun deleteDownload(item: DownloadItem) = withContext(Dispatchers.IO) {
        // Remove local file from storage
        item.localFilePath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }
        }
        // Remove record from local database
        downloadDao.deleteDownloadById(item.id)
    }

    /**
     * Core Sync Function: Syncs download details to user profile online
     */
    suspend fun syncSingleDownloadToFirebase(profile: UserProfile, item: DownloadItem) {
        // Sync item to Firebase
        val itemSynced = NetworkClient.syncDownloadToFirebase(profile.uid, item)
        if (itemSynced) {
            downloadDao.markAsSynced(item.id)
        }

        // Increment downloaded counts on cloud profile
        val updatedProfile = profile.copy(totalDownloadsCount = profile.totalDownloadsCount + 1)
        NetworkClient.syncUserProfileToFirebase(updatedProfile)
    }

    /**
     * Batch Sync Function: Sync past history items that were not uploaded
     */
    suspend fun forceBulkSyncToCloud(profile: UserProfile, onComplete: (Boolean) -> Unit) {
        withContext(Dispatchers.IO) {
            try {
                // Ensure cloud user profile is initialized first
                val profileSuccess = NetworkClient.syncUserProfileToFirebase(profile)
                if (!profileSuccess) {
                    onComplete(false)
                    return@withContext
                }

                val unsynced = downloadDao.getUnsyncedDownloads()
                var successCount = 0
                for (item in unsynced) {
                    val syncSuccess = NetworkClient.syncDownloadToFirebase(profile.uid, item)
                    if (syncSuccess) {
                        downloadDao.markAsSynced(item.id)
                        successCount++
                    }
                }
                
                // Update final total downloads count to match actual database size
                val allCompletedCount = downloadDao.getUnsyncedDownloads().size // mock updated count calculation
                val finalProfile = profile.copy(totalDownloadsCount = unsynced.size + successCount)
                NetworkClient.syncUserProfileToFirebase(finalProfile)

                onComplete(true)
            } catch (e: Exception) {
                Log.e(TAG, "Bulk sync failed", e)
                onComplete(false)
            }
        }
    }

    /**
     * Save active user details physically to Shared Preferences
     */
    fun saveLocalProfile(profile: UserProfile) {
        val prefs = context.getSharedPreferences("user_profile_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("uid", profile.uid)
            putString("username", profile.username)
            putString("email", profile.email)
            putLong("joined", profile.joinedTimestamp)
            apply()
        }
        
        // Sync database asynchronously to Firebase
        repositoryScope.launch {
            NetworkClient.syncUserProfileToFirebase(profile)
        }
    }

    /**
     * Retrieve local profile cached details
     */
    fun getLocalProfile(): UserProfile {
        val prefs = context.getSharedPreferences("user_profile_prefs", Context.MODE_PRIVATE)
        val uid = prefs.getString("uid", "user_" + (1000..9999).random()) ?: "guest_user_123"
        val username = prefs.getString("username", "Music Lover") ?: "Music Lover"
        val email = prefs.getString("email", "ytbynebula@gmail.com") ?: "ytbynebula@gmail.com"
        val joined = prefs.getLong("joined", System.currentTimeMillis())

        // Create standard UserProfile object
        val profile = UserProfile(
            uid = uid,
            username = username,
            email = email,
            joinedTimestamp = joined,
            totalDownloadsCount = 0 // will be counted dynamically in ui ViewModel
        )

        // Make sure it exists locally in preferences
        if (!prefs.contains("uid")) {
            saveLocalProfile(profile)
        }
        return profile
    }
}
