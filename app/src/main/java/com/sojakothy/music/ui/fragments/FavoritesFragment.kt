package com.sojakothy.music.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.sojakothy.music.databinding.FragmentDownloadsBinding
import com.sojakothy.music.ui.adapters.SongAdapter
import com.sojakothy.music.ui.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@UnstableApi
@AndroidEntryPoint
class FavoritesFragment : Fragment() {

    private var _binding: FragmentDownloadsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var adapter: SongAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDownloadsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = SongAdapter(
            onSongClick = { song, queue -> viewModel.playSong(song, queue) },
            onDownloadClick = { song -> viewModel.downloadSong(song) },
            onFavoriteClick = { song -> viewModel.toggleFavorite(song) }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = this@FavoritesFragment.adapter
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.favoriteSongs.collectLatest { songs ->
                adapter.submitList(songs)
                binding.emptyState.visibility = if (songs.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
