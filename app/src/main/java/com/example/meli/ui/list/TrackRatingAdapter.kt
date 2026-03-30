package com.example.meli.ui.list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.meli.R
import com.example.meli.databinding.ItemTrackRatingBinding
import com.example.meli.model.RatingSentiment
import com.example.meli.model.TrackRating
import java.text.DateFormat
import java.util.Date

class TrackRatingAdapter(
    private val onTrackSelected: (TrackRating) -> Unit
) : ListAdapter<TrackRating, TrackRatingAdapter.TrackRatingViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackRatingViewHolder {
        val binding = ItemTrackRatingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TrackRatingViewHolder(binding, onTrackSelected)
    }

    override fun onBindViewHolder(holder: TrackRatingViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    class TrackRatingViewHolder(
        private val binding: ItemTrackRatingBinding,
        private val onTrackSelected: (TrackRating) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TrackRating, position: Int) {
            binding.ratingTrackTitle.text = "${position + 1}. ${item.trackTitle}"
            binding.ratingArtistText.text = item.artistText
            binding.ratingAlbumText.text = item.albumTitle?.takeIf { it.isNotBlank() }
                ?: "Single"
            binding.ratingScoreText.text = item.formattedScore
            binding.ratingMetaText.text = item.notes.takeIf { it.isNotBlank() }
                ?: "${item.comparisonCount} comparisons • Updated ${
                    DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(item.updatedAtMillis))
                }"

            val scoreColor = when (item.sentiment) {
                RatingSentiment.LOVE -> binding.root.context.getColor(R.color.meli_green)
                RatingSentiment.FINE -> binding.root.context.getColor(R.color.meli_amber)
                RatingSentiment.DISLIKE -> binding.root.context.getColor(R.color.meli_red)
            }
            binding.ratingScoreText.setTextColor(scoreColor)

            binding.root.setOnClickListener { onTrackSelected(item) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<TrackRating>() {
        override fun areItemsTheSame(oldItem: TrackRating, newItem: TrackRating): Boolean {
            return oldItem.trackId == newItem.trackId
        }

        override fun areContentsTheSame(oldItem: TrackRating, newItem: TrackRating): Boolean {
            return oldItem == newItem
        }
    }
}
