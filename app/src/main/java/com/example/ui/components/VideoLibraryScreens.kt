package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Playlist
import com.example.data.PlaylistItem
import com.example.viewmodel.VideoPlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoLibraryScreens(
    viewModel: VideoPlayerViewModel,
    playlists: List<Playlist>,
    selectedPlaylistId: Int?,
    playlistItems: List<PlaylistItem>,
    favorites: List<PlaylistItem>,
    onPlayVideo: (PlaylistItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var showAddVideoDialog by remember { mutableStateOf(false) }

    // Tab state: 0 = My Library, 1 = Favorites
    var activeTab by remember { mutableIntStateOf(0) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.tertiary
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Text(
                            text = "LibMedia Player",
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp,
                            fontSize = 20.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            if (activeTab == 0 && selectedPlaylistId == null) {
                // Create Playlist Floating Action Button
                ExtendedFloatingActionButton(
                    onClick = { showCreatePlaylistDialog = true },
                    icon = { Icon(Icons.Default.CreateNewFolder, "Create Playlist Icon") },
                    text = { Text("Create Playlist") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.Black,
                    modifier = Modifier.testTag("create_playlist_fab")
                )
            } else if (activeTab == 0 && selectedPlaylistId != null) {
                // Add Video to current selected Playlist
                ExtendedFloatingActionButton(
                    onClick = { showAddVideoDialog = true },
                    icon = { Icon(Icons.Default.AddLink, "Add Stream Link Icon") },
                    text = { Text("Add Video") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.Black,
                    modifier = Modifier.testTag("add_video_fab")
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Screen tabs: Library & Favorites
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = {
                        Text(
                            "My Library",
                            fontWeight = if (activeTab == 0) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp
                        )
                    },
                    modifier = Modifier.testTag("library_tab")
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "Favorites",
                                fontWeight = if (activeTab == 1) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                            if (favorites.isNotEmpty()) {
                                Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                    Text(
                                        "${favorites.size}",
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    },
                    modifier = Modifier.testTag("favorites_tab")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Body Area
            if (activeTab == 1) {
                // Favorites Listing Screen
                FavoritesTabContent(
                    favorites = favorites,
                    onPlayVideo = onPlayVideo,
                    onRemoveFavorite = { viewModel.toggleFavorite(it) }
                )
            } else {
                // Library Listing (Playlists Overview or specific Playlist Detail)
                if (selectedPlaylistId != null) {
                    val currentPlaylist = playlists.find { it.id == selectedPlaylistId }
                    PlaylistDetailContent(
                        playlist = currentPlaylist,
                        videos = playlistItems,
                        onBack = { viewModel.selectPlaylist(playlists.firstOrNull()?.id ?: -1) },
                        onPlayVideo = onPlayVideo,
                        onDeleteVideo = { viewModel.removeVideoFromPlaylist(it) },
                        onToggleFavorite = { viewModel.toggleFavorite(it) }
                    )
                } else {
                    PlaylistsOverviewContent(
                        playlists = playlists,
                        onSelectPlaylist = { viewModel.selectPlaylist(it) },
                        onDeletePlaylist = { viewModel.deletePlaylist(it) }
                    )
                }
            }
        }
    }

    // CREATE PLAYLIST DIALOG
    if (showCreatePlaylistDialog) {
        var playlistName by remember { mutableStateOf("") }
        var playlistDesc by remember { mutableStateOf("") }
        var isNameError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            title = {
                Text(
                    "New Playlist",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = playlistName,
                        onValueChange = {
                            playlistName = it
                            isNameError = it.isBlank()
                        },
                        label = { Text("Playlist Name") },
                        isError = isNameError,
                        supportingText = {
                            if (isNameError) {
                                Text("Name cannot be empty", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("playlist_name_input")
                    )

                    OutlinedTextField(
                        value = playlistDesc,
                        onValueChange = { playlistDesc = it },
                        label = { Text("Description (Optional)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("playlist_description_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (playlistName.isNotBlank()) {
                            viewModel.createPlaylist(playlistName.trim(), playlistDesc.trim())
                            showCreatePlaylistDialog = false
                        } else {
                            isNameError = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.Black),
                    modifier = Modifier.testTag("confirm_create_playlist_button")
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // ADD VIDEO TO PLAYLIST DIALOG
    if (showAddVideoDialog && selectedPlaylistId != null) {
        var videoTitle by remember { mutableStateOf("") }
        var videoUrl by remember { mutableStateOf("") }
        var isTitleError by remember { mutableStateOf(false) }
        var isUrlError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddVideoDialog = false },
            title = {
                Text(
                    "Add Video Stream",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Paste any MP4, MKV, 3GP or compatible Android media stream URL details below:",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )

                    OutlinedTextField(
                        value = videoTitle,
                        onValueChange = {
                            videoTitle = it
                            isTitleError = it.isBlank()
                        },
                        label = { Text("Video Title") },
                        isError = isTitleError,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = Color.Gray
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("video_title_input")
                    )

                    OutlinedTextField(
                        value = videoUrl,
                        onValueChange = {
                            videoUrl = it
                            isUrlError = it.isBlank()
                        },
                        label = { Text("Media Stream URL") },
                        isError = isUrlError,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = Color.Gray
                        ),
                        placeholder = { Text("https://example.com/movie.mp4") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("video_url_input")
                    )

                    // Helpful Seed URL Suggestion triggers (VLC Convenience features)
                    Text(
                        text = "Suggested online streams to test:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                "1. Tears of Steel Sci-Fi Movie (MP4)",
                                color = Color.White,
                                fontSize = 11.sp,
                                modifier = Modifier
                                    .clickable {
                                        videoTitle = "Tears of Steel (VFX Movie)"
                                        videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"
                                    }
                                    .padding(vertical = 4.dp)
                            )
                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                            Text(
                                "+ Subway Surfers Gameplay (Short)",
                                color = Color.White,
                                fontSize = 11.sp,
                                modifier = Modifier
                                    .clickable {
                                        videoTitle = "Subway Surfers Test Loop"
                                        videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubwaySurfers.mp4"
                                    }
                                    .padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val validTitle = videoTitle.isNotBlank()
                        val validUrl = videoUrl.trim().startsWith("http")
                        isTitleError = !validTitle
                        isUrlError = !validUrl
                        if (validTitle && validUrl) {
                            viewModel.addVideoToPlaylist(selectedPlaylistId, videoTitle.trim(), videoUrl.trim())
                            showAddVideoDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.Black),
                    modifier = Modifier.testTag("confirm_add_video_button")
                ) {
                    Text("Save Video")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddVideoDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
fun PlaylistsOverviewContent(
    playlists: List<Playlist>,
    onSelectPlaylist: (Int) -> Unit,
    onDeletePlaylist: (Playlist) -> Unit
) {
    if (playlists.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "No Playlists Found",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    "Create or seed standard playlists using the button in the bottom corner.",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "My Libraries",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(playlists) { playlist ->
                Card(
                    onClick = { onSelectPlaylist(playlist.id) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("playlist_card_${playlist.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Visual Icon representing standard file folders
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = playlist.name,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = playlist.description.ifEmpty { "Custom Media Folder Stream Collection" },
                                color = Color.Gray,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        // VLC standard Playlist management deletes
                        IconButton(
                            onClick = { onDeletePlaylist(playlist) },
                            modifier = Modifier.testTag("delete_playlist_button_${playlist.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Playlist",
                                tint = Color.Gray.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VideoItemThumbnail(title: String) {
    val isMkv = title.contains("Sintel", ignoreCase = true) || title.contains("Tears", ignoreCase = true) || title.contains("Elephants", ignoreCase = true) || title.contains(".mkv", ignoreCase = true)
    val is10Bit = title.contains("Tears", ignoreCase = true) || title.contains("Elephants", ignoreCase = true) || title.contains("10bit", ignoreCase = true) || title.contains("10-bit", ignoreCase = true)
    val isH264 = title.contains("Tears", ignoreCase = true) || title.contains("Elephants", ignoreCase = true) || title.contains("Bunny", ignoreCase = true) || title.contains("h264", ignoreCase = true) || title.contains("h.264", ignoreCase = true)

    val gradientColors = when {
        title.contains("Bunny", ignoreCase = true) -> listOf(Color(0xFFE5A823), Color(0xFFE85D04)) // Bunny: Orange-Golden Sun
        title.contains("Sintel", ignoreCase = true) -> listOf(Color(0xFF833AB4), Color(0xFFFD1D1D)) // Sintel: Purple-Crimson Mystic
        title.contains("Tears", ignoreCase = true) -> listOf(Color(0xFF00F2FE), Color(0xFF4FACFE)) // Tears of Steel: Sci-Fi Neon Blue-Teal
        title.contains("Elephants", ignoreCase = true) -> listOf(Color(0xFF11998E), Color(0xFF38EF7D)) // Elephants Dream: Emerald Green
        else -> listOf(Color(0xFF1F1C2C), Color(0xFF928DAB)) // Generic: Slate Charcoal
    }

    Box(
        modifier = Modifier
            .size(72.dp, 50.dp) // Widescreen visual orientation ratio
            .clip(RoundedCornerShape(8.dp))
            .background(
                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = gradientColors
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Center play overlay
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(Color.Black.copy(alpha = 0.45f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }

        // Mini badge bottom right
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(2.dp)
                .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(3.dp))
                .padding(horizontal = 4.dp, vertical = 1.dp)
        ) {
            Text(
                text = when {
                    isMkv -> "MKV"
                    isH264 -> "H264"
                    else -> "MP4"
                },
                color = Color(0xFF00FFCC),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Mini badge bottom start
        if (is10Bit) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(2.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.9f), RoundedCornerShape(3.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = "10B",
                    color = Color.Black,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun PlaylistDetailContent(
    playlist: Playlist?,
    videos: List<PlaylistItem>,
    onBack: () -> Unit,
    onPlayVideo: (PlaylistItem) -> Unit,
    onDeleteVideo: (PlaylistItem) -> Unit,
    onToggleFavorite: (PlaylistItem) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Folder Header Details
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("back_to_playlists_button")) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = playlist?.name ?: "Playlist Contents",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${videos.size} videos available",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 8.dp))

        if (videos.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FeaturedVideo,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Playlist Empty",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        "Click the '+' button in the bottom corner to add a video URL to play.",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp).padding(top = 4.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(videos) { video ->
                    Card(
                        onClick = { onPlayVideo(video) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("video_item_card_${video.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Thumb / Video Indicator
                            VideoItemThumbnail(video.title)

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = video.title,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Row(
                                    modifier = Modifier.padding(top = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Duration Badge
                                    Box(
                                        modifier = Modifier
                                            .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = video.formatDuration(),
                                            color = Color.LightGray,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // Resume Bookmark Check
                                    if (video.lastPosition > 0) {
                                        Text(
                                            text = "Resume at ${video.formatLastPosition()}",
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 10.sp,
                                            maxLines = 1
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.padding(top = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val titleLower = video.title.lowercase()
                                    val isMkv = titleLower.contains("sintel") || titleLower.contains("tears") || titleLower.contains("elephants") || titleLower.contains(".mkv")
                                    val is10Bit = titleLower.contains("tears") || titleLower.contains("elephants") || titleLower.contains("10bit") || titleLower.contains("10-bit")
                                    val isH264 = titleLower.contains("tears") || titleLower.contains("elephants") || titleLower.contains("bunny") || titleLower.contains("h264") || titleLower.contains("h.264")

                                    if (isMkv) {
                                        Box(
                                            modifier = Modifier
                                                .border(0.5.dp, Color(0xFF00E5FF).copy(alpha = 0.5f), RoundedCornerShape(3.dp))
                                                .background(Color(0xFF011627), RoundedCornerShape(3.dp))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text("MKV", color = Color(0xFF00E5FF), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    if (is10Bit) {
                                        Box(
                                            modifier = Modifier
                                                .border(0.5.dp, Color(0xFFFFD200).copy(alpha = 0.5f), RoundedCornerShape(3.dp))
                                                .background(Color(0xFF281F00), RoundedCornerShape(3.dp))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text("10-Bit Color", color = Color(0xFFFFD200), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    if (isH264) {
                                        Box(
                                            modifier = Modifier
                                                .border(0.5.dp, Color(0xFF00FF7F).copy(alpha = 0.5f), RoundedCornerShape(3.dp))
                                                .background(Color(0xFF00220F), RoundedCornerShape(3.dp))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text("H.264 AVC", color = Color(0xFF00FF7F), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            // Favorite Action
                            IconButton(onClick = { onToggleFavorite(video) }) {
                                Icon(
                                    imageVector = if (video.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Pin Favorite",
                                    tint = if (video.isFavorite) MaterialTheme.colorScheme.primary else Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Delete Action
                            IconButton(
                                onClick = { onDeleteVideo(video) },
                                modifier = Modifier.testTag("delete_video_button_${video.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = "Remove video",
                                    tint = Color.Gray.copy(alpha = 0.6f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FavoritesTabContent(
    favorites: List<PlaylistItem>,
    onPlayVideo: (PlaylistItem) -> Unit,
    onRemoveFavorite: (PlaylistItem) -> Unit
) {
    if (favorites.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "No Favorites Saved",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    "Click the heart icon on any playlist video to pin them here for fast cinematic playback.",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp).padding(top = 4.dp)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = "Pinned Favorites",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(favorites) { video ->
                Card(
                    onClick = { onPlayVideo(video) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("favorite_item_card_${video.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        VideoItemThumbnail(video.title)

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = video.title,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                modifier = Modifier.padding(top = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = video.formatDuration(),
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
                                if (video.lastPosition > 0) {
                                    Text(
                                        text = "Bookmark: ${video.formatLastPosition()}",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.padding(top = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val titleLower = video.title.lowercase()
                                val isMkv = titleLower.contains("sintel") || titleLower.contains("tears") || titleLower.contains("elephants") || titleLower.contains(".mkv")
                                val is10Bit = titleLower.contains("tears") || titleLower.contains("elephants") || titleLower.contains("10bit") || titleLower.contains("10-bit")
                                val isH264 = titleLower.contains("tears") || titleLower.contains("elephants") || titleLower.contains("bunny") || titleLower.contains("h264") || titleLower.contains("h.264")

                                if (isMkv) {
                                    Box(
                                        modifier = Modifier
                                            .border(0.5.dp, Color(0xFF00E5FF).copy(alpha = 0.5f), RoundedCornerShape(3.dp))
                                            .background(Color(0xFF011627), RoundedCornerShape(3.dp))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text("MKV", color = Color(0xFF00E5FF), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (is10Bit) {
                                    Box(
                                        modifier = Modifier
                                            .border(0.5.dp, Color(0xFFFFD200).copy(alpha = 0.5f), RoundedCornerShape(3.dp))
                                            .background(Color(0xFF281F00), RoundedCornerShape(3.dp))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                        Text("10-Bit", color = Color(0xFFFFD200), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (isH264) {
                                    Box(
                                        modifier = Modifier
                                            .border(0.5.dp, Color(0xFF00FF7F).copy(alpha = 0.5f), RoundedCornerShape(3.dp))
                                            .background(Color(0xFF00220F), RoundedCornerShape(3.dp))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text("H.264 AVC", color = Color(0xFF00FF7F), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        IconButton(onClick = { onRemoveFavorite(video) }) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Unfavorite",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
