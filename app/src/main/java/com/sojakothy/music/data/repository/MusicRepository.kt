package com.sojakothy.music.data.repository

import com.sojakothy.music.data.database.AppDatabase
import com.sojakothy.music.data.models.Song
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepository @Inject constructor(
    private val database: AppDatabase,
    private val youTubeRepository: YouTubeRepository
) {
    private val songDao = database.songDao()
    private val downloadDao = database.downloadDao()

    // --- Local DB operations ---

    fun getDownloadedSongs(): Flow<List<Song>> = songDao.getDownloadedSongs()

    fun getFavoriteSongs(): Flow<List<Song>> = songDao.getFavoriteSongs()

    suspend fun getSongById(id: String): Song? = songDao.getSongById(id)

    suspend fun saveSong(song: Song) = songDao.insertSong(song)

    suspend fun updateSong(song: Song) = songDao.updateSong(song)

    suspend fun toggleFavorite(song: Song) {
        songDao.updateFavorite(song.id, !song.isFavorite)
    }

    suspend fun updateDownloadStatus(songId: String, isDownloaded: Boolean, path: String?) {
        songDao.updateDownloadStatus(songId, isDownloaded, path)
    }

    suspend fun deleteSong(song: Song) = songDao.deleteSong(song)

    suspend fun searchLocalSongs(query: String): List<Song> = songDao.searchLocalSongs(query)

    // --- YouTube / Online operations ---

    suspend fun searchYouTube(query: String): Result<List<Song>> =
        youTubeRepository.searchMusic(query)

    suspend fun getTrending(): Result<List<Song>> = youTubeRepository.getTrending()

    suspend fun getAndCacheStreamUrl(song: Song): Result<String> {
        // Check if we have a valid local file first
        if (song.isDownloaded && !song.downloadPath.isNullOrEmpty()) {
            return Result.success(song.downloadPath)
        }

        // Fetch from YouTube
        return youTubeRepository.getStreamInfo(song.id).map { streamResult ->
            // Cache the stream URL
            songDao.updateStreamUrl(song.id, streamResult.audioUrl)

            // Update full metadata if needed
            val updatedSong = song.copy(
                title = streamResult.title.ifEmpty { song.title },
                artist = streamResult.artist.ifEmpty { song.artist },
                description = streamResult.description,
                thumbnailUrl = streamResult.thumbnailUrl.ifEmpty { song.thumbnailUrl },
                duration = if (streamResult.duration > 0) streamResult.duration else song.duration,
                viewCount = streamResult.viewCount,
                channelAvatarUrl = streamResult.channelAvatarUrl.ifEmpty { song.channelAvatarUrl },
                streamUrl = streamResult.audioUrl
            )
            songDao.insertSong(updatedSong)

            streamResult.audioUrl
        }
    }

    suspend fun getStreamInfoFull(videoId: String) =
        youTubeRepository.getStreamInfo(videoId)
}
