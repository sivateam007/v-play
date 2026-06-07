package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlist_items")
data class PlaylistItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val playlistId: Int, // The parent playlist ID
    val title: String,
    val url: String,
    val duration: Long = 0, // Duration in milliseconds
    val lastPosition: Long = 0, // Resume seek position in milliseconds
    val isFavorite: Boolean = false,
    val dateAdded: Long = System.currentTimeMillis(),
    val folder: String = "Movies",
    val lastPlayedTime: Long = 0
) {
    // Utility to get nice displayable duration
    fun formatDuration(): String {
        if (duration <= 0) return "--:--"
        val totalSecs = duration / 1000
        val hours = totalSecs / 3600
        val minutes = (totalSecs % 3600) / 60
        val seconds = totalSecs % 60
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    fun formatLastPosition(): String {
        if (lastPosition <= 0) return "00:00"
        val totalSecs = lastPosition / 1000
        val hours = totalSecs / 3600
        val minutes = (totalSecs % 3600) / 60
        val seconds = totalSecs % 60
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }
}
