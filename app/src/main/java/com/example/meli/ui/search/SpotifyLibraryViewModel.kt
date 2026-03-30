package com.example.meli.ui.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.meli.data.repository.SpotifyRepository
import com.example.meli.data.repository.TrackRatingRepository
import com.example.meli.spotify.SpotifyConnectionState
import com.example.meli.spotify.SpotifyPlaylistDetails
import com.example.meli.spotify.SpotifyTrack
import kotlinx.coroutines.launch

class SpotifyLibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val spotifyRepository = SpotifyRepository.getInstance(application)
    private val trackRatingRepository = TrackRatingRepository()

    val spotifyState: LiveData<SpotifyConnectionState> = spotifyRepository.connectionState

    private val _playlistDetails = MutableLiveData<SpotifyPlaylistDetails?>()
    val playlistDetails: LiveData<SpotifyPlaylistDetails?> = _playlistDetails

    private val _playlistLoading = MutableLiveData(false)
    val playlistLoading: LiveData<Boolean> = _playlistLoading

    private val _playlistError = MutableLiveData<String?>()
    val playlistError: LiveData<String?> = _playlistError

    private val _trackSearchResults = MutableLiveData<List<SpotifyTrack>>(emptyList())
    val trackSearchResults: LiveData<List<SpotifyTrack>> = _trackSearchResults

    private val _trackSearchLoading = MutableLiveData(false)
    val trackSearchLoading: LiveData<Boolean> = _trackSearchLoading

    private val _trackSearchError = MutableLiveData<String?>()
    val trackSearchError: LiveData<String?> = _trackSearchError

    private val _ratedTrackIds = MutableLiveData<Set<String>>(emptySet())
    val ratedTrackIds: LiveData<Set<String>> = _ratedTrackIds

    fun refreshSpotifyData() {
        viewModelScope.launch {
            spotifyRepository.refreshSpotifyData()
            refreshRatedTrackIds()
        }
    }

    fun loadPlaylistDetails(playlistId: String) {
        if (playlistId.isBlank()) return

        _playlistLoading.value = true
        _playlistError.value = null

        viewModelScope.launch {
            spotifyRepository.fetchPlaylistDetails(playlistId)
                .onSuccess { details ->
                    _playlistDetails.value = details
                }
                .onFailure { throwable ->
                    _playlistDetails.value = null
                    _playlistError.value = throwable.localizedMessage ?: "Failed to load playlist."
                }

            _playlistLoading.value = false
        }
    }

    fun searchTracks(query: String) {
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            _trackSearchResults.value = emptyList()
            _trackSearchError.value = null
            _trackSearchLoading.value = false
            return
        }

        _trackSearchLoading.value = true
        _trackSearchError.value = null
        viewModelScope.launch {
            spotifyRepository.searchTracks(trimmed)
                .onSuccess { tracks ->
                    _trackSearchResults.value = tracks
                }
                .onFailure { error ->
                    _trackSearchResults.value = emptyList()
                    _trackSearchError.value = error.localizedMessage ?: "Failed to search songs."
                }
            _trackSearchLoading.value = false
        }
    }

    fun refreshRatedTrackIds() {
        viewModelScope.launch {
            trackRatingRepository.loadRatedTrackIds()
                .onSuccess { ids ->
                    _ratedTrackIds.value = ids
                }
        }
    }
}
