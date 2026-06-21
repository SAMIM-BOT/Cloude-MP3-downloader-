package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.DownloadItem
import com.example.data.model.UserProfile
import com.example.data.remote.SongSearchResult
import com.example.data.repository.DownloadRepository
import com.example.util.AudioPlayerManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DownloadRepository

    init {
        val database = AppDatabase.getInstance(application)
        repository = DownloadRepository(database.downloadDao(), application)
    }

    // Media Player bindings
    val isPlaying = AudioPlayerManager.isPlaying
    val currentPlayingId = AudioPlayerManager.currentPlayingId
    val currentPlayingTitle = AudioPlayerManager.currentPlayingTitle
    val playbackProgress = AudioPlayerManager.playbackProgress
    val currentTimeText = AudioPlayerManager.currentTimeText
    val totalTimeText = AudioPlayerManager.totalTimeText

    // Selected API pattern
    val apiOptions = listOf(
        ApiOption("EliteProTech API", "https://eliteprotech-apis.zone.id/ytdown?url={url}&format=mp3"),
        ApiOption("Yupra Downloader", "https://api.yupra.my.id/api/downloader/ytmp3?url={url}"),
        ApiOption("Okatsu Downloader", "https://okatsu-rolezapiiz.vercel.app/downloader/ytmp3?url={url}")
    )

    private val _selectedApiIndex = MutableStateFlow(0)
    val selectedApiIndex: StateFlow<Int> = _selectedApiIndex.asStateFlow()

    // Local profile State
    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    // Download history lists
    val downloadHistory: StateFlow<List<DownloadItem>> = repository.allDownloads
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Search query states
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SongSearchResult>>(emptyList())
    val searchResults: StateFlow<List<SongSearchResult>> = _searchResults.asStateFlow()

    private val _searchState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val searchState: StateFlow<SearchUiState> = _searchState.asStateFlow()

    // Cloud sync status feedbacks
    private val _syncingState = MutableStateFlow<SyncUiState>(SyncUiState.Idle)
    val syncingState: StateFlow<SyncUiState> = _syncingState.asStateFlow()

    init {
        // Load local configurations
        val savedProfile = repository.getLocalProfile()
        _userProfile.value = savedProfile

        // Keep local profile counts in sync dynamically
        viewModelScope.launch {
            downloadHistory.collect { history ->
                val completedCount = history.count { it.status == "COMPLETED" }
                _userProfile.update { it.copy(totalDownloadsCount = completedCount) }
            }
        }
    }

    fun setApiIndex(index: Int) {
        if (index in apiOptions.indices) {
            _selectedApiIndex.value = index
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun executeSearch() {
        val query = _searchQuery.value.trim()
        if (query.isEmpty()) return

        viewModelScope.launch {
            _searchState.value = SearchUiState.Loading
            try {
                val results = repository.searchSongs(query)
                _searchResults.value = results
                _searchState.value = if (results.isEmpty()) {
                    SearchUiState.Empty
                } else {
                    SearchUiState.Success(results)
                }
            } catch (e: Exception) {
                _searchState.value = SearchUiState.Error(e.localizedMessage ?: "Search failed")
            }
        }
    }

    fun downloadSong(song: SongSearchResult) {
        val pattern = apiOptions[_selectedApiIndex.value].pattern
        repository.startDownload(song, pattern, _userProfile.value)
    }

    fun deleteHistoricalItem(item: DownloadItem) {
        viewModelScope.launch {
            // If playing right now, make sure to stop first
            if (item.id == currentPlayingId.value) {
                AudioPlayerManager.stopPlayback()
            }
            repository.deleteDownload(item)
        }
    }

    fun updateProfile(username: String, email: String) {
        val updated = _userProfile.value.copy(
            username = username.trim().ifEmpty { "Music Lover" },
            email = email.trim().ifEmpty { "ytbynebula@gmail.com" }
        )
        _userProfile.value = updated
        repository.saveLocalProfile(updated)
    }

    fun forceCloudBackup() {
        viewModelScope.launch {
            _syncingState.value = SyncUiState.Syncing
            repository.forceBulkSyncToCloud(_userProfile.value) { success ->
                if (success) {
                    _syncingState.value = SyncUiState.Success
                } else {
                    _syncingState.value = SyncUiState.Error("Realtime database sync failed. Check connection.")
                }
            }
        }
    }

    fun playLocalMp3(item: DownloadItem) {
        val path = item.localFilePath
        if (path != null) {
            AudioPlayerManager.playFile(item.id, item.title, path)
        }
    }

    fun togglePlayPause() {
        AudioPlayerManager.togglePlayPause()
    }

    fun stopPlayback() {
        AudioPlayerManager.stopPlayback()
    }

    override fun onCleared() {
        super.onCleared()
        AudioPlayerManager.stopPlayback()
    }
}

data class ApiOption(val name: String, val pattern: String)

sealed class SearchUiState {
    object Idle : SearchUiState()
    object Loading : SearchUiState()
    data class Success(val songs: List<SongSearchResult>) : SearchUiState()
    object Empty : SearchUiState()
    data class Error(val message: String) : SearchUiState()
}

sealed class SyncUiState {
    object Idle : SyncUiState()
    object Syncing : SyncUiState()
    object Success : SyncUiState()
    data class Error(val message: String) : SyncUiState()
}
