package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class VideoRepository(private val videoDao: VideoDao) {
    val allPlaylists: Flow<List<Playlist>> = videoDao.getAllPlaylists()
    val favoriteItems: Flow<List<PlaylistItem>> = videoDao.getFavoriteItems()
    val allVideos: Flow<List<PlaylistItem>> = videoDao.getAllVideos()

    fun getItemsForPlaylist(playlistId: Int): Flow<List<PlaylistItem>> =
        videoDao.getItemsForPlaylist(playlistId)

    suspend fun getPlaylistById(id: Int): Playlist? = videoDao.getPlaylistById(id)

    suspend fun getItemById(id: Int): PlaylistItem? = videoDao.getItemById(id)

    suspend fun insertPlaylist(playlist: Playlist): Long = videoDao.insertPlaylist(playlist)

    suspend fun updatePlaylist(playlist: Playlist) = videoDao.updatePlaylist(playlist)

    suspend fun deletePlaylist(playlist: Playlist) {
        videoDao.deleteItemsByPlaylistId(playlist.id)
        videoDao.deletePlaylist(playlist)
    }

    suspend fun insertPlaylistItem(item: PlaylistItem): Long = videoDao.insertPlaylistItem(item)

    suspend fun updatePlaylistItem(item: PlaylistItem) = videoDao.updatePlaylistItem(item)

    suspend fun deletePlaylistItem(item: PlaylistItem) = videoDao.deletePlaylistItem(item)

    suspend fun updatePlaybackPosition(id: Int, position: Long) =
        videoDao.updatePlaybackPosition(id, position)

    suspend fun updateDuration(id: Int, duration: Long) =
        videoDao.updateDuration(id, duration)

    suspend fun updateFavoriteStatus(id: Int, isFav: Boolean) =
        videoDao.updateFavoriteStatus(id, isFav)

    suspend fun updateLastPlayedTime(id: Int, time: Long) =
        videoDao.updateLastPlayedTime(id, time)

    // Checks if database is empty, and if so, seeds it with beautiful cinematic standard test streams!
    suspend fun checkAndSeedDatabase() {
        val playlists = allPlaylists.first()
        if (playlists.isEmpty()) {
            // Seed playlist 1
            val sampleId = insertPlaylist(
                Playlist(
                    name = "Cinematic Open Movies",
                    description = "Stunning open-source films hosted on high-speed servers, perfect for testing aspect ratios and player gestures."
                )
            ).toInt()

            insertPlaylistItem(
                PlaylistItem(
                    playlistId = sampleId,
                    title = "Big Buck Bunny (Animation)",
                    url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                    duration = 596000, // approx 9m 56s
                    folder = "Animations"
                )
            )
            insertPlaylistItem(
                PlaylistItem(
                    playlistId = sampleId,
                    title = "Sintel (Fantasy Drama)",
                    url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                    duration = 848000, // approx 14m 8s
                    folder = "Movies"
                )
            )
            insertPlaylistItem(
                PlaylistItem(
                    playlistId = sampleId,
                    title = "Tears of Steel (Sci-Fi VFX)",
                    url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                    duration = 734000, // approx 12m 14s
                    folder = "Sci-Fi"
                )
            )

            // Seed playlist 2
            val techId = insertPlaylist(
                Playlist(
                    name = "Experimental Demo Clips",
                    description = "Short sample clips useful for checking volume/brightness responsiveness and loop modes."
                )
            ).toInt()

            insertPlaylistItem(
                PlaylistItem(
                    playlistId = techId,
                    title = "Elephants Dream (Surreal)",
                    url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                    duration = 653000, // approx 10m 53s
                    folder = "Animations"
                )
            )
            insertPlaylistItem(
                PlaylistItem(
                    playlistId = techId,
                    title = "For Bigger Blazes (Action Promo)",
                    url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                    duration = 15000, // 15 seconds
                    folder = "Promos"
                )
            )
            insertPlaylistItem(
                PlaylistItem(
                    playlistId = techId,
                    title = "Going on Bullrun (Scenic Driving)",
                    url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4",
                    duration = 47000, // 47 seconds
                    folder = "Scenic"
                )
            )
        }
    }
}
