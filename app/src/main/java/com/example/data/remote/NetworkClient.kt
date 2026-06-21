package com.example.data.remote

import android.util.Log
import com.example.data.model.DownloadItem
import com.example.data.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

object NetworkClient {
    private const val TAG = "NetworkClient"

    // Set a solid timeout so searches and downloads have plenty of breathing room
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // Public Invidious instances for YouTube Search fallback
    private val searchInstances = listOf(
        "https://yewtu.be",
        "https://vid.puffyan.us",
        "https://invidious.nerdvpn.de",
        "https://invidious.flokinet.to",
        "https://iv.melmac.space"
    )

    /**
     * Search songs on YouTube via public Invidious API instances
     */
    suspend fun searchSongs(query: String): List<SongSearchResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<SongSearchResult>()
        
        // Let's iterate over our instances and try them
        for (instance in searchInstances) {
            val url = "$instance/api/v1/search?q=${java.net.URLEncoder.encode(query, "UTF-8")}&type=video"
            try {
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: return@use
                        val jsonArray = JSONArray(body)
                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            val title = obj.optString("title", "Unknown Track")
                            val videoId = obj.optString("videoId", "")
                            val author = obj.optString("author", "Unknown Artist")
                            val lengthSeconds = obj.optLong("lengthSeconds", 0L)
                            
                            var thumbUrl = ""
                            if (obj.has("videoThumbnails")) {
                                val thumbs = obj.getJSONArray("videoThumbnails")
                                if (thumbs.length() > 0) {
                                    // Usually there is a default medium thumbnail
                                    thumbUrl = thumbs.getJSONObject(0).optString("url", "")
                                }
                            }
                            if (thumbUrl.isEmpty() && videoId.isNotEmpty()) {
                                thumbUrl = "https://img.youtube.com/vi/$videoId/mqdefault.jpg"
                            }

                            if (videoId.isNotEmpty()) {
                                results.add(
                                    SongSearchResult(
                                        id = videoId,
                                        title = title,
                                        artist = author,
                                        durationSeconds = lengthSeconds,
                                        thumbnailUrl = thumbUrl,
                                        youtubeUrl = "https://www.youtube.com/watch?v=$videoId"
                                    )
                                )
                            }
                        }
                    }
                }
                
                if (results.isNotEmpty()) {
                    Log.d(TAG, "Search successful using $instance. Found ${results.size} items.")
                    break // Stop if we got successful search results!
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed searching with instance: $instance. Trying next...", e)
            }
        }
        
        // If all online Invidious instances fail or are rate-limited, provide fallback local suggestions
        if (results.isEmpty()) {
            return@withContext getOfflineMockFallbacks(query)
        }
        
        return@withContext results
    }

    /**
     * Call one of the selected YouTube-to-MP3 APIs to get the direct download link
     */
    suspend fun getMp3DownloadUrl(apiPattern: String, videoUrl: String): String = withContext(Dispatchers.IO) {
        val formattedApiUrl = apiPattern.replace("{url}", java.net.URLEncoder.encode(videoUrl, "UTF-8"))
        Log.d(TAG, "Fetching MP3 download URL from: $formattedApiUrl")

        val request = Request.Builder().url(formattedApiUrl).build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("API returned unsuccessful code: ${response.code}")
            }
            val body = response.body?.string() ?: throw Exception("Empty API response body")
            Log.d(TAG, "Raw Response: $body")

            // Let's parse JSON fields flexibly
            val directUrl = parseJsonForUrl(body)
            if (directUrl != null && directUrl.startsWith("http")) {
                return@withContext directUrl
            }

            // Fallback - check if body itself is a plain text URL
            if (body.trim().startsWith("http")) {
                return@withContext body.trim()
            }

            throw Exception("Could not extract a valid download link from API response. Response was: $body")
        }
    }

    /**
     * Download the MP3 file from the direct download URL with progress updates
     */
    suspend fun downloadFileWithProgress(
        downloadUrl: String,
        destinationFile: File,
        onProgress: (Float, Long) -> Unit
    ) = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(downloadUrl).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("Failed to retrieve file from direct download URL. Code: ${response.code}")
            }
            val body = response.body ?: throw Exception("Failure: Empty file body")
            val totalBytes = body.contentLength()
            
            var bytesCopied = 0L
            val buffer = ByteArray(8192)
            var bytes = body.source().read(buffer)

            FileOutputStream(destinationFile).use { output ->
                while (bytes >= 0) {
                    output.write(buffer, 0, bytes)
                    bytesCopied += bytes
                    
                    val progress = if (totalBytes > 0) bytesCopied.toFloat() / totalBytes else 0f
                    onProgress(progress, bytesCopied)
                    
                    bytes = body.source().read(buffer)
                }
            }
        }
    }

    /**
     * Flexibility parser to handle multiple formats: url, dlink, result.url, download, custom api structures
     */
    private fun parseJsonForUrl(jsonString: String): String? {
        try {
            val root = JSONObject(jsonString)
            // Direct strings
            val keys = listOf("result", "url", "link", "download", "download_url", "downloadUrl", "data", "mp3")
            for (key in keys) {
                if (root.has(key)) {
                    val value = root.get(key)
                    if (value is String) {
                        return value
                    } else if (value is JSONObject) {
                        val subKeys = listOf("url", "link", "download", "downloadUrl", "dlink", "mp3", "file")
                        for (subKey in subKeys) {
                            if (value.has(subKey)) {
                                val subVal = value.get(subKey)
                                if (subVal is String) return subVal
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing json dynamic keys", e)
        }
        return null
    }

    /**
     * Backup mock results if search fails completely without network
     */
    private fun getOfflineMockFallbacks(query: String): List<SongSearchResult> {
        val tracks = listOf(
            Triple("No correlation", "LO-FI Chill Coffee", "https://www.youtube.com/watch?v=5qap5aO4i9A"),
            Triple("Aura of Beats", "Hip Hop Chill", "https://www.youtube.com/watch?v=05689YytA9B"),
            Triple("Summer Vibe Synth", "Retro Synth Wave", "https://www.youtube.com/watch?v=938f38ffbA3"),
            Triple("Lofi study session", "Lofi Girl", "https://www.youtube.com/watch?v=jfKfPfyJRdk")
        )
        return tracks.mapIndexed { idx, item ->
            SongSearchResult(
                id = "mock_r_$idx",
                title = "${item.first} (offline fallback for: $query)",
                artist = item.second,
                durationSeconds = 180,
                thumbnailUrl = "https://img.youtube.com/vi/jfKfPfyJRdk/mqdefault.jpg",
                youtubeUrl = item.third
            )
        }
    }

    /**
     * Firebase Realtime Database Sync Function
     * Syncs a UserProfile to the REST Database
     */
    suspend fun syncUserProfileToFirebase(profile: UserProfile): Boolean = withContext(Dispatchers.IO) {
        val url = "https://earningwalaby-default-rtdb.firebaseio.com/users/${profile.uid}.json"
        try {
            val jsonObject = JSONObject().apply {
                put("uid", profile.uid)
                put("username", profile.username)
                put("email", profile.email)
                put("joinedTimestamp", profile.joinedTimestamp)
                put("totalDownloadsCount", profile.totalDownloadsCount)
                put("deviceModel", profile.deviceModel)
                put("deviceBrand", profile.deviceBrand)
            }
            val requestBody = jsonObject.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(url).put(requestBody).build()
            client.newCall(request).execute().use { response ->
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase Profile Sync Failed", e)
            return@withContext false
        }
    }

    /**
     * Firebase Realtime Database Sync Download Item
     * Syncs single download to cloud history
     */
    suspend fun syncDownloadToFirebase(uid: String, item: DownloadItem): Boolean = withContext(Dispatchers.IO) {
        val url = "https://earningwalaby-default-rtdb.firebaseio.com/users/$uid/downloads/${item.id}.json"
        try {
            val jsonObject = JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("artist", item.artist)
                put("durationSeconds", item.durationSeconds)
                put("thumbnailUrl", item.thumbnailUrl)
                put("downloadUrl", item.downloadUrl)
                put("status", item.status)
                put("timestamp", item.timestamp)
            }
            val requestBody = jsonObject.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(url).put(requestBody).build()
            client.newCall(request).execute().use { response ->
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase Download Item Sync Failed", e)
            return@withContext false
        }
    }
}

/**
 * Clean data model for video searches
 */
data class SongSearchResult(
    val id: String,
    val title: String,
    val artist: String,
    val durationSeconds: Long,
    val thumbnailUrl: String,
    val youtubeUrl: String
)
