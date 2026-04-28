package com.sojakothy.music.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.sojakothy.music.R
import com.sojakothy.music.databinding.FragmentHomeBinding
import com.sojakothy.music.ui.adapters.SongAdapter
import com.sojakothy.music.ui.viewmodels.MainViewModel
import com.sojakothy.music.ui.viewmodels.SearchState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

@UnstableApi
@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var songAdapter: SongAdapter

    // Debounce search input
    private val searchQueryFlow = MutableStateFlow("")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    @OptIn(FlowPreview::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSearch()
        observeState()
    }

    private fun setupRecyclerView() {
        songAdapter = SongAdapter(
            onSongClick = { song, queue -> viewModel.playSong(song, queue) },
            onDownloadClick = { song -> viewModel.downloadSong(song) },
            onFavoriteClick = { song -> viewModel.toggleFavorite(song) }
        )
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = songAdapter
            setHasFixedSize(true)
        }
    }

    @OptIn(FlowPreview::class)
    private fun setupSearch() {
        // Single listener — debounce via flow
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                viewModel.search(query ?: "")
                binding.searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                searchQueryFlow.value = newText ?: ""
                return true
            }
        })

        // Debounced auto-search (500ms after typing stops)
        viewLifecycleOwner.lifecycleScope.launch {
            searchQueryFlow
                .debounce(500L)
                .collectLatest { query ->
                    viewModel.search(query)
                }
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.searchState.collectLatest { state ->
                when (state) {
                    is SearchState.Idle -> {
                        binding.progressBar.visibility = View.GONE
                        binding.errorText.visibility = View.GONE
                    }
                    is SearchState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.errorText.visibility = View.GONE
                        binding.recyclerView.visibility = View.GONE
                    }
                    is SearchState.Success -> {
                        binding.progressBar.visibility = View.GONE
                        binding.errorText.visibility = View.GONE
                        binding.recyclerView.visibility = View.VISIBLE
                        songAdapter.submitList(state.results)
                        binding.sectionTitle.setText(
                            if (viewModel.searchQuery.value.isBlank())
                                R.string.trending_music
                            else
                                R.string.search_results
                        )
                    }
                    is SearchState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        binding.recyclerView.visibility = View.GONE
                        binding.errorText.visibility = View.VISIBLE
                        binding.errorText.text = state.message
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
