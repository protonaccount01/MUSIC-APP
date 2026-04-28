package com.sojakothy.music.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import com.bumptech.glide.Glide
import com.sojakothy.music.R
import com.sojakothy.music.data.models.PlaybackMode
import com.sojakothy.music.databinding.FragmentPlayerBinding
import com.sojakothy.music.ui.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@UnstableApi
@AndroidEntryPoint
class PlayerFragment : Fragment() {

    private var _binding: FragmentPlayerBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()
    private var isSeeking = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupControls()
        observeState()
    }

    private fun setupControls() {
        binding.btnPlayPause.setOnClickListener { viewModel.togglePlayPause() }
        binding.btnNext.setOnClickListener { viewModel.skipToNext() }
        binding.btnPrev.setOnClickListener { viewModel.skipToPrevious() }

        binding.btnRepeat.setOnClickListener {
            val currentMode = viewModel.playerState.value.playbackMode
            val nextMode = when (currentMode) {
                PlaybackMode.NORMAL -> PlaybackMode.REPEAT_ALL
                PlaybackMode.REPEAT_ALL -> PlaybackMode.REPEAT_ONE
                PlaybackMode.REPEAT_ONE -> PlaybackMode.SHUFFLE
                PlaybackMode.SHUFFLE -> PlaybackMode.NORMAL
            }
            viewModel.setPlaybackMode(nextMode)
        }

        binding.btnFavorite.setOnClickListener {
            viewModel.playerState.value.currentSong?.let {
                viewModel.toggleFavorite(it)
            }
        }

        binding.btnDownload.setOnClickListener {
            viewModel.playerState.value.currentSong?.let {
                viewModel.downloadSong(it)
            }
        }

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val duration = viewModel.playerState.value.duration
                    if (duration > 0) {
                        val position = (progress * duration) / 100
                        binding.tvCurrentTime.text = formatTime(position)
                    }
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) { isSeeking = true }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                isSeeking = false
                val duration = viewModel.playerState.value.duration
                val position = ((seekBar?.progress ?: 0) * duration) / 100
                viewModel.seekTo(position)
            }
        })
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.playerState.collectLatest { state ->
                val song = state.currentSong ?: return@collectLatest

                binding.tvTitle.text = song.title
                binding.tvArtist.text = song.artist
                binding.tvAlbum.text = song.channelName

                Glide.with(this@PlayerFragment)
                    .load(song.thumbnailUrl)
                    .placeholder(R.drawable.ic_music_note)
                    .into(binding.ivAlbumArt)

                binding.btnPlayPause.setImageResource(
                    if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                )

                // Update seek bar
                if (!isSeeking && state.duration > 0) {
                    val progress = ((state.currentPosition * 100) / state.duration).toInt()
                    binding.seekBar.progress = progress
                }

                binding.tvCurrentTime.text = formatTime(state.currentPosition)
                binding.tvTotalTime.text = formatTime(state.duration)

                // Playback mode icon
                val modeIcon = when (state.playbackMode) {
                    PlaybackMode.NORMAL -> R.drawable.ic_repeat_off
                    PlaybackMode.REPEAT_ALL -> R.drawable.ic_repeat
                    PlaybackMode.REPEAT_ONE -> R.drawable.ic_repeat_one
                    PlaybackMode.SHUFFLE -> R.drawable.ic_shuffle
                }
                binding.btnRepeat.setImageResource(modeIcon)

                // Loading state
                binding.loadingIndicator.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                binding.btnPlayPause.isEnabled = !state.isLoading

                // Error state
                state.error?.let { error ->
                    binding.tvError.visibility = View.VISIBLE
                    binding.tvError.text = error
                } ?: run { binding.tvError.visibility = View.GONE }
            }
        }
    }

    private fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
