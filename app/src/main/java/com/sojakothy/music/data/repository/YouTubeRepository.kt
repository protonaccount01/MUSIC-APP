package com.sojakothy.music.data.repository

import com.sojakothy.music.data.models.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YouTubeRepository @Inject constructor() {

    private val youtubeService by lazy {
        NewPipe.getService(ServiceList.YouTube.serviceId)
    }

    /** Search YouTube videos and map to Songs */
    suspend fun searchMusic(query: String): Result<List<Song>> = withContext(Dispatchers.IO) {
        runCatching {
            val searchHandler = youtubeService.searchQHFactory.fromQuery(
                query,
                listOf(YoutubeSearchQueryHandlerFactory.VIDEOS),
                ""
            )
            val extractor = youtubeService.getSearchExtractor(searchHandler)
            extractor.fetchPage()

            extractor.initialPage.items
                .filterIsInstance<StreamInfoItem>()
                .map { it.toSong() }
        }
    }

    /** Get direct audio stream URL + full metadata for a video */
    suspend fun getStreamInfo(videoId: String): Result<StreamResult> = withContext(Dispatchers.IO) {
        runCatching {
            val url = "https://www.youtube.com/watch?v=$videoId"
            val extractor = youtubeService.getStreamExtractor(url)
            extractor.fetchPage()

            val audioStreams: List<AudioStream> = extractor.audioStreams
            val bestAudio = audioStreams
                .filter { it.averageBitrate > 0 }
                .maxByOrNull { it.averageBitrate }
                ?: audioStreams.firstOrNull()
                ?: throw IllegalStateException("No audio stream available for $videoId")

            // content = the direct URL for AudioStream
            val audioUrl = bestAudio.content

            val thumbnailUrl = runCatching {
                extractor.thumbnails.maxByOrNull { it.width }?.url
            }.getOrNull() ?: "https://img.youtube.com/vi/$videoId/maxresdefault.jpg"

            val description = runCatching {
                extractor.description?.content ?: ""
            }.getOrDefault("")

            val channelAvatar = runCatching {
                extractor.uploaderAvatars.firstOrNull()?.url ?: ""
            }.getOrDefault("")

            val likeCount = runCatching {
                extractor.likeCount.toLong()
            }.getOrDefault(0L)

            val uploadDate = runCatching {
                extractor.uploadDate?.offsetDateTime()?.toString() ?: ""
            }.getOrDefault("")

            StreamResult(
                audioUrl = audioUrl,
                title = extractor.name,
                artist = extractor.uploaderName ?: "Unknown",
                description = description,
                thumbnailUrl = thumbnailUrl,
                duration = extractor.length,
                viewCount = extractor.viewCount,
                likeCount = likeCount,
                uploadDate = uploadDate,
                channelId = extractor.uploaderUrl ?: "",
                channelAvatarUrl = channelAvatar
            )
        }
    }

    /** Trending = search for popular recent music */
    suspend fun getTrending(): Result<List<Song>> = searchMusic("popular music 2024")

    // --- Private helpers ---

    private fun StreamInfoItem.toSong(): Song {
        val videoId = extractVideoId(this.url)
        return Song(
            id = videoId,
            title = name,
            artist = uploaderName ?: "Unknown",
            description = "",
            thumbnailUrl = thumbnails.maxByOrNull { it.width }?.url
                ?: "https://img.youtube.com/vi/$videoId/maxresdefault.jpg",
            duration = duration,
            viewCount = viewCount,
            likeCount = 0L,
            uploadDate = uploadDate ?: "",
            channelName = uploaderName ?: "Unknown",
            channelId = uploaderUrl ?: "",
            channelAvatarUrl = uploaderAvatars.firstOrNull()?.url ?: ""
        )
    }

    private fun extractVideoId(url: String): String {
        listOf(
            "(?:v=|youtu\\.be/|shorts/)([A-Za-z0-9_-]{11})".toRegex()
        ).forEach { pattern ->
            pattern.find(url)?.groupValues?.get(1)?.let { return it }
        }
        return url.substringAfterLast("/").take(11)
    }

    data class StreamResult(
        val audioUrl: String,
        val title: String,
        val artist: String,
        val description: String,
        val thumbnailUrl: String,
        val duration: Long,
        val viewCount: Long,
        val likeCount: Long,
        val uploadDate: String,
        val channelId: String,
        val channelAvatarUrl: String
    )
}
