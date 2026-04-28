package com.sojakothy.music.data.database

import androidx.room.*
import com.sojakothy.music.data.models.DownloadStatus
import com.sojakothy.music.data.models.DownloadTask
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {

    @Query("SELECT * FROM download_tasks ORDER BY createdAt DESC")
    fun getAllDownloadTasks(): Flow<List<DownloadTask>>

    @Query("SELECT * FROM download_tasks WHERE status = 'DOWNLOADING' OR status = 'PENDING'")
    fun getActiveDownloads(): Flow<List<DownloadTask>>

    @Query("SELECT * FROM download_tasks WHERE songId = :songId")
    suspend fun getDownloadTask(songId: String): DownloadTask?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownloadTask(task: DownloadTask)

    @Update
    suspend fun updateDownloadTask(task: DownloadTask)

    @Query("UPDATE download_tasks SET status = :status, progress = :progress WHERE songId = :songId")
    suspend fun updateProgress(songId: String, status: DownloadStatus, progress: Int)

    @Query("UPDATE download_tasks SET status = :status, filePath = :path WHERE songId = :songId")
    suspend fun updateCompletion(songId: String, status: DownloadStatus, path: String?)

    @Query("DELETE FROM download_tasks WHERE songId = :songId")
    suspend fun deleteDownloadTask(songId: String)

    @Query("DELETE FROM download_tasks WHERE status = 'COMPLETED'")
    suspend fun clearCompletedTasks()
}
