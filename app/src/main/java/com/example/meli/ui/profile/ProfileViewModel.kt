package com.example.meli.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.meli.data.repository.ProfileRankingRepository
import com.example.meli.model.ProfileRankingActivity
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    private val repository = ProfileRankingRepository()

    private val _activities = MutableLiveData<List<ProfileRankingActivity>>(emptyList())
    val activities: LiveData<List<ProfileRankingActivity>> = _activities

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadRankings(uid: String?) {
        if (uid.isNullOrBlank()) {
            _activities.value = emptyList()
            _error.value = null
            return
        }

        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            repository.loadUserRankings(uid)
                .onSuccess { rankingItems ->
                    if (rankingItems.isNotEmpty()) {
                        _activities.value = rankingItems
                        _error.value = null
                    } else {
                        repository.seedMockRankingsIfEmpty(uid)
                            .onSuccess { seedResult ->
                                if (seedResult.created) {
                                    repository.loadUserRankings(uid)
                                        .onSuccess { seededItems ->
                                            _activities.value = seededItems
                                            _error.value = null
                                        }
                                        .onFailure { reloadError ->
                                            _activities.value = emptyList()
                                            _error.value = reloadError.localizedMessage
                                                ?: "Failed to load seeded rankings."
                                        }
                                } else {
                                    _activities.value = emptyList()
                                    _error.value = null
                                }
                            }
                            .onFailure { seedError ->
                                _activities.value = emptyList()
                                _error.value = seedError.localizedMessage
                                    ?: "Failed to seed mock rankings."
                            }
                    }
                }
                .onFailure { throwable ->
                    _activities.value = emptyList()
                    _error.value = throwable.localizedMessage ?: "Failed to load ranking activity."
                }
            _isLoading.value = false
        }
    }
}
