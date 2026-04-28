package com.sojakothy.music.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Binder
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.sojakothy.music.R
import com.sojakothy.music.data.models.PlaybackMode
import com.sojakothy.music.data.models.PlayerState
import com.sojakothy.music.data.models.Song
import com.sojakothy.music.data.repository.MusicRepository
import com.sojakothy.music.ui.activities.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@UnstableApi
@AndroidEntryPoint
class MusicPlayerService : MediaSessionService() {

    @Inject
    lateinit var repository: MusicRepository

    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    inner class LocalBinder : Binder() {
        fun getService(): MusicPlayerService = this@MusicPlayerService
    }

    private val localBinder = LocalBinder()

    companion object {
        const val CHANNEL_ID = "music_playback_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_PLAY = "com.sojakothy.music.PLAY"
        const val ACTION_PAUSE = "com.sojakothy.music.PAUSE"
        const val ACTION_NEXT = "com.sojakothy.music.NEXT"
        const val ACTION_PREV = "com.sojakothy.music.PREV"
        const val ACTION_STOP = "com.sojakothy.music.STOP"

        val playerState = MutableStateFlow(PlayerState())
        val stateFlow: StateFlow<PlayerState> = playerState
    }

    override fun onBind(intent: Intent) = localBinder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initPlayer()
    }

    private fun initPlayer() {
        player = ExoPlayer.Builder(this).build()
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                updateState()
                if (state == Player.STATE_ENDED) handleEnd()
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateState()
                updateNotification()
            }
        })
        serviceScope.launch {
            while (true) {
                delay(500)
                if (player.isPlaying) updateState()
            }
        }
        mediaSession = MediaSession.Builder(this, player).build()
    }

    fun playSong(song: Song, queue: List<Song> = listOf(song)) {
        val idx = queue.indexOf(song).coerceAtLeast(0)
        loadAndPlay(song, queue, idx)
    }

    private fun loadAndPlay(song: Song, queue: List<Song>, idx: Int) {
        serviceScope.launch {
            playerState.value = playerState.value.copy(
                isLoading = true, queue = queue, currentIndex = idx,
                currentSong = song, error = null
            )
            try {
                repository.getAndCacheStreamUrl(song).fold(
                    onSuccess = { url ->
                        val item = MediaItem.Builder()
                            .setUri(url)
                            .setMediaMetadata(
                                MediaMetadata.Builder()
                                    .setTitle(song.title)
                                    .setArtist(song.artist)
                                    .setArtworkUri(android.net.Uri.parse(song.thumbnailUrl))
                                    .build()
                            ).build()
                        player.setMediaItem(item)
                        player.prepare()
                        player.play()
                        playerState.value = playerState.value.copy(
                            isLoading = false, currentSong = song, isPlaying = true
                        )
                        startForeground(NOTIFICATION_ID, buildNotification(song))
                    },
                    onFailure = { e ->
                        playerState.value = playerState.value.copy(
                            isLoading = false, error = e.message
                        )
                    }
                )
            } catch (e: Exception) {
                playerState.value = playerState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun togglePlayPause() {
        if (player.isPlaying) player.pause() else player.play()
        updateState()
    }

    fun seekTo(pos: Long) { player.seekTo(pos); updateState() }

    fun skipNext() {
        val s = playerState.value
        val next = when (s.playbackMode) {
            PlaybackMode.SHUFFLE -> s.queue.indices.random()
            PlaybackMode.REPEAT_ALL -> (s.currentIndex + 1) % s.queue.size
            else -> if (s.hasNext) s.currentIndex + 1 else return
        }
        s.queue.getOrNull(next)?.let { loadAndPlay(it, s.queue, next) }
    }

    fun skipPrev() {
        val s = playerState.value
        if (player.currentPosition > 3000) { player.seekTo(0); return }
        val prev = when (s.playbackMode) {
            PlaybackMode.REPEAT_ALL -> if (s.currentIndex == 0) s.queue.size - 1 else s.currentIndex - 1
            else -> if (s.hasPrevious) s.currentIndex - 1 else return
        }
        s.queue.getOrNull(prev)?.let { loadAndPlay(it, s.queue, prev) }
    }

    fun setMode(mode: PlaybackMode) {
        playerState.value = playerState.value.copy(playbackMode = mode)
    }

    private fun handleEnd() {
        if (playerState.value.playbackMode == PlaybackMode.REPEAT_ONE) {
            player.seekTo(0); player.play()
        } else { skipNext() }
    }

    private fun updateState() {
        playerState.value = playerState.value.copy(
            isPlaying = player.isPlaying,
            currentPosition = player.currentPosition,
            duration = player.duration.coerceAtLeast(0)
        )
    }

    private fun updateNotification() {
        val song = playerState.value.currentSong ?: return
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .notify(NOTIFICATION_ID, buildNotification(song))
    }

    private fun buildNotification(song: Song): Notification {
        val pi = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val playIcon = if (player.isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(song.title)
            .setContentText(song.artist)
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentIntent(pi)
            .addAction(R.drawable.ic_prev, "Prev", actionPI(ACTION_PREV))
            .addAction(playIcon, if (player.isPlaying) "Pause" else "Play", actionPI(ACTION_PLAY))
            .addAction(R.drawable.ic_next, "Next", actionPI(ACTION_NEXT))
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle().setShowActionsInCompactView(0, 1, 2))
            .setOngoing(player.isPlaying)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun actionPI(action: String) = PendingIntent.getService(
        this, action.hashCode(),
        Intent(this, MusicPlayerService::class.java).apply { this.action = action },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_PLAY, ACTION_PAUSE -> togglePlayPause()
            ACTION_NEXT -> skipNext()
            ACTION_PREV -> skipPrev()
            ACTION_STOP -> { player.stop(); stopSelf() }
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "Music Playback", NotificationManager.IMPORTANCE_LOW)
                .apply { setShowBadge(false) }
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
        }
    }

    override fun onGetSession(info: MediaSession.ControllerInfo) = mediaSession

    override fun onDestroy() {
        mediaSession.release(); player.release(); serviceScope.cancel()
        super.onDestroy()
    }
}
