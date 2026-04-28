package com.sojakothy.music.ui.viewmodels

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import com.sojakothy.music.data.models.PlaybackMode
import com.sojakothy.music.data.models.PlayerState
import com.sojakothy.music.data.models.Song
import com.sojakothy.music.data.repository.MusicRepository
import com.sojakothy.music.service.DownloadWorker
import com.sojakothy.music.service.MusicPlayerService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SearchState {
    object Idle : SearchState()
    object Loading : SearchState()
    data class Success(val results: List<Song>) : SearchState()
    data class Error(val message: String) : SearchState()
}

@UnstableApi
@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: MusicRepository,
    private val workManager: WorkManager
) : ViewModel() {

    val playerState: StateFlow<PlayerState> = MusicPlayerService.stateFlow

    private val _searchState = MutableStateFlow<SearchState>(SearchState.Idle)
    val searchState: StateFlow<SearchState> = _searchState

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val downloadedSongs: StateFlow<List<Song>> = repository.getDownloadedSongs()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val favoriteSongs: StateFlow<List<Song>> = repository.getFavoriteSongs()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private var musicService: MusicPlayerService? = null
    private var isBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            musicService = (binder as? MusicPlayerService.LocalBinder)?.getService()
            isBound = true
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            musicService = null
            isBound = false
        }
    }

    init {
        startAndBindService()
        loadTrending()
    }

    private fun startAndBindService() {
        val intent = Intent(context, MusicPlayerService::class.java)
        context.startService(intent)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    private fun loadTrending() {
        viewModelScope.launch(Dispatchers.IO) {
            _searchState.value = SearchState.Loading
            repository.getTrending().fold(
                onSuccess = { songs -> _searchState.value = SearchState.Success(songs) },
                onFailure = { e -> _searchState.value = SearchState.Error(e.message ?: "Failed to load") }
            )
        }
    }

    fun search(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) { loadTrending(); return }
        viewModelScope.launch(Dispatchers.IO) {
            _searchState.value = SearchState.Loading
            repository.searchYouTube(query).fold(
                onSuccess = { songs ->
                    _searchState.value = if (songs.isEmpty())
                        SearchState.Error("No results for \"$query\"")
                    else SearchState.Success(songs)
                },
                onFailure = { e -> _searchState.value = SearchState.Error(e.message ?: "Search failed") }
            )
        }
    }

    fun playSong(song: Song, queue: List<Song> = listOf(song)) {
        viewModelScope.launch { repository.saveSong(song) }
        val svc = musicService
        if (svc != null) {
            svc.playSong(song, queue)
        } else {
            startAndBindService()
            viewModelScope.launch {
                delay(600)
                musicService?.playSong(song, queue)
            }
        }
    }

    fun togglePlayPause() {
        musicService?.togglePlayPause() ?: sendAction(MusicPlayerService.ACTION_PLAY)
    }

    fun skipToNext() {
        musicService?.skipNext() ?: sendAction(MusicPlayerService.ACTION_NEXT)
    }

    fun skipToPrevious() {
        musicService?.skipPrev() ?: sendAction(MusicPlayerService.ACTION_PREV)
    }

    fun seekTo(position: Long) { musicService?.seekTo(position) }

    fun setPlaybackMode(mode: PlaybackMode) { musicService?.setMode(mode) }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch { repository.toggleFavorite(song) }
    }

    fun downloadSong(song: Song) {
        viewModelScope.launch { repository.saveSong(song) }
        workManager.enqueueUniqueWork(
            "dl_${song.id}",
            ExistingWorkPolicy.KEEP,
            DownloadWorker.buildRequest(song.id, song.title, song.thumbnailUrl)
        )
    }

    fun deleteDownload(song: Song) {
        viewModelScope.launch {
            song.downloadPath?.let { java.io.File(it).takeIf { f -> f.exists() }?.delete() }
            repository.updateDownloadStatus(song.id, false, null)
        }
    }

    private fun sendAction(action: String) {
        context.startService(Intent(context, MusicPlayerService::class.java).apply { this.action = action })
    }

    override fun onCleared() {
        if (isBound) { context.unbindService(connection); isBound = false }
        super.onCleared()
    }
}
