package com.example.meli.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.meli.model.FeedComment
import com.example.meli.data.repository.ProfileRankingRepository
import com.example.meli.data.repository.SocialRepository
import com.example.meli.model.FriendshipStatus
import com.example.meli.model.ProfileRankingActivity
import com.example.meli.model.UserProfileSummary
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    private val repository = ProfileRankingRepository()
    private val socialRepository = SocialRepository()

    private val _activities = MutableLiveData<List<ProfileRankingActivity>>(emptyList())
    val activities: LiveData<List<ProfileRankingActivity>> = _activities

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _profile = MutableLiveData<UserProfileSummary?>()
    val profile: LiveData<UserProfileSummary?> = _profile

    private val _friendActionMessage = MutableLiveData<String?>()
    val friendActionMessage: LiveData<String?> = _friendActionMessage

    fun loadProfile(viewedUid: String?, currentUid: String?) {
        val targetUid = viewedUid ?: currentUid
        if (targetUid.isNullOrBlank()) {
            _profile.value = null
            return
        }

        viewModelScope.launch {
            socialRepository.getUserProfile(targetUid, currentUid)
                .onSuccess { summary ->
                    _profile.value = summary
                }
                .onFailure { throwable ->
                    _error.value = throwable.localizedMessage ?: "Failed to load profile."
                }
        }
    }

    fun loadRankings(uid: String?, viewerUid: String?) {
        if (uid.isNullOrBlank()) {
            _activities.value = emptyList()
            _error.value = null
            return
        }

        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            repository.loadUserRankings(uid, viewerUid)
                .onSuccess { rankingItems ->
                    _activities.value = rankingItems
                    _error.value = null
                }
                .onFailure { throwable ->
                    _activities.value = emptyList()
                    _error.value = throwable.localizedMessage ?: "Failed to load ranking activity."
                }
            _isLoading.value = false
        }
    }

    fun toggleLike(activity: ProfileRankingActivity, currentUid: String?) {
        if (currentUid.isNullOrBlank()) return
        viewModelScope.launch {
            repository.toggleLike(activity, currentUid)
                .onSuccess { loadRankings(activity.actorUid, currentUid) }
                .onFailure { throwable ->
                    _friendActionMessage.value = throwable.localizedMessage ?: "Failed to update like."
                }
        }
    }

    suspend fun loadComments(activity: ProfileRankingActivity): Result<List<FeedComment>> {
        return repository.loadComments(activity)
    }

    fun addComment(activity: ProfileRankingActivity, currentUid: String?, text: String) {
        if (currentUid.isNullOrBlank() || text.isBlank()) return
        viewModelScope.launch {
            repository.addComment(activity, currentUid, text)
                .onSuccess { loadRankings(activity.actorUid, currentUid) }
                .onFailure { throwable ->
                    _friendActionMessage.value = throwable.localizedMessage ?: "Failed to add comment."
                }
        }
    }

    fun sendFriendRequest(currentUid: String?, targetUid: String?) {
        if (currentUid.isNullOrBlank() || targetUid.isNullOrBlank()) return

        viewModelScope.launch {
            socialRepository.sendFriendRequest(currentUid, targetUid)
                .onSuccess { status ->
                    _profile.value = _profile.value?.copy(friendshipStatus = status)
                    _friendActionMessage.value = when (status) {
                        FriendshipStatus.REQUESTED -> "Friend request sent"
                        FriendshipStatus.FRIENDS -> "Already friends"
                        FriendshipStatus.RECEIVED -> "This user already requested you"
                        FriendshipStatus.SELF -> null
                        FriendshipStatus.NONE -> null
                    }
                }
                .onFailure { throwable ->
                    _friendActionMessage.value =
                        throwable.localizedMessage ?: "Failed to send friend request."
                }
        }
    }

    fun cancelFriendRequest(currentUid: String?, targetUid: String?) {
        if (currentUid.isNullOrBlank() || targetUid.isNullOrBlank()) return

        viewModelScope.launch {
            socialRepository.cancelOutgoingRequest(currentUid, targetUid)
                .onSuccess {
                    _profile.value = _profile.value?.copy(friendshipStatus = FriendshipStatus.NONE)
                    _friendActionMessage.value = "Friend request canceled"
                }
                .onFailure { throwable ->
                    _friendActionMessage.value =
                        throwable.localizedMessage ?: "Failed to cancel friend request."
                }
        }
    }

    fun acceptFriendRequest(currentUid: String?, targetUid: String?) {
        if (currentUid.isNullOrBlank() || targetUid.isNullOrBlank()) return

        viewModelScope.launch {
            socialRepository.acceptFriendRequest(currentUid, targetUid)
                .onSuccess { status ->
                    _profile.value = _profile.value?.copy(friendshipStatus = status)
                    _friendActionMessage.value = "Friend request accepted"
                }
                .onFailure { throwable ->
                    _friendActionMessage.value =
                        throwable.localizedMessage ?: "Failed to accept friend request."
                }
        }
    }

    fun declineFriendRequest(currentUid: String?, targetUid: String?) {
        if (currentUid.isNullOrBlank() || targetUid.isNullOrBlank()) return

        viewModelScope.launch {
            socialRepository.declineFriendRequest(currentUid, targetUid)
                .onSuccess { status ->
                    _profile.value = _profile.value?.copy(friendshipStatus = status)
                    _friendActionMessage.value = "Friend request declined"
                }
                .onFailure { throwable ->
                    _friendActionMessage.value =
                        throwable.localizedMessage ?: "Failed to decline friend request."
                }
        }
    }

    fun clearFriendActionMessage() {
        _friendActionMessage.value = null
    }
}
