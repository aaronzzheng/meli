package com.example.meli.ui.feed

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.meli.databinding.ItemFeedCommentBinding
import com.example.meli.model.FeedComment

class FeedCommentsAdapter(
    private val onReplyClicked: (FeedComment) -> Unit,
    private val onLikeClicked: (FeedComment) -> Unit
) : ListAdapter<FeedComment, FeedCommentsAdapter.CommentViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val binding = ItemFeedCommentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CommentViewHolder(binding, onReplyClicked, onLikeClicked)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class CommentViewHolder(
        private val binding: ItemFeedCommentBinding,
        private val onReplyClicked: (FeedComment) -> Unit,
        private val onLikeClicked: (FeedComment) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FeedComment) {
            binding.commentActorNameText.text = item.actorName
            binding.commentBodyText.text = item.text
            binding.commentReplyToText.text = item.replyToName?.let { "Replying to $it" }.orEmpty()
            binding.commentReplyToText.visibility = if (item.replyToName.isNullOrBlank()) View.GONE else View.VISIBLE
            binding.commentLikeButton.text = item.likeCount.toString()
            binding.commentLikeButton.alpha = if (item.likedByCurrentUser) 1f else 0.7f

            val imageBytes = item.actorImageBase64?.let {
                runCatching { Base64.decode(it, Base64.DEFAULT) }.getOrNull()
            }
            val bitmap = imageBytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
            if (bitmap != null) {
                binding.commentAvatarImage.setImageBitmap(bitmap)
            } else {
                binding.commentAvatarImage.setImageResource(com.example.meli.R.drawable.ic_profile_black_24dp)
            }

            binding.commentReplyButton.setOnClickListener { onReplyClicked(item) }
            binding.commentLikeButton.setOnClickListener { onLikeClicked(item) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<FeedComment>() {
        override fun areItemsTheSame(oldItem: FeedComment, newItem: FeedComment): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: FeedComment, newItem: FeedComment): Boolean = oldItem == newItem
    }
}
