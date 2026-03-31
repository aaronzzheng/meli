package com.example.meli.ui.notification

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.meli.databinding.ItemNotificationBinding
import com.example.meli.model.AppNotification

class NotificationAdapter(
    private val onAccept: (AppNotification) -> Unit,
    private val onDecline: (AppNotification) -> Unit,
    private val onNotificationClicked: (AppNotification) -> Unit
) : ListAdapter<AppNotification, NotificationAdapter.NotificationViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NotificationViewHolder(binding, onAccept, onDecline, onNotificationClicked)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class NotificationViewHolder(
        private val binding: ItemNotificationBinding,
        private val onAccept: (AppNotification) -> Unit,
        private val onDecline: (AppNotification) -> Unit,
        private val onNotificationClicked: (AppNotification) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AppNotification) {
            binding.notificationMessageText.text = item.message
            binding.notificationTimeText.text = relativeTimeLabel(item.createdAtMillis)

            val showActions = item.type == "FRIEND_REQUEST" && item.status == "PENDING"
            binding.notificationAcceptButton.visibility = if (showActions) View.VISIBLE else View.GONE
            binding.notificationDeclineButton.visibility = if (showActions) View.VISIBLE else View.GONE
            binding.notificationStatusText.visibility = if (showActions) View.GONE else View.VISIBLE
            binding.notificationStatusText.text = when (item.status) {
                "ACCEPTED" -> "Accepted"
                "DECLINED" -> "Declined"
                else -> ""
            }

            binding.root.setOnClickListener { onNotificationClicked(item) }
            binding.notificationAcceptButton.setOnClickListener { onAccept(item) }
            binding.notificationDeclineButton.setOnClickListener { onDecline(item) }
        }

        private fun relativeTimeLabel(createdAtMillis: Long): String {
            val elapsedMinutes = ((System.currentTimeMillis() - createdAtMillis) / 60000L).coerceAtLeast(0L)
            return when {
                elapsedMinutes < 60 -> "${elapsedMinutes}m"
                elapsedMinutes < 1440 -> "${elapsedMinutes / 60}h"
                else -> "${elapsedMinutes / 1440}d"
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<AppNotification>() {
        override fun areItemsTheSame(oldItem: AppNotification, newItem: AppNotification): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: AppNotification, newItem: AppNotification): Boolean {
            return oldItem == newItem
        }
    }
}
