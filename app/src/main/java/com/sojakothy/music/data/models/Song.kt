package com.sojakothy.music.data.models

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "songs")
data class Song(
    @PrimaryKey
    val id: String,                     // YouTube video ID
    val title: String,
    val artist: String,
    val description: String,
    val thumbnailUrl: String,
    val duration: Long,                 // duration in seconds
    val viewCount: Long,
    val likeCount: Long,
    val uploadDate: String,
    val channelName: String,
    val channelId: String,
    val channelAvatarUrl: String,
    val streamUrl: String? = null,      // cached stream URL (expires)
    val downloadPath: String? = null,   // local download path
    val isDownloaded: Boolean = false,
    val isFavorite: Boolean = false,
    val addedToDbAt: Long = System.currentTimeMillis()
) : Parcelable {

    fun isLocallyAvailable(): Boolean = isDownloaded && !downloadPath.isNullOrEmpty()

    fun formattedDuration(): String {
        val hours = duration / 3600
        val minutes = (duration % 3600) / 60
        val seconds = duration % 60
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }

    fun formattedViews(): String {
        return when {
            viewCount >= 1_000_000_000 -> String.format("%.1fB", viewCount / 1_000_000_000.0)
            viewCount >= 1_000_000 -> String.format("%.1fM", viewCount / 1_000_000.0)
            viewCount >= 1_000 -> String.format("%.1fK", viewCount / 1_000.0)
            else -> viewCount.toString()
        }
    }
}
