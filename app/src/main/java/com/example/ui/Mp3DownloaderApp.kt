package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.DownloadItem
import com.example.data.model.UserProfile
import com.example.data.remote.SongSearchResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Mp3DownloaderApp(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(AppTab.Discover) }
    
    // ViewModel states
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchState by viewModel.searchState.collectAsState()
    val downloadHistory by viewModel.downloadHistory.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val selectedApiIndex by viewModel.selectedApiIndex.collectAsState()
    val syncingState by viewModel.syncingState.collectAsState()
    
    // Audio Player states
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPlayingId by viewModel.currentPlayingId.collectAsState()
    val currentPlayingTitle by viewModel.currentPlayingTitle.collectAsState()
    val playbackProgress by viewModel.playbackProgress.collectAsState()
    val currentTimeText by viewModel.currentTimeText.collectAsState()
    val totalTimeText by viewModel.totalTimeText.collectAsState()

    // Slate Synthwave Color Palette
    val darkBg = Color(0xFF0F172A) // slate 900
    val cardBg = Color(0xFF1E293B) // slate 800
    val accentColor = Color(0xFF10B981) // emerald 500 (emerald accent looks stunning with slate!)
    val primaryColor = Color(0xFF3B82F6) // blue 500
    val textMuted = Color(0xFF94A3B8) // slate 400

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(darkBg),
        topBar = {
            Column(
                modifier = Modifier
                    .background(darkBg)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Filled.CloudDownload,
                        contentDescription = "Cloud Icon",
                        tint = accentColor,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Cloud MP3",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = FontFamily.SansSerif
                        )
                        Text(
                            text = "Premium Sync & Download Engine",
                            fontSize = 11.sp,
                            color = textMuted
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // Simple indicator for synced items count
                    Box(
                        modifier = Modifier
                            .background(cardBg, RoundedCornerShape(12.dp))
                            .border(1.dp, textMuted.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.CloudCircle,
                                contentDescription = "Cloud Status",
                                tint = accentColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "v1.0 Ready",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Elegant Segmented Navigation Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(cardBg, RoundedCornerShape(14.dp))
                        .padding(4.dp)
                ) {
                    AppTab.values().forEach { tab ->
                        val isSelected = activeTab == tab
                        val isSelectedColor = if (isSelected) primaryColor else Color.Transparent
                        
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(isSelectedColor)
                                .clickable { activeTab = tab }
                                .padding(vertical = 10.dp)
                                .testTag("tab_${tab.name.lowercase()}")
                        ) {
                            Text(
                                text = tab.title,
                                color = if (isSelected) Color.White else textMuted,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            // Anchor dynamic mini media playback drawer at the very bottom
            AnimatedVisibility(
                visible = currentPlayingId != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Surface(
                    color = cardBg,
                    tonalElevation = 8.dp,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Spinning vinyl icon placeholder
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(darkBg)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MusicNote,
                                    contentDescription = "Playing Music",
                                    tint = accentColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentPlayingTitle ?: "No Title",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "$currentTimeText / $totalTimeText",
                                    color = textMuted,
                                    fontSize = 11.sp
                                )
                            }

                            // Interactive controls
                            IconButton(
                                onClick = { viewModel.togglePlayPause() },
                                modifier = Modifier.testTag("player_play_pause")
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Filled.PauseCircle else Icons.Filled.PlayCircle,
                                    contentDescription = "Play Pause",
                                    tint = accentColor,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            IconButton(
                                onClick = { viewModel.stopPlayback() },
                                modifier = Modifier.testTag("player_stop")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.StopCircle,
                                    contentDescription = "Stop Playback",
                                    tint = Color.Red.copy(alpha = 0.8f),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Media progress bar slider representation
                        LinearProgressIndicator(
                            progress = { playbackProgress },
                            color = accentColor,
                            trackColor = textMuted.copy(alpha = 0.2f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(darkBg)
                .padding(innerPadding)
        ) {
            when (activeTab) {
                AppTab.Discover -> DiscoverScreen(
                    viewModel = viewModel,
                    searchQuery = searchQuery,
                    searchState = searchState,
                    selectedApiIndex = selectedApiIndex,
                    cardBg = cardBg,
                    accentColor = accentColor,
                    primaryColor = primaryColor,
                    textMuted = textMuted
                )
                AppTab.Library -> LibraryScreen(
                    viewModel = viewModel,
                    downloadHistory = downloadHistory,
                    currentPlayingId = currentPlayingId,
                    isPlaying = isPlaying,
                    cardBg = cardBg,
                    accentColor = accentColor,
                    primaryColor = primaryColor,
                    textMuted = textMuted
                )
                AppTab.Profile -> ProfileScreen(
                    viewModel = viewModel,
                    userProfile = userProfile,
                    syncingState = syncingState,
                    downloadHistory = downloadHistory,
                    cardBg = cardBg,
                    accentColor = accentColor,
                    primaryColor = primaryColor,
                    textMuted = textMuted
                )
            }
        }
    }
}

/**
 * Tab definitions
 */
enum class AppTab(val title: String) {
    Discover("Discover"),
    Library("Library"),
    Profile("Cloud Sync")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    viewModel: MainViewModel,
    searchQuery: String,
    searchState: SearchUiState,
    selectedApiIndex: Int,
    cardBg: Color,
    accentColor: Color,
    primaryColor: Color,
    textMuted: Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        
        // Search Input Card field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            placeholder = { Text("Search songs (e.g. Lofi beats)", color = textMuted) },
            textStyle = LocalTextStyle.current.copy(color = Color.White),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Search Icon",
                    tint = accentColor
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = "Clear",
                            tint = Color.White
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor,
                unfocusedBorderColor = cardBg,
                focusedContainerColor = cardBg,
                unfocusedContainerColor = cardBg,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // API Downloader selector
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBg.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Dns,
                contentDescription = "API selection",
                tint = primaryColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Engine:",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            
            // Render clickable chips for three downloader APIs
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                viewModel.apiOptions.forEachIndexed { index, option ->
                    val isSelected = selectedApiIndex == index
                    val chipColor = if (isSelected) primaryColor else cardBg
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(chipColor)
                            .clickable { viewModel.setApiIndex(index) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("api_chip_$index")
                    ) {
                        Text(
                            text = if (index == 0) "ElitePro" else if (index == 1) "Yupra" else "Okatsu",
                            color = if (isSelected) Color.White else textMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { viewModel.executeSearch() },
            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("search_button")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Send,
                    contentDescription = "Submit search",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Find MP3", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // State machine UI handler
        when (searchState) {
            is SearchUiState.Idle -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.Headset,
                            contentDescription = "Headphones",
                            tint = textMuted.copy(alpha = 0.3f),
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Search for YouTube tracks and compile offline library",
                            color = textMuted,
                            fontSize = 13.sp,
                            maxLines = 2,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            }
            is SearchUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = accentColor)
                }
            }
            is SearchUiState.Success -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(searchState.songs) { song ->
                        SearchResultItemCard(
                            song = song,
                            viewModel = viewModel,
                            cardBg = cardBg,
                            accentColor = accentColor,
                            textMuted = textMuted
                        )
                    }
                }
            }
            is SearchUiState.Empty -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No tracks found on YouTube. Try a different query.",
                        color = Color.Red.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                }
            }
            is SearchUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Query failed: ${searchState.message}",
                        color = Color.Red,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun SearchResultItemCard(
    song: SongSearchResult,
    viewModel: MainViewModel,
    cardBg: Color,
    accentColor: Color,
    textMuted: Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Song Image with Coil
            AsyncImage(
                model = song.thumbnailUrl,
                contentDescription = "Thumbnail for ${song.title}",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.2f))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = song.artist,
                    color = textMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            IconButton(
                onClick = { viewModel.downloadSong(song) },
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.08f), CircleShape)
                    .size(40.dp)
                    .testTag("download_action_${song.id}")
            ) {
                Icon(
                    imageVector = Icons.Filled.DownloadForOffline,
                    contentDescription = "Download Item",
                    tint = accentColor
                )
            }
        }
    }
}

@Composable
fun LibraryScreen(
    viewModel: MainViewModel,
    downloadHistory: List<DownloadItem>,
    currentPlayingId: String?,
    isPlaying: Boolean,
    cardBg: Color,
    accentColor: Color,
    primaryColor: Color,
    textMuted: Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.List,
                contentDescription = "Library Header Icon",
                tint = primaryColor,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "My Offline Archive",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${downloadHistory.size} Songs",
                fontSize = 12.sp,
                color = textMuted,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (downloadHistory.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.FolderOpen,
                        contentDescription = "No Downloads Icon",
                        tint = textMuted.copy(alpha = 0.3f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Your offline library is currently empty",
                        color = textMuted,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Search songs in discover to populate!",
                        color = primaryColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(downloadHistory, key = { it.id }) { item ->
                    LibraryDownloadItemRow(
                        item = item,
                        viewModel = viewModel,
                        isCurrentPlaying = currentPlayingId == item.id,
                        isPlaying = isPlaying,
                        cardBg = cardBg,
                        accentColor = accentColor,
                        primaryColor = primaryColor,
                        textMuted = textMuted
                    )
                }
            }
        }
    }
}

@Composable
fun LibraryDownloadItemRow(
    item: DownloadItem,
    viewModel: MainViewModel,
    isCurrentPlaying: Boolean,
    isPlaying: Boolean,
    cardBg: Color,
    accentColor: Color,
    primaryColor: Color,
    textMuted: Color
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentPlaying) cardBg.copy(alpha = 0.9f) else cardBg
        ),
        shape = RoundedCornerShape(14.dp),
        border = if (isCurrentPlaying) BorderStroke(1.dp, accentColor) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                AsyncImage(
                    model = item.thumbnailUrl,
                    contentDescription = "Thumbnail",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(8.dp))
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.artist,
                        color = textMuted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Render actions depending on states
                when (item.status) {
                    "PENDING" -> {
                        CircularProgressIndicator(
                            color = primaryColor,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    "DOWNLOADING" -> {
                        CircularProgressIndicator(
                            progress = { item.progress },
                            color = accentColor,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    "FAILED" -> {
                        IconButton(
                            onClick = { viewModel.deleteHistoricalItem(item) }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Error,
                                contentDescription = "Retry / delete Failed state",
                                tint = Color.Red.copy(alpha = 0.8f)
                            )
                        }
                    }
                    "COMPLETED" -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Local play action
                            val iconChoice = if (isCurrentPlaying && isPlaying) Icons.Filled.PauseCircle else Icons.Filled.PlayCircle
                            val colorChoice = if (isCurrentPlaying) accentColor else Color.White
                            
                            IconButton(
                                onClick = { viewModel.playLocalMp3(item) },
                                modifier = Modifier.testTag("play_${item.id}")
                            ) {
                                Icon(
                                    imageVector = iconChoice,
                                    contentDescription = "Play/pause downloaded track",
                                    tint = colorChoice,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            // Cloud synced logo badge indicator
                            Icon(
                                imageVector = if (item.synced) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
                                contentDescription = "Sync state logo",
                                tint = if (item.synced) accentColor else textMuted.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .size(18.dp)
                                    .padding(end = 4.dp)
                            )

                            // Local remove file
                            IconButton(
                                onClick = { viewModel.deleteHistoricalItem(item) },
                                modifier = Modifier.testTag("delete_${item.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DeleteOutline,
                                    contentDescription = "Delete item",
                                    tint = textMuted
                                )
                            }
                        }
                    }
                }
            }

            // If downloading render a sleek background progress tracker row
            if (item.status == "DOWNLOADING") {
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress = { item.progress },
                        color = accentColor,
                        trackColor = textMuted.copy(alpha = 0.2f),
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${(item.progress * 100).toInt()}%",
                        color = accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: MainViewModel,
    userProfile: UserProfile,
    syncingState: SyncUiState,
    downloadHistory: List<DownloadItem>,
    cardBg: Color,
    accentColor: Color,
    primaryColor: Color,
    textMuted: Color
) {
    var isEditing by remember { mutableStateOf(false) }
    var editUsername by remember { mutableStateOf(userProfile.username) }
    var editEmail by remember { mutableStateOf(userProfile.email) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Profile Details Card
        Card(
            colors = CardDefaults.cardColors(containerColor = cardBg),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Avatar placeholder
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(primaryColor, accentColor)))
                    ) {
                        Text(
                            text = userProfile.username.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = userProfile.username,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = userProfile.email,
                            color = textMuted,
                            fontSize = 12.sp
                        )
                    }

                    IconButton(
                        onClick = {
                            if (isEditing) {
                                viewModel.updateProfile(editUsername, editEmail)
                            } else {
                                editUsername = userProfile.username
                                editEmail = userProfile.email
                            }
                            isEditing = !isEditing
                        },
                        modifier = Modifier.testTag("edit_profile_btn")
                    ) {
                        Icon(
                            imageVector = if (isEditing) Icons.Filled.CheckCircle else Icons.Filled.Edit,
                            contentDescription = "Edit profiles",
                            tint = if (isEditing) accentColor else Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                AnimatedVisibility(visible = isEditing) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = editUsername,
                            onValueChange = { editUsername = it },
                            label = { Text("Cloud Username", color = textMuted) },
                            textStyle = LocalTextStyle.current.copy(color = Color.White),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = cardBg,
                                focusedContainerColor = cardBg,
                                unfocusedContainerColor = cardBg
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("edit_username")
                        )

                        OutlinedTextField(
                            value = editEmail,
                            onValueChange = { editEmail = it },
                            label = { Text("Cloud Email", color = textMuted) },
                            textStyle = LocalTextStyle.current.copy(color = Color.White),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = cardBg,
                                focusedContainerColor = cardBg,
                                unfocusedContainerColor = cardBg
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("edit_email")
                        )
                    }
                }

                Divider(
                    color = textMuted.copy(alpha = 0.2f),
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                // Render Specs Details of the physical platform
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "USER UID", color = textMuted, fontSize = 10.sp)
                        Text(text = userProfile.uid, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "DEVICE IDENTIFIER", color = textMuted, fontSize = 10.sp)
                        Text(text = "${userProfile.deviceBrand} ${userProfile.deviceModel}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Firebase Cloud configuration parameters
        Text(
            text = "Firebase Cloud Database Sync",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        Text(
            text = "Your configurations synchronize to your project node automatically",
            color = textMuted,
            fontSize = 11.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = cardBg),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Storage,
                            contentDescription = "Database Icon",
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = "Firebase Realtime DB URL", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(text = "https://earningwalaby-default-rtdb.firebaseio.com", color = primaryColor, fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Stats rows
                val syncedCount = downloadHistory.count { it.status == "COMPLETED" && it.synced }
                val unsyncedCount = downloadHistory.count { it.status == "COMPLETED" && !it.synced }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Cloud Backed Up Songs:", color = textMuted, fontSize = 12.sp)
                    Text(text = "$syncedCount Items", color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Pending Offline-Only Items:", color = textMuted, fontSize = 12.sp)
                    Text(text = "$unsyncedCount Items", color = if (unsyncedCount > 0) Color.Yellow else textMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.forceCloudBackup() },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("backup_to_firebase_btn")
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Sync,
                            contentDescription = "Manual sync backup",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Manual Backup To Cloud", fontWeight = FontWeight.Bold)
                    }
                }

                // Sync dynamics status feedbacks
                when (syncingState) {
                    is SyncUiState.Syncing -> {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(color = accentColor, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Syncing with earningwalaby.firebaseapp.com...", color = textMuted, fontSize = 11.sp)
                        }
                    }
                    is SyncUiState.Success -> {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = "Synced successfully badge", tint = accentColor, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Cloud archive synchronized successfully!", color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    is SyncUiState.Error -> {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Filled.Warning, contentDescription = "Error badge", tint = Color.Red, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = syncingState.message, color = Color.Red, fontSize = 11.sp)
                        }
                    }
                    else -> {}
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
