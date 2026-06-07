package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class VideoPlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = VideoRepository(database.videoDao())

    // All Playlists
    val playlists: StateFlow<List<Playlist>> = repository.allPlaylists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Favorites
    val favorites: StateFlow<List<PlaylistItem>> = repository.favoriteItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All videos
    val allVideos: StateFlow<List<PlaylistItem>> = repository.allVideos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Recently played
    val recentlyPlayed: StateFlow<List<PlaylistItem>> = repository.allVideos
        .map { videos ->
            videos.filter { it.lastPlayedTime > 0 }
                .sortedByDescending { it.lastPlayedTime }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Current playlist selection
    private val _selectedPlaylistId = MutableStateFlow<Int?>(null)
    val selectedPlaylistId = _selectedPlaylistId.asStateFlow()

    // Playlist items for selected playlist
    val playlistItems: StateFlow<List<PlaylistItem>> = _selectedPlaylistId
        .flatMapLatest { id ->
            if (id != null) {
                repository.getItemsForPlaylist(id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Current playing video
    private val _currentPlayingItem = MutableStateFlow<PlaylistItem?>(null)
    val currentPlayingItem = _currentPlayingItem.asStateFlow()

    // Active playback queue (list of PlaylistItem)
    private val _playbackQueue = MutableStateFlow<List<PlaylistItem>>(emptyList())
    val playbackQueue = _playbackQueue.asStateFlow()

    // Repeat Mode (0 = No Repeat, 1 = Repeat All, 2 = Repeat One)
    private val _repeatMode = MutableStateFlow(0)
    val repeatMode = _repeatMode.asStateFlow()

    // Shuffle Mode
    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled = _isShuffleEnabled.asStateFlow()

    init {
        viewModelScope.launch {
            repository.checkAndSeedDatabase()
            // Set first playlist as selected by default when playlists are loaded
            playlists.filter { it.isNotEmpty() }.first().let { list ->
                if (_selectedPlaylistId.value == null && list.isNotEmpty()) {
                    selectPlaylist(list.first().id)
                }
            }
        }
    }

    fun selectPlaylist(id: Int) {
        _selectedPlaylistId.value = id
        // Sync the playback queue with playlist items
        viewModelScope.launch {
            repository.getItemsForPlaylist(id).first().let { items ->
                _playbackQueue.value = if (_isShuffleEnabled.value) items.shuffled() else items
            }
        }
    }

    fun setPlayingItem(item: PlaylistItem?) {
        _currentPlayingItem.value = item
        // Update selection if item belongs to another playlist
        item?.let {
            _selectedPlaylistId.value = it.playlistId
            viewModelScope.launch {
                repository.updateLastPlayedTime(it.id, System.currentTimeMillis())
                val dbItem = repository.getItemById(it.id)
                if (dbItem != null) {
                    // Update latest position from database for resume!
                    _currentPlayingItem.value = dbItem
                }
                // Refresh queue
                repository.getItemsForPlaylist(it.playlistId).first().let { items ->
                    _playbackQueue.value = if (_isShuffleEnabled.value) items.shuffled() else items
                }
            }
        }
    }

    fun createPlaylist(name: String, description: String) {
        viewModelScope.launch {
            val newId = repository.insertPlaylist(Playlist(name = name, description = description))
            selectPlaylist(newId.toInt())
        }
    }

    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            repository.deletePlaylist(playlist)
            val remPlaylists = repository.allPlaylists.first()
            if (remPlaylists.isNotEmpty()) {
                selectPlaylist(remPlaylists.first().id)
            } else {
                _selectedPlaylistId.value = null
                _playbackQueue.value = emptyList()
            }
        }
    }

    fun addVideoToPlaylist(playlistId: Int, title: String, url: String, folder: String = "Downloads") {
        viewModelScope.launch {
            val newItem = PlaylistItem(playlistId = playlistId, title = title, url = url, folder = folder)
            repository.insertPlaylistItem(newItem)
            // If the selected playlist is active, reload queue
            if (_selectedPlaylistId.value == playlistId) {
                repository.getItemsForPlaylist(playlistId).first().let { items ->
                    _playbackQueue.value = if (_isShuffleEnabled.value) items.shuffled() else items
                }
            }
        }
    }

    fun addMultipleVideosToPlaylist(playlistId: Int, videosList: List<Triple<String, String, String>>) {
        viewModelScope.launch {
            videosList.forEach { (title, url, folder) ->
                val newItem = PlaylistItem(playlistId = playlistId, title = title, url = url, folder = folder)
                repository.insertPlaylistItem(newItem)
            }
            if (_selectedPlaylistId.value == playlistId) {
                repository.getItemsForPlaylist(playlistId).first().let { items ->
                    _playbackQueue.value = if (_isShuffleEnabled.value) items.shuffled() else items
                }
            }
        }
    }

    fun removeVideoFromPlaylist(item: PlaylistItem) {
        viewModelScope.launch {
            repository.deletePlaylistItem(item)
            if (_currentPlayingItem.value?.id == item.id) {
                playNext()
            }
            // Reload queue
            if (_selectedPlaylistId.value == item.playlistId) {
                repository.getItemsForPlaylist(item.playlistId).first().let { items ->
                    _playbackQueue.value = if (_isShuffleEnabled.value) items.shuffled() else items
                }
            }
        }
    }

    fun toggleFavorite(item: PlaylistItem) {
        viewModelScope.launch {
            val updatedFav = !item.isFavorite
            repository.updateFavoriteStatus(item.id, updatedFav)
            // Update active item if matched
            if (_currentPlayingItem.value?.id == item.id) {
                _currentPlayingItem.value = _currentPlayingItem.value?.copy(isFavorite = updatedFav)
            }
            // Update in selected items list
            _selectedPlaylistId.value?.let { playlistId ->
                repository.getItemsForPlaylist(playlistId).first().let { items ->
                    _playbackQueue.value = if (_isShuffleEnabled.value) items.shuffled() else items
                }
            }
        }
    }

    fun updatePlaybackPosition(itemId: Int, position: Long) {
        viewModelScope.launch {
            repository.updatePlaybackPosition(itemId, position)
            // Local update of playing item
            if (_currentPlayingItem.value?.id == itemId) {
                _currentPlayingItem.value = _currentPlayingItem.value?.copy(lastPosition = position)
            }
        }
    }

    fun updateVideoDuration(itemId: Int, duration: Long) {
        viewModelScope.launch {
            repository.updateDuration(itemId, duration)
            if (_currentPlayingItem.value?.id == itemId) {
                _currentPlayingItem.value = _currentPlayingItem.value?.copy(duration = duration)
            }
        }
    }

    fun toggleShuffle() {
        _isShuffleEnabled.value = !_isShuffleEnabled.value
        viewModelScope.launch {
            _selectedPlaylistId.value?.let { playlistId ->
                val items = repository.getItemsForPlaylist(playlistId).first()
                _playbackQueue.value = if (_isShuffleEnabled.value) items.shuffled() else items
            }
        }
    }

    fun setRepeatMode(mode: Int) {
        // Mode 0: No repeat, 1: Repeat All, 2: Repeat One
        _repeatMode.value = mode
    }

    fun playNext() {
        val currentQueue = _playbackQueue.value
        val currentItem = _currentPlayingItem.value ?: return
        if (currentQueue.isEmpty()) return

        val currentIndex = currentQueue.indexOfFirst { it.id == currentItem.id }
        if (currentIndex == -1) {
            setPlayingItem(currentQueue.firstOrNull())
            return
        }

        val nextIndex = currentIndex + 1
        if (nextIndex < currentQueue.size) {
            setPlayingItem(currentQueue[nextIndex])
        } else {
            // End of queue
            if (_repeatMode.value == 1) { // Repeat all
                setPlayingItem(currentQueue.firstOrNull())
            } else {
                setPlayingItem(null) // Stop playing
            }
        }
    }

    fun playPrevious() {
        val currentQueue = _playbackQueue.value
        val currentItem = _currentPlayingItem.value ?: return
        if (currentQueue.isEmpty()) return

        val currentIndex = currentQueue.indexOfFirst { it.id == currentItem.id }
        if (currentIndex == -1) {
            setPlayingItem(currentQueue.firstOrNull())
            return
        }

        val prevIndex = currentIndex - 1
        if (prevIndex >= 0) {
            setPlayingItem(currentQueue[prevIndex])
        } else {
            if (_repeatMode.value == 1) { // Repeat all
                setPlayingItem(currentQueue.lastOrNull())
            } else {
                setPlayingItem(currentQueue.firstOrNull()) // Rewind to first
            }
        }
    }
}
