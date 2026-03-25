package com.example.meli.ui.friends

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.meli.databinding.ItemFriendBinding
import com.example.meli.model.FriendListItem

class FriendsAdapter(
    private val onFriendClicked: (FriendListItem) -> Unit,
    private val onUnfriendClicked: (FriendListItem) -> Unit
) : ListAdapter<FriendListItem, FriendsAdapter.FriendViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val binding = ItemFriendBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FriendViewHolder(binding, onFriendClicked, onUnfriendClicked)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class FriendViewHolder(
        private val binding: ItemFriendBinding,
        private val onFriendClicked: (FriendListItem) -> Unit,
        private val onUnfriendClicked: (FriendListItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: FriendListItem) {
            binding.friendNameText.text = item.displayName
            binding.friendUsernameText.text =
                if (item.username.isNotBlank()) "@${item.username}" else item.email
            binding.root.setOnClickListener { onFriendClicked(item) }
            binding.friendUnfriendButton.setOnClickListener { onUnfriendClicked(item) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<FriendListItem>() {
        override fun areItemsTheSame(oldItem: FriendListItem, newItem: FriendListItem): Boolean {
            return oldItem.uid == newItem.uid
        }

        override fun areContentsTheSame(oldItem: FriendListItem, newItem: FriendListItem): Boolean {
            return oldItem == newItem
        }
    }
}
