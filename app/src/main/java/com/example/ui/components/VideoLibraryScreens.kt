package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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

    val allVideos by viewModel.allVideos.collectAsState()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsState()

    // Tab state: 0 = Playlists, 1 = Folders, 2 = Recent, 3 = Favorites
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
            // Screen tabs: Playlists, Folders, Recent, Favorites
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
                            "Playlists",
                            fontWeight = if (activeTab == 0) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier.testTag("playlists_tab")
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = {
                        Text(
                            "Folders",
                            fontWeight = if (activeTab == 1) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier.testTag("folders_tab")
                )
                Tab(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "Recent",
                                fontWeight = if (activeTab == 2) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                            if (recentlyPlayed.isNotEmpty()) {
                                Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                    Text(
                                        "${recentlyPlayed.size}",
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    },
                    modifier = Modifier.testTag("recent_tab")
                )
                Tab(
                    selected = activeTab == 3,
                    onClick = { activeTab = 3 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "Favorites",
                                fontWeight = if (activeTab == 3) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                            if (favorites.isNotEmpty()) {
                                Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                    Text(
                                        "${favorites.size}",
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
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
            when (activeTab) {
                0 -> {
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
                1 -> {
                    // Folders / Folder wise video view / folders view
                    FoldersTabContent(
                        allVideos = allVideos,
                        onPlayVideo = onPlayVideo,
                        onToggleFavorite = { viewModel.toggleFavorite(it) }
                    )
                }
                2 -> {
                    // Recent Tab Content
                    RecentTabContent(
                        recentVideos = recentlyPlayed,
                        onPlayVideo = onPlayVideo,
                        onToggleFavorite = { viewModel.toggleFavorite(it) }
                    )
                }
                3 -> {
                    // Favorites Listing Screen
                    FavoritesTabContent(
                        favorites = favorites,
                        onPlayVideo = onPlayVideo,
                        onRemoveFavorite = { viewModel.toggleFavorite(it) }
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

    // DATA CLASS FOR FILE CHOOSERS IN THE DIALOG
    // ADD VIDEO TO PLAYLIST DIALOG
    if (showAddVideoDialog && selectedPlaylistId != null) {
        var activeDialogTab by remember { mutableIntStateOf(0) }
        
        // Manual tab states
        var videoTitle by remember { mutableStateOf("") }
        var videoUrl by remember { mutableStateOf("") }
        var videoFolder by remember { mutableStateOf("Downloads") }
        var isTitleError by remember { mutableStateOf(false) }
        var isUrlError by remember { mutableStateOf(false) }

        // Files/Folders tab states
        var fileSearchQuery by remember { mutableStateOf("") }
        val selectedFileUrls = remember { mutableStateMapOf<String, Boolean>() }
        var selectedDialogFolder by remember { mutableStateOf<String?>(null) }

        val dummyFileCatalog = remember {
            listOf(
                AvailableFile("Big Buck Bunny (Animation)", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4", "Animations"),
                AvailableFile("Sintel (Fantasy Movie)", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4", "Movies"),
                AvailableFile("Tears of Steel (VFX Movie)", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4", "Sci-Fi"),
                AvailableFile("Elephants Dream (Surreal)", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4", "Animations"),
                AvailableFile("For Bigger Blazes (Action Promo)", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4", "Promos"),
                AvailableFile("For Bigger Fun (Comedy Promo)", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4", "Promos"),
                AvailableFile("We Are Going On Bullrun", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4", "Scenic"),
                AvailableFile("Subway Surfers Test Loop", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubwaySurfers.mp4", "Promos"),
                AvailableFile("Sintel HD Trailer (Alternative)", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4", "Movies"),
                AvailableFile("Tears of Steel Behind the Scenes", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4", "Sci-Fi"),
                AvailableFile("Caminandes Escape (Animation Short)", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4", "Animations")
            )
        }

        AlertDialog(
            onDismissRequest = { showAddVideoDialog = false },
            title = {
                Text(
                    "Add Videos",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TabRow(
                        selectedTabIndex = activeDialogTab,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Tab(
                            selected = activeDialogTab == 0,
                            onClick = { activeDialogTab = 0 },
                            text = { Text("Manual", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.AddLink, null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.testTag("add_manual_tab")
                        )
                        Tab(
                            selected = activeDialogTab == 1,
                            onClick = { activeDialogTab = 1 },
                            text = { Text("By Files", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.FeaturedVideo, null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.testTag("add_files_tab")
                        )
                        Tab(
                            selected = activeDialogTab == 2,
                            onClick = { activeDialogTab = 2 },
                            text = { Text("By Folder", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.Folder, null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.testTag("add_folder_tab")
                        )
                    }

                    when (activeDialogTab) {
                        0 -> {
                            // MANUAL ADD SCREEN
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    "Enter media stream details manually below:",
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

                                OutlinedTextField(
                                    value = videoFolder,
                                    onValueChange = { videoFolder = it },
                                    label = { Text("Folder / Category") },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedLabelColor = Color.Gray
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("video_folder_input")
                                )

                                // Clickable Seed triggers for quick testing
                                Text(
                                    "Standard High-Quality Feeds:",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 4.dp)
                                )

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.3f)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            "1. Tears of Steel Sci-Fi Movie (Sci-Fi)",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            modifier = Modifier
                                                .clickable {
                                                    videoTitle = "Tears of Steel (VFX Movie)"
                                                    videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"
                                                    videoFolder = "Sci-Fi"
                                                }
                                                .padding(vertical = 4.dp)
                                                .testTag("seed_tears_manual")
                                        )
                                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                                        Text(
                                            "2. Subway Surfers Test Loop (Promos)",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            modifier = Modifier
                                                .clickable {
                                                    videoTitle = "Subway Surfers Test Loop"
                                                    videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubwaySurfers.mp4"
                                                    videoFolder = "Promos"
                                                }
                                                .padding(vertical = 4.dp)
                                                .testTag("seed_surfers_manual")
                                        )
                                    }
                                }
                            }
                        }
                        1 -> {
                            // BY FILES TAB (SELECT ALL / DESELECT ALL)
                            val filteredFiles = remember(fileSearchQuery) {
                                dummyFileCatalog.filter {
                                    it.title.contains(fileSearchQuery, ignoreCase = true) ||
                                            it.folder.contains(fileSearchQuery, ignoreCase = true)
                                }
                            }

                            Column(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = fileSearchQuery,
                                    onValueChange = { fileSearchQuery = it },
                                    label = { Text("Search system files...") },
                                    leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedLabelColor = Color.Gray
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 6.dp)
                                        .testTag("file_search_input")
                                )

                                // Select All Row
                                val allFilesChecked = filteredFiles.isNotEmpty() && filteredFiles.all { selectedFileUrls[it.url] == true }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val nextState = !allFilesChecked
                                            filteredFiles.forEach { file ->
                                                selectedFileUrls[file.url] = nextState
                                            }
                                        }
                                        .padding(vertical = 6.dp)
                                        .testTag("select_all_files_row")
                                ) {
                                    Checkbox(
                                        checked = allFilesChecked,
                                        onCheckedChange = { checked ->
                                            filteredFiles.forEach { file ->
                                                selectedFileUrls[file.url] = checked
                                            }
                                        },
                                        modifier = Modifier.testTag("select_all_files_checkbox")
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Select All Files (${filteredFiles.size})",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }

                                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(bottom = 6.dp))

                                // Files List scrollable
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (filteredFiles.isEmpty()) {
                                        Text("No matching files found.", color = Color.Gray, fontSize = 12.sp)
                                    } else {
                                        filteredFiles.forEach { file ->
                                            val isChecked = selectedFileUrls[file.url] == true
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { selectedFileUrls[file.url] = !isChecked }
                                                    .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(6.dp))
                                                    .padding(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Checkbox(
                                                    checked = isChecked,
                                                    onCheckedChange = { selectedFileUrls[file.url] = it },
                                                    modifier = Modifier.testTag("file_check_${file.title.lowercase().replace(" ", "_").replace("(", "").replace(")", "")}")
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(file.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                    Text("Folder: ${file.folder}", color = Color.Gray, fontSize = 10.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        2 -> {
                            // BY FOLDER TAB
                            val groupedCatalog = remember { dummyFileCatalog.groupBy { it.folder } }

                            if (selectedDialogFolder == null) {
                                // List of Available system folders
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("Select a folder to import views from:", color = Color.Gray, fontSize = 12.sp)
                                    
                                    groupedCatalog.forEach { (folding, videos) ->
                                        Card(
                                            onClick = { selectedDialogFolder = folding },
                                            modifier = Modifier.fillMaxWidth().testTag("add_folder_${folding.lowercase()}"),
                                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Folder, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(folding, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                    Text("${videos.size} videos inside", color = Color.Gray, fontSize = 11.sp)
                                                }
                                                Icon(Icons.Default.ArrowForward, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            } else {
                                // Specific Folder Inner Multi-Select Content
                                val currentFolder = selectedDialogFolder!!
                                val videosInFolder = groupedCatalog[currentFolder] ?: emptyList()
                                val allFolderChecked = videosInFolder.isNotEmpty() && videosInFolder.all { selectedFileUrls[it.url] == true }

                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                    ) {
                                        IconButton(onClick = { selectedDialogFolder = null }) {
                                            Icon(Icons.Default.ArrowBack, "Back to folder choice", tint = MaterialTheme.colorScheme.primary)
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = "Folder: $currentFolder", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    }

                                    // Select All Folder Items Row
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                val nextState = !allFolderChecked
                                                videosInFolder.forEach { file ->
                                                    selectedFileUrls[file.url] = nextState
                                                }
                                            }
                                            .padding(vertical = 4.dp)
                                            .testTag("select_all_folder_row")
                                    ) {
                                        Checkbox(
                                            checked = allFolderChecked,
                                            onCheckedChange = { checked ->
                                                videosInFolder.forEach { file ->
                                                    selectedFileUrls[file.url] = checked
                                                }
                                            },
                                            modifier = Modifier.testTag("select_all_folder_checkbox")
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Select All in $currentFolder", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }

                                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 6.dp))

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                            .verticalScroll(rememberScrollState()),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        videosInFolder.forEach { file ->
                                            val isChecked = selectedFileUrls[file.url] == true
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { selectedFileUrls[file.url] = !isChecked }
                                                    .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(6.dp))
                                                    .padding(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Checkbox(
                                                    checked = isChecked,
                                                    onCheckedChange = { selectedFileUrls[file.url] = it }
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(file.title, color = Color.White, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                val selectedCount = dummyFileCatalog.count { selectedFileUrls[it.url] == true }
                Button(
                    onClick = {
                        if (activeDialogTab == 0) {
                            // MANUAL SAVE
                            val validTitle = videoTitle.isNotBlank()
                            val validUrl = videoUrl.trim().startsWith("http")
                            isTitleError = !validTitle
                            isUrlError = !validUrl
                            if (validTitle && validUrl) {
                                val targetFolder = if (videoFolder.isBlank()) "Downloads" else videoFolder.trim()
                                viewModel.addVideoToPlaylist(selectedPlaylistId, videoTitle.trim(), videoUrl.trim(), targetFolder)
                                showAddVideoDialog = false
                            }
                        } else {
                            // BULK ADD FROM SELECTED FILES/FOLDERS
                            val videosToAdd = dummyFileCatalog.filter { selectedFileUrls[it.url] == true }
                            if (videosToAdd.isNotEmpty()) {
                                viewModel.addMultipleVideosToPlaylist(
                                    selectedPlaylistId,
                                    videosToAdd.map { Triple(it.title, it.url, it.folder) }
                                )
                            }
                            showAddVideoDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.Black),
                    modifier = Modifier.testTag("confirm_add_video_button")
                ) {
                    Text(
                        if (activeDialogTab == 0) "Save Video" 
                        else "Add Selected ($selectedCount)"
                    )
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

data class AvailableFile(val title: String, val url: String, val folder: String)

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
    var isSelectMode by remember { mutableStateOf(false) }
    val selectedItemIds = remember { mutableStateMapOf<Int, Boolean>() }

    // Clear selection if we change playlists or the active list structure changes
    LaunchedEffect(playlist?.id) {
        selectedItemIds.clear()
        isSelectMode = false
    }

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
            Column(modifier = Modifier.weight(1f)) {
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
            
            if (videos.isNotEmpty()) {
                IconButton(
                    onClick = {
                        isSelectMode = !isSelectMode
                        selectedItemIds.clear()
                    },
                    modifier = Modifier.testTag("playlist_multi_select_toggle")
                ) {
                    Icon(
                        imageVector = if (isSelectMode) Icons.Default.Close else Icons.Default.DoneAll,
                        contentDescription = "Toggle Selection Mode",
                        tint = if (isSelectMode) MaterialTheme.colorScheme.primary else Color.White
                    )
                }
            }
        }

        // Selection Action Bar (Sticky beneath Header)
        if (videos.isNotEmpty() && isSelectMode) {
            val allChecked = videos.isNotEmpty() && videos.all { selectedItemIds[it.id] == true }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        val targetState = !allChecked
                        videos.forEach { video ->
                            selectedItemIds[video.id] = targetState
                        }
                    }
                ) {
                    Checkbox(
                        checked = allChecked,
                        onCheckedChange = { checked ->
                            videos.forEach { video ->
                                selectedItemIds[video.id] = checked
                            }
                        },
                        modifier = Modifier.testTag("playlist_select_all_checkbox")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Select All (${selectedItemIds.count { it.value }}/${videos.size})",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    val countSelected = selectedItemIds.count { it.value }
                    if (countSelected > 0) {
                        // Bulk play
                        IconButton(
                            onClick = {
                                val selectedVideos = videos.filter { selectedItemIds[it.id] == true }
                                if (selectedVideos.isNotEmpty()) {
                                    onPlayVideo(selectedVideos.first())
                                }
                            },
                            modifier = Modifier.testTag("playlist_bulk_play")
                        ) {
                            Icon(Icons.Default.PlayArrow, "Play Selected List", tint = MaterialTheme.colorScheme.primary)
                        }

                        // Bulk delete
                        IconButton(
                            onClick = {
                                videos.filter { selectedItemIds[it.id] == true }.forEach { video ->
                                    onDeleteVideo(video)
                                }
                                selectedItemIds.clear()
                                isSelectMode = false
                            },
                            modifier = Modifier.testTag("playlist_bulk_delete")
                        ) {
                            Icon(Icons.Default.Delete, "Delete Selected List", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(bottom = 4.dp))

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
                    val isChecked = selectedItemIds[video.id] == true
                    Card(
                        onClick = {
                            if (isSelectMode) {
                                selectedItemIds[video.id] = !isChecked
                            } else {
                                onPlayVideo(video)
                            }
                        },
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
                            if (isSelectMode) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { selectedItemIds[video.id] = it },
                                    modifier = Modifier.testTag("item_checkbox_${video.id}")
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }

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

@Composable
fun FoldersTabContent(
    allVideos: List<PlaylistItem>,
    onPlayVideo: (PlaylistItem) -> Unit,
    onToggleFavorite: (PlaylistItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFolder by remember { mutableStateOf<String?>(null) }
    
    // Group videos by folder name
    val foldersMap = remember(allVideos) {
        allVideos.groupBy { it.folder }
    }

    if (selectedFolder != null) {
        val folderName = selectedFolder!!
        val folderVideos = foldersMap[folderName] ?: emptyList()
        
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Folder header / back button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { selectedFolder = null }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back to Folders",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = folderName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                    Text(
                        text = "${folderVideos.size} videos",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (folderVideos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No videos in this folder", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(folderVideos) { video ->
                        FolderVideoItemRow(
                            video = video,
                            onPlayVideo = onPlayVideo,
                            onToggleFavorite = onToggleFavorite
                        )
                    }
                }
            }
        }
    } else {
        // Render folder list
        if (foldersMap.isEmpty()) {
            Box(
                modifier = modifier
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
                        "No Folders Found",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Try creating a playlist or adding some videos first.",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                item {
                    Text(
                        text = "Directories",
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                
                items(foldersMap.keys.toList()) { folderName ->
                    val videosInFolder = foldersMap[folderName] ?: emptyList()
                    FolderCardItem(
                        folderName = folderName,
                        videoCount = videosInFolder.size,
                        onClick = { selectedFolder = folderName }
                    )
                }
            }
        }
    }
}

@Composable
fun FolderCardItem(
    folderName: String,
    videoCount: Int,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("folder_card_${folderName.lowercase()}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folderName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )
                Text(
                    text = "$videoCount videos available",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun FolderVideoItemRow(
    video: PlaylistItem,
    onPlayVideo: (PlaylistItem) -> Unit,
    onToggleFavorite: (PlaylistItem) -> Unit
) {
    Card(
        onClick = { onPlayVideo(video) },
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VideoItemThumbnail(video.title)
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White,
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
                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = video.formatDuration(),
                            color = Color.LightGray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Progress
                    if (video.lastPosition > 0 && video.duration > 0) {
                        val progressPercent = (video.lastPosition.toFloat() / video.duration * 100).toInt()
                        Text(
                            text = "Resumes at $progressPercent%",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            IconButton(onClick = { onToggleFavorite(video) }) {
                Icon(
                    imageVector = if (video.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Toggle Favorite",
                    tint = if (video.isFavorite) Color.Red else Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun RecentTabContent(
    recentVideos: List<PlaylistItem>,
    onPlayVideo: (PlaylistItem) -> Unit,
    onToggleFavorite: (PlaylistItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (recentVideos.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "No Recent History",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Videos you play will appear here for easy resumption.",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recently Played Videos",
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "${recentVideos.size} items",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            items(recentVideos) { video ->
                RecentVideoItemRow(
                    video = video,
                    onPlayVideo = onPlayVideo,
                    onToggleFavorite = onToggleFavorite
                )
            }
        }
    }
}

@Composable
fun RecentVideoItemRow(
    video: PlaylistItem,
    onPlayVideo: (PlaylistItem) -> Unit,
    onToggleFavorite: (PlaylistItem) -> Unit
) {
    Card(
        onClick = { onPlayVideo(video) },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("recent_item_${video.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Video thumbnail
                VideoItemThumbnail(video.title)
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = video.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Text(
                        text = "Folder: ${video.folder}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    
                    Text(
                        text = "Played at ${video.formatLastPosition()} / ${video.formatDuration()}",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                
                IconButton(onClick = { onToggleFavorite(video) }) {
                    Icon(
                        imageVector = if (video.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Toggle Favorite",
                        tint = if (video.isFavorite) Color.Red else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            // Progress Indicator
            if (video.lastPosition > 0 && video.duration > 0) {
                val progressFraction = video.lastPosition.toFloat() / video.duration
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LinearProgressIndicator(
                        progress = progressFraction.coerceIn(0f, 1f),
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )
                    
                    val progressPercent = (progressFraction * 100).toInt()
                    Text(
                        text = "$progressPercent% watched",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
