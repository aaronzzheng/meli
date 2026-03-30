package com.example.meli.ui.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.meli.data.repository.TrackRatingRepository
import com.example.meli.model.ComparisonChoice
import com.example.meli.model.RatingSentiment
import com.example.meli.model.TrackRating
import kotlinx.coroutines.launch

data class TrackDetailUiState(
    val trackId: String = "",
    val trackTitle: String = "",
    val artistText: String = "",
    val albumTitle: String = "",
    val imageUrl: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val currentRating: TrackRating? = null,
    val selectedSentiment: RatingSentiment? = null,
    val comparisonTrack: TrackRating? = null,
    val selectedComparisonChoice: ComparisonChoice? = null,
    val notes: String = "",
    val helperText: String? = null,
    val comparisonsCompleted: Int = 0,
    val comparisonsTarget: Int = TrackRatingRepository.TARGET_COMPARISONS,
    val message: String? = null
) {
    val showComposer: Boolean
        get() = selectedSentiment != null
}

class TrackRatingViewModel : ViewModel() {

    private val repository = TrackRatingRepository()

    private val _state = MutableLiveData(TrackDetailUiState())
    val state: LiveData<TrackDetailUiState> = _state

    private var persistedCurrentRating: TrackRating? = null
    private var workingRating: TrackRating? = null
    private val updatedOpponents = linkedMapOf<String, TrackRating>()
    private val usedComparisonIds = linkedSetOf<String>()

    fun initialize(
        trackId: String,
        trackTitle: String,
        artistText: String,
        albumTitle: String,
        imageUrl: String
    ) {
        if (_state.value?.trackId == trackId && trackId.isNotBlank()) {
            return
        }

        _state.value = TrackDetailUiState(
            trackId = trackId,
            trackTitle = trackTitle,
            artistText = artistText,
            albumTitle = albumTitle,
            imageUrl = imageUrl,
            isLoading = true
        )

        viewModelScope.launch {
            repository.getRating(trackId)
                .onSuccess { rating ->
                    persistedCurrentRating = rating
                    _state.value = _state.value?.copy(
                        isLoading = false,
                        currentRating = rating,
                        selectedSentiment = rating?.sentiment,
                        notes = rating?.notes.orEmpty(),
                        helperText = rating?.let {
                            "Current score ${it.formattedScore}. ${it.confidenceLabel} after ${it.comparisonCount} comparisons."
                        }
                    )
                    if (rating != null) {
                        selectSentiment(rating.sentiment)
                    }
                }
                .onFailure { error ->
                    _state.value = _state.value?.copy(
                        isLoading = false,
                        message = error.localizedMessage ?: "Failed to load this song."
                    )
                }
        }
    }

    fun selectSentiment(sentiment: RatingSentiment) {
        val currentState = _state.value ?: return
        resetSession()

        val artistNames = currentState.artistText.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        workingRating = repository.buildWorkingRating(
            trackId = currentState.trackId,
            trackTitle = currentState.trackTitle,
            artistNames = artistNames,
            albumTitle = currentState.albumTitle.ifBlank { null },
            imageUrl = currentState.imageUrl.ifBlank { null },
            sentiment = sentiment,
            notes = currentState.notes,
            existing = persistedCurrentRating
        )

        _state.value = currentState.copy(
            selectedSentiment = sentiment,
            selectedComparisonChoice = null,
            comparisonsCompleted = 0,
            helperText = "Round 1 of ${TrackRatingRepository.TARGET_COMPARISONS}"
        )
        refreshComparisonCandidate()
    }

    fun updateNotes(notes: String) {
        _state.value = _state.value?.copy(notes = notes)
        workingRating = workingRating?.copy(notes = notes)
    }

    fun submitComparison(choice: ComparisonChoice) {
        val currentState = _state.value ?: return
        val currentWorking = workingRating ?: run {
            _state.value = currentState.copy(message = "Choose how the song felt first.")
            return
        }

        _state.value = currentState.copy(isSaving = true, message = null)

        val result = repository.applyComparison(
            current = currentWorking.copy(notes = currentState.notes),
            opponent = currentState.comparisonTrack,
            choice = choice
        )

        workingRating = result.updatedCurrent
        result.updatedOpponent?.let { updatedOpponents[it.trackId] = it }
        currentState.comparisonTrack?.trackId?.let { usedComparisonIds += it }

        val completed = currentState.comparisonsCompleted + if (currentState.comparisonTrack != null) 1 else 0
        _state.value = currentState.copy(
            isSaving = false,
            selectedComparisonChoice = choice,
            comparisonsCompleted = completed
        )

        if (completed >= TrackRatingRepository.TARGET_COMPARISONS || currentState.comparisonTrack == null) {
            persistSession()
        } else {
            refreshComparisonCandidate()
        }
    }

    fun deleteRating() {
        val trackId = _state.value?.trackId.orEmpty()
        if (trackId.isBlank()) return

        _state.value = _state.value?.copy(isSaving = true, message = null)

        viewModelScope.launch {
            repository.deleteRating(trackId)
                .onSuccess {
                    persistedCurrentRating = null
                    resetSession()
                    _state.value = _state.value?.copy(
                        isSaving = false,
                        currentRating = null,
                        selectedSentiment = null,
                        comparisonTrack = null,
                        selectedComparisonChoice = null,
                        notes = "",
                        helperText = "Ranking removed.",
                        message = "Ranking deleted."
                    )
                }
                .onFailure { error ->
                    _state.value = _state.value?.copy(
                        isSaving = false,
                        message = error.localizedMessage ?: "Failed to delete ranking."
                    )
                }
        }
    }

    fun consumeMessage() {
        _state.value = _state.value?.copy(message = null)
    }

    private fun refreshComparisonCandidate() {
        val currentState = _state.value ?: return
        val sentiment = currentState.selectedSentiment ?: return
        val currentWorking = workingRating ?: return

        _state.value = currentState.copy(isLoading = true)

        viewModelScope.launch {
            repository.getNextComparisonCandidate(
                excludedTrackIds = usedComparisonIds + currentState.trackId,
                sentiment = sentiment,
                targetScore = currentWorking.score
            ).onSuccess { comparison ->
                val completed = _state.value?.comparisonsCompleted ?: 0
                val helperText = if (comparison == null) {
                    if (completed == 0) {
                        "No ${sentiment.label.lowercase()} songs yet. Too Tough will place this song at ${currentWorking.formattedScore}."
                    } else {
                        "No more same-bucket songs left. This song is ready to place."
                    }
                } else {
                    "Round ${completed + 1} of ${TrackRatingRepository.TARGET_COMPARISONS}"
                }

                _state.value = _state.value?.copy(
                    isLoading = false,
                    comparisonTrack = comparison,
                    selectedComparisonChoice = null,
                    helperText = helperText
                )
            }.onFailure { error ->
                _state.value = _state.value?.copy(
                    isLoading = false,
                    comparisonTrack = null,
                    message = error.localizedMessage ?: "Failed to load comparison song."
                )
            }
        }
    }

    private fun persistSession() {
        val current = workingRating ?: return
        _state.value = _state.value?.copy(isSaving = true)

        viewModelScope.launch {
            repository.saveSession(
                current = current.copy(notes = _state.value?.notes.orEmpty()),
                updatedOpponents = updatedOpponents.values
            ).onSuccess { saved ->
                persistedCurrentRating = saved
                workingRating = saved
                _state.value = _state.value?.copy(
                    isSaving = false,
                    currentRating = saved,
                    comparisonTrack = null,
                    helperText = "Placed at ${saved.formattedScore} after ${(_state.value?.comparisonsCompleted ?: 0)} comparisons.",
                    message = "Ranking saved."
                )
            }.onFailure { error ->
                _state.value = _state.value?.copy(
                    isSaving = false,
                    message = error.localizedMessage ?: "Failed to save ranking."
                )
            }
        }
    }

    private fun resetSession() {
        workingRating = null
        updatedOpponents.clear()
        usedComparisonIds.clear()
    }
}
