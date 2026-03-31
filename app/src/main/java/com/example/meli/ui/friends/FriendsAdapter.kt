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

    private var allowUnfriend: Boolean = true

    fun setAllowUnfriend(allow: Boolean) {
        allowUnfriend = allow
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val binding = ItemFriendBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FriendViewHolder(binding, onFriendClicked, onUnfriendClicked)
    }

    class FriendViewHolder(
        private val binding: ItemFriendBinding,
        private val onFriendClicked: (FriendListItem) -> Unit,
        private val onUnfriendClicked: (FriendListItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: FriendListItem, allowUnfriend: Boolean) {
            binding.friendNameText.text = item.displayName
            binding.friendUsernameText.text =
                if (item.username.isNotBlank()) "@${item.username}" else item.email
            binding.root.setOnClickListener { onFriendClicked(item) }
            binding.friendUnfriendButton.visibility =
                if (allowUnfriend) android.view.View.VISIBLE else android.view.View.GONE
            binding.friendUnfriendButton.setOnClickListener { onUnfriendClicked(item) }
        }
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        holder.bind(getItem(position), allowUnfriend)
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
