package com.example.meli.ui.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.meli.data.repository.SpotifyRepository
import com.example.meli.spotify.SpotifyConnectionState
import com.example.meli.spotify.SpotifyPlaylistDetails
import kotlinx.coroutines.launch

class SpotifyLibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val spotifyRepository = SpotifyRepository.getInstance(application)

    val spotifyState: LiveData<SpotifyConnectionState> = spotifyRepository.connectionState

    private val _playlistDetails = MutableLiveData<SpotifyPlaylistDetails?>()
    val playlistDetails: LiveData<SpotifyPlaylistDetails?> = _playlistDetails

    private val _playlistLoading = MutableLiveData(false)
    val playlistLoading: LiveData<Boolean> = _playlistLoading

    private val _playlistError = MutableLiveData<String?>()
    val playlistError: LiveData<String?> = _playlistError

    fun refreshSpotifyData() {
        viewModelScope.launch {
            spotifyRepository.refreshSpotifyData()
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
}
