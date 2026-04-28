package com.sojakothy.music.data.models

data class PlayerState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val playbackMode: PlaybackMode = PlaybackMode.NORMAL,
    val queue: List<Song> = emptyList(),
    val currentIndex: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val progress: Float
        get() = if (duration > 0) currentPosition.toFloat() / duration else 0f

    val hasNext: Boolean
        get() = currentIndex < queue.size - 1 || playbackMode == PlaybackMode.REPEAT_ALL

    val hasPrevious: Boolean
        get() = currentIndex > 0 || playbackMode == PlaybackMode.REPEAT_ALL
}

enum class PlaybackMode {
    NORMAL,
    REPEAT_ONE,
    REPEAT_ALL,
    SHUFFLE
}
