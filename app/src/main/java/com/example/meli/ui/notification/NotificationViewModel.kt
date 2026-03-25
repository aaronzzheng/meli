package com.example.meli.ui.notification

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.meli.data.repository.NotificationRepository
import com.example.meli.model.AppNotification
import kotlinx.coroutines.launch

class NotificationViewModel : ViewModel() {
    private val repository = NotificationRepository()

    private val _notifications = MutableLiveData<List<AppNotification>>(emptyList())
    val notifications: LiveData<List<AppNotification>> = _notifications

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _actionMessage = MutableLiveData<String?>()
    val actionMessage: LiveData<String?> = _actionMessage

    fun loadNotifications(uid: String?) {
        if (uid.isNullOrBlank()) {
            _notifications.value = emptyList()
            return
        }

        viewModelScope.launch {
            repository.loadNotifications(uid)
                .onSuccess { items ->
                    _notifications.value = items
                    _error.value = null
                }
                .onFailure { throwable ->
                    _notifications.value = emptyList()
                    _error.value = throwable.localizedMessage ?: "Failed to load notifications."
                }
        }
    }

    fun respondToFriendRequest(uid: String?, notification: AppNotification, accept: Boolean) {
        if (uid.isNullOrBlank()) return

        viewModelScope.launch {
            repository.respondToFriendRequest(uid, notification, accept)
                .onSuccess {
                    _notifications.value = _notifications.value.orEmpty().map { item ->
                        if (item.id == notification.id) {
                            item.copy(status = if (accept) "ACCEPTED" else "DECLINED")
                        } else {
                            item
                        }
                    }
                    _actionMessage.value = if (accept) {
                        "Friend request accepted"
                    } else {
                        "Friend request declined"
                    }
                }
                .onFailure { throwable ->
                    _actionMessage.value =
                        throwable.localizedMessage ?: "Failed to update friend request."
                }
        }
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }
}
