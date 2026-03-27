package com.example.meli.ui.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.meli.data.repository.SocialRepository
import com.example.meli.data.repository.UserSearchRepository
import com.example.meli.model.FriendshipStatus
import com.example.meli.model.UserSearchResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    enum class SearchMode {
        SONGS,
        MEMBERS
    }

    private val userSearchRepository = UserSearchRepository()
    private val socialRepository = SocialRepository()
    private var searchJob: Job? = null

    private val _mode = MutableLiveData(SearchMode.SONGS)
    val mode: LiveData<SearchMode> = _mode

    private val _searchResults = MutableLiveData<List<UserSearchResult>>(emptyList())
    val searchResults: LiveData<List<UserSearchResult>> = _searchResults

    private val _searchLoading = MutableLiveData(false)
    val searchLoading: LiveData<Boolean> = _searchLoading

    private val _searchError = MutableLiveData<String?>()
    val searchError: LiveData<String?> = _searchError

    private val _friendActionMessage = MutableLiveData<String?>()
    val friendActionMessage: LiveData<String?> = _friendActionMessage

    fun setMode(mode: SearchMode) {
        _mode.value = mode
        if (mode == SearchMode.SONGS) {
            _searchResults.value = emptyList()
            _searchError.value = null
            _searchLoading.value = false
            searchJob?.cancel()
        }
    }

    fun searchUsers(query: String, currentUid: String?) {
        searchJob?.cancel()

        if (_mode.value != SearchMode.MEMBERS) {
            _searchResults.value = emptyList()
            _searchError.value = null
            _searchLoading.value = false
            return
        }

        if (currentUid.isNullOrBlank()) {
            _searchResults.value = emptyList()
            _searchError.value = null
            _searchLoading.value = false
            return
        }

        val trimmedQuery = query.trim()
        if (trimmedQuery.length < 2) {
            _searchResults.value = emptyList()
            _searchError.value = null
            _searchLoading.value = false
            return
        }

        searchJob = viewModelScope.launch {
            delay(250)
            _searchLoading.value = true
            _searchError.value = null

            userSearchRepository.searchUsers(trimmedQuery, currentUid)
                .onSuccess { results ->
                    _searchResults.value = results
                    _searchError.value = null
                }
                .onFailure { throwable ->
                    _searchResults.value = emptyList()
                    _searchError.value = throwable.localizedMessage ?: "Failed to search users."
                }

            _searchLoading.value = false
        }
    }

    fun sendFriendRequest(targetUid: String, currentUid: String?) {
        if (currentUid.isNullOrBlank()) return

        viewModelScope.launch {
            socialRepository.sendFriendRequest(currentUid, targetUid)
                .onSuccess { status ->
                    if (status == FriendshipStatus.REQUESTED) {
                        _searchResults.value = _searchResults.value.orEmpty().map { result ->
                            if (result.uid == targetUid) {
                                result.copy(friendshipStatus = FriendshipStatus.REQUESTED)
                            } else {
                                result
                            }
                        }
                        _friendActionMessage.value = "Friend request sent"
                    }
                }
                .onFailure { throwable ->
                    _friendActionMessage.value = throwable.localizedMessage ?: "Failed to send friend request."
                }
        }
    }

    fun cancelFriendRequest(targetUid: String, currentUid: String?) {
        if (currentUid.isNullOrBlank()) return

        viewModelScope.launch {
            socialRepository.cancelOutgoingRequest(currentUid, targetUid)
                .onSuccess {
                    _searchResults.value = _searchResults.value.orEmpty().map { result ->
                        if (result.uid == targetUid) {
                            result.copy(friendshipStatus = FriendshipStatus.NONE)
                        } else {
                            result
                        }
                    }
                    _friendActionMessage.value = "Friend request canceled"
                }
                .onFailure { throwable ->
                    _friendActionMessage.value = throwable.localizedMessage ?: "Failed to cancel friend request."
                }
        }
    }

    fun clearFriendActionMessage() {
        _friendActionMessage.value = null
    }
}
