package com.example.meli.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.meli.databinding.ItemHomeUserSearchBinding
import com.example.meli.model.FriendshipStatus
import com.example.meli.model.UserSearchResult

class HomeUserSearchAdapter(
    private val onUserClicked: (UserSearchResult) -> Unit,
    private val onAddFriendClicked: (UserSearchResult) -> Unit
) : ListAdapter<UserSearchResult, HomeUserSearchAdapter.HomeUserSearchViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeUserSearchViewHolder {
        val binding = ItemHomeUserSearchBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HomeUserSearchViewHolder(binding, onUserClicked, onAddFriendClicked)
    }

    override fun onBindViewHolder(holder: HomeUserSearchViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class HomeUserSearchViewHolder(
        private val binding: ItemHomeUserSearchBinding,
        private val onUserClicked: (UserSearchResult) -> Unit,
        private val onAddFriendClicked: (UserSearchResult) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: UserSearchResult) {
            binding.userSearchNameText.text = item.displayName
            binding.userSearchUsernameText.text =
                if (item.username.isNotBlank()) "@${item.username}" else item.email
            binding.root.setOnClickListener { onUserClicked(item) }
            binding.userSearchAddFriendButton.text = friendButtonLabel(item.friendshipStatus)
            binding.userSearchAddFriendButton.isEnabled =
                item.friendshipStatus == FriendshipStatus.NONE ||
                    item.friendshipStatus == FriendshipStatus.REQUESTED
            binding.userSearchAddFriendButton.setOnClickListener { onAddFriendClicked(item) }
        }

        private fun friendButtonLabel(status: FriendshipStatus): String {
            return when (status) {
                FriendshipStatus.NONE -> "Add"
                FriendshipStatus.REQUESTED -> "Requested"
                FriendshipStatus.RECEIVED -> "Requested You"
                FriendshipStatus.FRIENDS -> "Friends"
                FriendshipStatus.SELF -> "You"
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<UserSearchResult>() {
        override fun areItemsTheSame(oldItem: UserSearchResult, newItem: UserSearchResult): Boolean {
            return oldItem.uid == newItem.uid
        }

        override fun areContentsTheSame(oldItem: UserSearchResult, newItem: UserSearchResult): Boolean {
            return oldItem == newItem
        }
    }
}
