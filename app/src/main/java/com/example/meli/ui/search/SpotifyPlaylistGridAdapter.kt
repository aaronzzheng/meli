package com.example.meli.ui.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.meli.R
import com.example.meli.databinding.ItemSpotifyPlaylistGridBinding
import com.example.meli.spotify.SpotifyPlaylist

class SpotifyPlaylistGridAdapter(
    private val onCoverClicked: (SpotifyPlaylist) -> Unit
) : ListAdapter<SpotifyPlaylist, SpotifyPlaylistGridAdapter.SpotifyPlaylistViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SpotifyPlaylistViewHolder {
        val binding = ItemSpotifyPlaylistGridBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SpotifyPlaylistViewHolder(binding, onCoverClicked)
    }

    override fun onBindViewHolder(holder: SpotifyPlaylistViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class SpotifyPlaylistViewHolder(
        private val binding: ItemSpotifyPlaylistGridBinding,
        private val onCoverClicked: (SpotifyPlaylist) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SpotifyPlaylist) {
            binding.spotifyPlaylistCoverImage.load(item.imageUrl) {
                crossfade(true)
                placeholder(R.drawable.bg_profile_album_placeholder)
                error(R.drawable.bg_profile_album_placeholder)
            }
            binding.spotifyPlaylistNameText.text = item.name
            binding.spotifyPlaylistMetaText.text = item.ownerName ?: "${item.totalTracks} tracks"
            binding.spotifyPlaylistCoverImage.setOnClickListener { onCoverClicked(item) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<SpotifyPlaylist>() {
        override fun areItemsTheSame(oldItem: SpotifyPlaylist, newItem: SpotifyPlaylist): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: SpotifyPlaylist, newItem: SpotifyPlaylist): Boolean {
            return oldItem == newItem
        }
    }
}
