package com.example.meli.ui.profile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.meli.databinding.ItemProfileRankingBinding
import com.example.meli.model.ProfileRankingActivity
import java.util.Locale

class ProfileRankingAdapter :
    ListAdapter<ProfileRankingActivity, ProfileRankingAdapter.ProfileRankingViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfileRankingViewHolder {
        val binding = ItemProfileRankingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProfileRankingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProfileRankingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ProfileRankingViewHolder(
        private val binding: ItemProfileRankingBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ProfileRankingActivity) {
            binding.rankingTrackTitle.text = item.trackTitle
            binding.rankingArtistsText.text = item.artistText.ifBlank { "Unknown artist" }
            binding.rankingListNameText.text = item.listName?.takeIf { it.isNotBlank() } ?: "Ranking"
            binding.rankingScoreText.text = item.rankingScore?.let {
                String.format(Locale.US, "%.1f", it)
            } ?: "--"
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<ProfileRankingActivity>() {
        override fun areItemsTheSame(
            oldItem: ProfileRankingActivity,
            newItem: ProfileRankingActivity
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: ProfileRankingActivity,
            newItem: ProfileRankingActivity
        ): Boolean = oldItem == newItem
    }
}
