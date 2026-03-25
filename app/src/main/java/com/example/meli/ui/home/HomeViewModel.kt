package com.example.meli.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.meli.data.repository.ProfileRankingRepository
import com.example.meli.data.repository.SocialRepository
import com.example.meli.data.repository.UserSearchRepository
import com.example.meli.model.FriendshipStatus
import com.example.meli.model.ProfileRankingActivity
import com.example.meli.model.UserSearchResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val repository = ProfileRankingRepository()
    private val userSearchRepository = UserSearchRepository()
    private val socialRepository = SocialRepository()
    private var searchJob: Job? = null

    private val _activities = MutableLiveData<List<ProfileRankingActivity>>(emptyList())
    val activities: LiveData<List<ProfileRankingActivity>> = _activities

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _searchResults = MutableLiveData<List<UserSearchResult>>(emptyList())
    val searchResults: LiveData<List<UserSearchResult>> = _searchResults

    private val _searchLoading = MutableLiveData(false)
    val searchLoading: LiveData<Boolean> = _searchLoading

    private val _searchError = MutableLiveData<String?>()
    val searchError: LiveData<String?> = _searchError

    private val _friendActionMessage = MutableLiveData<String?>()
    val friendActionMessage: LiveData<String?> = _friendActionMessage

    fun loadFeed(uid: String?) {
        if (uid.isNullOrBlank()) {
            _activities.value = emptyList()
            _error.value = null
            return
        }

        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            repository.loadFeedRankings(uid)
                .onSuccess { feedItems ->
                    if (feedItems.isNotEmpty()) {
                        _activities.value = feedItems
                        _error.value = null
                    } else {
                        repository.seedMockRankingsIfEmpty(uid)
                            .onSuccess {
                                repository.loadFeedRankings(uid)
                                    .onSuccess { seededItems ->
                                        _activities.value = seededItems
                                        _error.value = null
                                    }
                                    .onFailure { reloadError ->
                                        _activities.value = emptyList()
                                        _error.value = reloadError.localizedMessage
                                            ?: "Failed to load feed."
                                    }
                            }
                            .onFailure { seedError ->
                                _activities.value = emptyList()
                                _error.value = seedError.localizedMessage
                                    ?: "Failed to seed feed activity."
                            }
                    }
                }
                .onFailure { throwable ->
                    _activities.value = emptyList()
                    _error.value = throwable.localizedMessage ?: "Failed to load feed activity."
                }
            _isLoading.value = false
        }
    }

    fun searchUsers(query: String, currentUid: String?) {
        searchJob?.cancel()

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
                    _friendActionMessage.value = throwable.localizedMessage
                        ?: "Failed to cancel friend request."
                }
        }
    }

    fun clearFriendActionMessage() {
        _friendActionMessage.value = null
    }
}
