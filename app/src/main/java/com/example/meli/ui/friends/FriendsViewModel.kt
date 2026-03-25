package com.example.meli.ui.friends

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.meli.data.repository.SocialRepository
import com.example.meli.model.FriendListItem
import kotlinx.coroutines.launch

class FriendsViewModel : ViewModel() {
    private val socialRepository = SocialRepository()

    private val _friends = MutableLiveData<List<FriendListItem>>(emptyList())
    val friends: LiveData<List<FriendListItem>> = _friends

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _actionMessage = MutableLiveData<String?>()
    val actionMessage: LiveData<String?> = _actionMessage

    fun loadFriends(uid: String?) {
        if (uid.isNullOrBlank()) {
            _friends.value = emptyList()
            return
        }

        viewModelScope.launch {
            socialRepository.loadFriends(uid)
                .onSuccess {
                    _friends.value = it
                    _error.value = null
                }
                .onFailure { throwable ->
                    _friends.value = emptyList()
                    _error.value = throwable.localizedMessage ?: "Failed to load friends."
                }
        }
    }

    fun unfriend(currentUid: String?, targetUid: String) {
        if (currentUid.isNullOrBlank()) return

        viewModelScope.launch {
            socialRepository.unfriend(currentUid, targetUid)
                .onSuccess {
                    _friends.value = _friends.value.orEmpty().filterNot { it.uid == targetUid }
                    _actionMessage.value = "Friend removed"
                }
                .onFailure { throwable ->
                    _actionMessage.value = throwable.localizedMessage ?: "Failed to remove friend."
                }
        }
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }
}
