package com.example.meli.ui.profile

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.meli.R
import com.example.meli.databinding.ItemProfileRankingBinding
import com.example.meli.model.ProfileRankingActivity
import com.google.firebase.auth.FirebaseAuth
import java.util.Locale

class ProfileRankingAdapter(
    private val onLikeClicked: (ProfileRankingActivity) -> Unit = {},
    private val onCommentClicked: (ProfileRankingActivity) -> Unit = {}
) :
    ListAdapter<ProfileRankingActivity, ProfileRankingAdapter.ProfileRankingViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfileRankingViewHolder {
        val binding = ItemProfileRankingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProfileRankingViewHolder(binding, onLikeClicked, onCommentClicked)
    }

    override fun onBindViewHolder(holder: ProfileRankingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ProfileRankingViewHolder(
        private val binding: ItemProfileRankingBinding,
        private val onLikeClicked: (ProfileRankingActivity) -> Unit,
        private val onCommentClicked: (ProfileRankingActivity) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ProfileRankingActivity) {
            val currentUid = FirebaseAuth.getInstance().currentUser?.uid
            val actorLabel = if (item.actorUid == currentUid) {
                "You ranked"
            } else {
                "${item.actorName} ranked"
            }

            binding.rankingArtistsText.text = item.artistText.ifBlank { "Unknown artist" }
            binding.rankingListNameText.text = item.listName?.takeIf { it.isNotBlank() } ?: "Ranking"
            binding.rankingActorText.text = "$actorLabel ${item.trackTitle}"
            binding.rankingScoreText.text = item.rankingScore?.let {
                String.format(Locale.US, "%.1f", it)
            } ?: "--"
            binding.rankingNotesText.text = item.notes
            binding.rankingNotesText.visibility = if (item.notes.isBlank()) View.GONE else View.VISIBLE
            binding.rankingNotesLabel.visibility = if (item.notes.isBlank()) View.GONE else View.VISIBLE
            binding.rankingLikeButton.text = item.likeCount.toString()
            binding.rankingCommentButton.text = item.commentCount.toString()
            binding.rankingLikeButton.alpha = if (item.likedByCurrentUser) 1f else 0.7f

            binding.rankingCoverImage.load(item.imageUrl) {
                placeholder(R.color.meli_surface_subtle)
                error(R.color.meli_surface_subtle)
            }
            bindActorAvatar(item)

            val score = item.rankingScore
            val scoreColorRes = when {
                score == null -> R.color.meli_ink
                score >= 7.0 -> R.color.meli_green
                score >= 4.0 -> R.color.meli_amber
                else -> R.color.meli_red
            }
            binding.rankingScoreText.setTextColor(binding.root.context.getColor(scoreColorRes))
            binding.rankingScoreText.setBackgroundResource(R.drawable.bg_list_score_circle)
            binding.rankingLikeButton.setOnClickListener { onLikeClicked(item) }
            binding.rankingCommentButton.setOnClickListener { onCommentClicked(item) }
        }

        private fun bindActorAvatar(item: ProfileRankingActivity) {
            val encoded = item.actorImageBase64
            if (encoded.isNullOrBlank()) {
                binding.rankingActorAvatarImage.setImageResource(R.drawable.ic_profile_black_24dp)
                return
            }

            val bytes = runCatching { Base64.decode(encoded, Base64.DEFAULT) }.getOrNull()
            val bitmap = bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
            if (bitmap != null) {
                binding.rankingActorAvatarImage.setImageBitmap(bitmap)
            } else {
                binding.rankingActorAvatarImage.setImageResource(R.drawable.ic_profile_black_24dp)
            }
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
