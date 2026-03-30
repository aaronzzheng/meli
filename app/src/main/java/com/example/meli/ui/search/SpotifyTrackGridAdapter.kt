package com.example.meli.ui.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.meli.R
import com.example.meli.databinding.ItemSpotifyTrackGridBinding
import com.example.meli.spotify.SpotifyTrack

class SpotifyTrackGridAdapter(
    private val onCoverClicked: (SpotifyTrack) -> Unit,
    private val onAddClicked: (SpotifyTrack) -> Unit
) : ListAdapter<SpotifyTrack, SpotifyTrackGridAdapter.SpotifyTrackViewHolder>(DiffCallback) {

    private var ratedTrackIds: Set<String> = emptySet()

    fun updateRatedTrackIds(trackIds: Set<String>) {
        ratedTrackIds = trackIds
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SpotifyTrackViewHolder {
        val binding = ItemSpotifyTrackGridBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SpotifyTrackViewHolder(binding, onCoverClicked, onAddClicked)
    }

    override fun onBindViewHolder(holder: SpotifyTrackViewHolder, position: Int) {
        holder.bind(getItem(position), ratedTrackIds.contains(getItem(position).id))
    }

    class SpotifyTrackViewHolder(
        private val binding: ItemSpotifyTrackGridBinding,
        private val onCoverClicked: (SpotifyTrack) -> Unit,
        private val onAddClicked: (SpotifyTrack) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SpotifyTrack, isRated: Boolean) {
            binding.spotifyTrackCoverImage.load(item.imageUrl) {
                crossfade(true)
                placeholder(R.drawable.bg_profile_album_placeholder)
                error(R.drawable.bg_profile_album_placeholder)
            }
            binding.spotifyTrackNameText.text = item.name
            binding.spotifyTrackArtistText.text = item.artistNames.joinToString(", ")
            binding.spotifyTrackCoverImage.setOnClickListener { onCoverClicked(item) }
            binding.spotifyTrackAddButton.visibility = if (isRated) android.view.View.GONE else android.view.View.VISIBLE
            binding.spotifyTrackAddButton.setOnClickListener { onAddClicked(item) }
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
