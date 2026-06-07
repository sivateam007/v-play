package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {
    // Playlists
    @Query("SELECT * FROM playlists ORDER BY dateCreated DESC")
    fun getAllPlaylists(): Flow<List<Playlist>>

    @Query("SELECT * FROM playlists WHERE id = :id LIMIT 1")
    suspend fun getPlaylistById(id: Int): Playlist?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist): Long

    @Update
    suspend fun updatePlaylist(playlist: Playlist)

    @Delete
    suspend fun deletePlaylist(playlist: Playlist)

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId")
    suspend fun deleteItemsByPlaylistId(playlistId: Int)

    // Playlist Items
    @Query("SELECT * FROM playlist_items WHERE playlistId = :playlistId ORDER BY dateAdded ASC")
    fun getItemsForPlaylist(playlistId: Int): Flow<List<PlaylistItem>>

    @Query("SELECT * FROM playlist_items WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: Int): PlaylistItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistItem(item: PlaylistItem): Long

    @Update
    suspend fun updatePlaylistItem(item: PlaylistItem)

    @Delete
    suspend fun deletePlaylistItem(item: PlaylistItem)

    @Query("UPDATE playlist_items SET lastPosition = :position WHERE id = :id")
    suspend fun updatePlaybackPosition(id: Int, position: Long)

    @Query("UPDATE playlist_items SET duration = :duration WHERE id = :id")
    suspend fun updateDuration(id: Int, duration: Long)

    @Query("SELECT * FROM playlist_items WHERE isFavorite = 1 ORDER BY dateAdded DESC")
    fun getFavoriteItems(): Flow<List<PlaylistItem>>

    @Query("UPDATE playlist_items SET isFavorite = :isFav WHERE id = :id")
    suspend fun updateFavoriteStatus(id: Int, isFav: Boolean)
}
