package com.example.meli.ui.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.meli.R
import com.example.meli.databinding.ItemSpotifyTrackRowBinding
import com.example.meli.spotify.SpotifyTrack

class SpotifyTrackListAdapter(
    private val onTrackClicked: (SpotifyTrack) -> Unit,
    private val onAddClicked: (SpotifyTrack) -> Unit
) : ListAdapter<SpotifyTrack, SpotifyTrackListAdapter.SpotifyTrackRowViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SpotifyTrackRowViewHolder {
        val binding = ItemSpotifyTrackRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SpotifyTrackRowViewHolder(binding, onTrackClicked, onAddClicked)
    }

    override fun onBindViewHolder(holder: SpotifyTrackRowViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class SpotifyTrackRowViewHolder(
        private val binding: ItemSpotifyTrackRowBinding,
        private val onTrackClicked: (SpotifyTrack) -> Unit,
        private val onAddClicked: (SpotifyTrack) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SpotifyTrack) {
            binding.spotifyTrackRowCoverImage.load(item.imageUrl) {
                crossfade(true)
                placeholder(R.drawable.bg_profile_album_placeholder)
                error(R.drawable.bg_profile_album_placeholder)
            }
            binding.spotifyTrackRowTitleText.text = item.name
            binding.spotifyTrackRowSubtitleText.text = item.artistNames.joinToString(", ")
            binding.spotifyTrackRowCoverImage.setOnClickListener { onTrackClicked(item) }
            binding.root.setOnClickListener { onTrackClicked(item) }
            binding.spotifyTrackRowAddButton.setOnClickListener { onAddClicked(item) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<SpotifyTrack>() {
        override fun areItemsTheSame(oldItem: SpotifyTrack, newItem: SpotifyTrack): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: SpotifyTrack, newItem: SpotifyTrack): Boolean {
            return oldItem == newItem
        }
    }
}
