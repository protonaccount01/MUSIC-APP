package com.sojakothy.music.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "download_tasks")
data class DownloadTask(
    @PrimaryKey
    val songId: String,
    val title: String,
    val thumbnailUrl: String,
    val status: DownloadStatus = DownloadStatus.PENDING,
    val progress: Int = 0,
    val filePath: String? = null,
    val fileSize: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val workerId: String? = null
)

enum class DownloadStatus {
    PENDING,
    DOWNLOADING,
    COMPLETED,
    FAILED,
    CANCELLED
}
