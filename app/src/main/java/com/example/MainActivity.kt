package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.CustomVideoPlayer
import com.example.ui.components.VideoLibraryScreens
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.VideoPlayerViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val playerViewModel: VideoPlayerViewModel = viewModel()
                
                val playlists by playerViewModel.playlists.collectAsState()
                val selectedPlaylistId by playerViewModel.selectedPlaylistId.collectAsState()
                val playlistItems by playerViewModel.playlistItems.collectAsState()
                val currentPlayingItem by playerViewModel.currentPlayingItem.collectAsState()
                val favorites by playerViewModel.favorites.collectAsState()
                val repeatMode by playerViewModel.repeatMode.collectAsState()
                val isShuffleEnabled by playerViewModel.isShuffleEnabled.collectAsState()
                val playbackQueue by playerViewModel.playbackQueue.collectAsState()

                if (currentPlayingItem != null) {
                    val activeVideo = currentPlayingItem!!
                    CustomVideoPlayer(
                        item = activeVideo,
                        onPositionChanged = { id, pos -> playerViewModel.updatePlaybackPosition(id, pos) },
                        onDurationChanged = { id, dur -> playerViewModel.updateVideoDuration(id, dur) },
                        onBack = { playerViewModel.setPlayingItem(null) },
                        onPlayNext = { playerViewModel.playNext() },
                        onPlayPrevious = { playerViewModel.playPrevious() },
                        repeatMode = repeatMode,
                        onRepeatModeChanged = { playerViewModel.setRepeatMode(it) },
                        isShuffleEnabled = isShuffleEnabled,
                        onShuffleToggle = { playerViewModel.toggleShuffle() },
                        isFavorite = activeVideo.isFavorite,
                        onFavoriteToggle = { playerViewModel.toggleFavorite(activeVideo) },
                        playbackQueue = playbackQueue,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    VideoLibraryScreens(
                        viewModel = playerViewModel,
                        playlists = playlists,
                        selectedPlaylistId = selectedPlaylistId,
                        playlistItems = playlistItems,
                        favorites = favorites,
                        onPlayVideo = { playerViewModel.setPlayingItem(it) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

