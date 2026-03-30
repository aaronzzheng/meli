package com.example.meli.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.meli.data.repository.TrackRatingRepository
import com.example.meli.model.RankingSortOption
import com.example.meli.model.TrackRating
import kotlinx.coroutines.launch

class ListViewModel : ViewModel() {

    private val repository = TrackRatingRepository()

    private val _ratings = MutableLiveData<List<TrackRating>>(emptyList())
    val ratings: LiveData<List<TrackRating>> = _ratings

    private val _sortOption = MutableLiveData(RankingSortOption.HIGHEST_RATED)
    val sortOption: LiveData<RankingSortOption> = _sortOption

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    init {
        refresh()
    }

    fun refresh() {
        val sort = _sortOption.value ?: RankingSortOption.HIGHEST_RATED
        _isLoading.value = true

        viewModelScope.launch {
            repository.loadRatings(sort)
                .onSuccess { loadedRatings ->
                    _ratings.value = loadedRatings
                }
                .onFailure { error ->
                    _ratings.value = emptyList()
                    _message.value = error.localizedMessage ?: "Failed to load your rankings."
                }
            _isLoading.value = false
        }
    }

    fun setSortOption(sortOption: RankingSortOption) {
        if (_sortOption.value == sortOption) return
        _sortOption.value = sortOption
        refresh()
    }

    fun deleteRating(trackId: String) {
        _isLoading.value = true
        viewModelScope.launch {
            repository.deleteRating(trackId)
                .onSuccess {
                    _message.value = "Ranking deleted."
                    refresh()
                }
                .onFailure { error ->
                    _isLoading.value = false
                    _message.value = error.localizedMessage ?: "Failed to delete ranking."
                }
        }
    }

    fun consumeMessage() {
        _message.value = null
    }
}
