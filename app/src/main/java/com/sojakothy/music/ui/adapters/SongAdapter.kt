package com.sojakothy.music.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.sojakothy.music.R
import com.sojakothy.music.data.models.Song
import com.sojakothy.music.databinding.ItemSongBinding

class SongAdapter(
    private val onSongClick: (Song, List<Song>) -> Unit,
    private val onDownloadClick: (Song) -> Unit,
    private val onFavoriteClick: (Song) -> Unit,
    private val isDownloadedView: Boolean = false
) : ListAdapter<Song, SongAdapter.SongViewHolder>(SongDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val binding = ItemSongBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return SongViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        holder.bind(getItem(position), currentList)
    }

    inner class SongViewHolder(private val binding: ItemSongBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(song: Song, queue: List<Song>) {
            binding.tvTitle.text = song.title
            binding.tvArtist.text = song.artist
            binding.tvDuration.text = song.formattedDuration()
            binding.tvViews.text = if (!isDownloadedView) "${song.formattedViews()} views" else ""

            Glide.with(binding.root.context)
                .load(song.thumbnailUrl)
                .placeholder(R.drawable.ic_music_note)
                .centerCrop()
                .into(binding.ivThumbnail)

            // Favorite
            binding.btnFavorite.setImageResource(
                if (song.isFavorite) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_outline
            )

            // Download icon
            if (isDownloadedView) {
                binding.btnDownload.setImageResource(R.drawable.ic_delete)
                binding.downloadedBadge.visibility = View.GONE
            } else {
                binding.btnDownload.setImageResource(
                    if (song.isDownloaded) R.drawable.ic_download_done else R.drawable.ic_download
                )
                binding.downloadedBadge.visibility =
                    if (song.isDownloaded) View.VISIBLE else View.GONE
            }

            binding.root.setOnClickListener { onSongClick(song, queue) }
            binding.btnDownload.setOnClickListener { onDownloadClick(song) }
            binding.btnFavorite.setOnClickListener { onFavoriteClick(song) }
        }
    }

    class SongDiffCallback : DiffUtil.ItemCallback<Song>() {
        override fun areItemsTheSame(oldItem: Song, newItem: Song) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Song, newItem: Song) = oldItem == newItem
    }
}
