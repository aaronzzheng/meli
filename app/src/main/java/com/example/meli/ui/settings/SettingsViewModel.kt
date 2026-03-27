package com.example.meli.ui.settings

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.meli.data.repository.SpotifyRepository
import com.example.meli.spotify.SpotifyConnectionState
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val spotifyRepository = SpotifyRepository.getInstance(application)

    val spotifyState: LiveData<SpotifyConnectionState> = spotifyRepository.connectionState

    fun connectSpotify(): Intent = spotifyRepository.connectSpotify()

    fun reconnectSpotify(): Intent = spotifyRepository.reconnectSpotify()

    fun disconnectSpotify() {
        spotifyRepository.disconnectSpotify()
    }

    fun refreshSpotifyData() {
        viewModelScope.launch {
            spotifyRepository.refreshSpotifyData()
        }
    }
}
