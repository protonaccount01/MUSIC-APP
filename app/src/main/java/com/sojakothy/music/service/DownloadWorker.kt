package com.sojakothy.music.service

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.sojakothy.music.data.database.AppDatabase
import com.sojakothy.music.data.models.DownloadStatus
import com.sojakothy.music.data.models.DownloadTask
import com.sojakothy.music.data.repository.YouTubeRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val database: AppDatabase,
    private val youTubeRepository: YouTubeRepository
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_SONG_ID = "song_id"
        const val KEY_SONG_TITLE = "song_title"
        const val KEY_THUMBNAIL_URL = "thumbnail_url"
        const val TAG = "DownloadWorker"

        fun buildRequest(songId: String, title: String, thumbnailUrl: String): OneTimeWorkRequest {
            val data = workDataOf(
                KEY_SONG_ID to songId,
                KEY_SONG_TITLE to title,
                KEY_THUMBNAIL_URL to thumbnailUrl
            )
            return OneTimeWorkRequestBuilder<DownloadWorker>()
                .setInputData(data)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .addTag(songId)
                .build()
        }
    }

    private val downloadDao = database.downloadDao()
    private val songDao = database.songDao()

    override suspend fun doWork(): Result {
        val songId = inputData.getString(KEY_SONG_ID) ?: return Result.failure()
        val title = inputData.getString(KEY_SONG_TITLE) ?: "Unknown"
        val thumbnailUrl = inputData.getString(KEY_THUMBNAIL_URL) ?: ""

        Log.d(TAG, "Starting download for: $title ($songId)")

        // Update status to downloading
        downloadDao.updateProgress(songId, DownloadStatus.DOWNLOADING, 0)

        return try {
            // Get stream URL
            val streamResult = youTubeRepository.getStreamInfo(songId).getOrThrow()
            val audioUrl = streamResult.audioUrl

            // Set up download directory
            val downloadDir = File(applicationContext.getExternalFilesDir(null), "Music")
            if (!downloadDir.exists()) downloadDir.mkdirs()

            val fileName = "${sanitizeFileName(title)}_$songId.m4a"
            val outputFile = File(downloadDir, fileName)

            // Download the file
            downloadFile(audioUrl, outputFile, songId)

            // Update DB
            val filePath = outputFile.absolutePath
            downloadDao.updateCompletion(songId, DownloadStatus.COMPLETED, filePath)
            songDao.updateDownloadStatus(songId, true, filePath)

            Log.d(TAG, "Download completed: $filePath")
            Result.success(workDataOf("file_path" to filePath))

        } catch (e: Exception) {
            Log.e(TAG, "Download failed for $songId", e)
            downloadDao.updateProgress(songId, DownloadStatus.FAILED, 0)
            Result.failure(workDataOf("error" to e.message))
        }
    }

    private suspend fun downloadFile(url: String, outputFile: File, songId: String) {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url(url)
            .addHeader("User-Agent",
                "Mozilla/5.0 (Android) AppleWebKit/537.36 Chrome/120.0 Mobile Safari/537.36")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("HTTP ${response.code}")

            val body = response.body ?: throw Exception("Empty response body")
            val totalBytes = body.contentLength()

            FileOutputStream(outputFile).use { output ->
                val buffer = ByteArray(8192)
                var downloadedBytes = 0L
                var lastProgressUpdate = 0

                body.byteStream().use { input ->
                    var bytes: Int
                    while (input.read(buffer).also { bytes = it } != -1) {
                        output.write(buffer, 0, bytes)
                        downloadedBytes += bytes

                        if (totalBytes > 0) {
                            val progress = ((downloadedBytes * 100) / totalBytes).toInt()
                            if (progress - lastProgressUpdate >= 5) {
                                lastProgressUpdate = progress
                                downloadDao.updateProgress(
                                    songId, DownloadStatus.DOWNLOADING, progress
                                )
                                setProgress(workDataOf("progress" to progress))
                            }
                        }
                    }
                }
            }
        }
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9\\s_-]"), "")
            .replace(Regex("\\s+"), "_")
            .take(50)
    }
}
