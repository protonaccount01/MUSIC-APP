package com.sojakothy.music.ui.activities

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.bumptech.glide.Glide
import com.sojakothy.music.R
import com.sojakothy.music.data.models.PlayerState
import com.sojakothy.music.databinding.ActivityMainBinding
import com.sojakothy.music.ui.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@UnstableApi
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var navController: NavController

    private val notifPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or denied - app works either way */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        setupMiniPlayer()
        requestPermissions()
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        binding.bottomNav.setupWithNavController(navController)
    }

    private fun setupMiniPlayer() {
        lifecycleScope.launch {
            viewModel.playerState.collectLatest { state ->
                updateMiniPlayer(state)
            }
        }

        // Click on mini player area → go to full player
        binding.miniPlayerContainer.setOnClickListener {
            navController.navigate(R.id.playerFragment)
        }

        // Play/Pause button inside the included view
        binding.miniPlayer.miniPlayerPlayPause.setOnClickListener {
            viewModel.togglePlayPause()
        }

        // Next button inside the included view
        binding.miniPlayer.miniPlayerNext.setOnClickListener {
            viewModel.skipToNext()
        }
    }

    private fun updateMiniPlayer(state: PlayerState) {
        if (state.currentSong == null) {
            binding.miniPlayerContainer.visibility = View.GONE
            return
        }

        binding.miniPlayerContainer.visibility = View.VISIBLE

        // Access children via the included layout binding object
        binding.miniPlayer.miniPlayerTitle.text = state.currentSong.title
        binding.miniPlayer.miniPlayerArtist.text = state.currentSong.artist

        Glide.with(this)
            .load(state.currentSong.thumbnailUrl)
            .placeholder(R.drawable.ic_music_note)
            .centerCrop()
            .into(binding.miniPlayer.miniPlayerThumb)

        binding.miniPlayer.miniPlayerPlayPause.setImageResource(
            if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        )

        // Progress bar (this ID is directly in activity_main.xml, not in the include)
        if (state.duration > 0) {
            binding.miniPlayerProgress.progress =
                ((state.currentPosition * 100) / state.duration).toInt()
        }
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
