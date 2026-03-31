package com.example.meli.ui.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.meli.databinding.ItemFriendTrackRatingBinding
import com.example.meli.model.FriendTrackRating

class FriendTrackRatingAdapter(
    private val onFriendClicked: (FriendTrackRating) -> Unit
) : ListAdapter<FriendTrackRating, FriendTrackRatingAdapter.FriendTrackRatingViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendTrackRatingViewHolder {
        val binding = ItemFriendTrackRatingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FriendTrackRatingViewHolder(binding, onFriendClicked)
    }

    override fun onBindViewHolder(holder: FriendTrackRatingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class FriendTrackRatingViewHolder(
        private val binding: ItemFriendTrackRatingBinding,
        private val onFriendClicked: (FriendTrackRating) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FriendTrackRating) {
            binding.friendRatingNameText.text = item.userName
            binding.friendRatingScoreText.text = item.formattedScore
            binding.friendRatingNotesText.text = item.notes.ifBlank { "No notes yet." }
            binding.root.setOnClickListener { onFriendClicked(item) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<FriendTrackRating>() {
        override fun areItemsTheSame(oldItem: FriendTrackRating, newItem: FriendTrackRating): Boolean {
            return oldItem.userUid == newItem.userUid
        }

        override fun areContentsTheSame(oldItem: FriendTrackRating, newItem: FriendTrackRating): Boolean {
            return oldItem == newItem
        }
    }
}
