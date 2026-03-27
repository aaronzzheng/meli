package com.example.meli.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.meli.spotify.SpotifyApiClient
import com.example.meli.spotify.SpotifyApiException
import com.example.meli.spotify.SpotifyAuthStore
import com.example.meli.spotify.SpotifyConfig
import com.example.meli.spotify.SpotifyConnectionState
import com.example.meli.spotify.SpotifyPlaylistDetails
import com.example.meli.spotify.SpotifyPkceUtils
import com.example.meli.spotify.SpotifyTokens
import com.example.meli.spotify.SpotifyUserData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "SpotifyRepository"
private const val TOKEN_EXPIRY_BUFFER_MS = 60_000L

class SpotifyRepository private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val authStore = SpotifyAuthStore(appContext)
    private val apiClient = SpotifyApiClient()

    private val _connectionState = MutableLiveData(initialState())
    val connectionState: LiveData<SpotifyConnectionState> = _connectionState

    fun connectSpotify(): Intent {
        val codeVerifier = SpotifyPkceUtils.generateCodeVerifier()
        val state = SpotifyPkceUtils.generateState()
        val authorizeUri = SpotifyConfig.buildAuthorizationUri(
            codeChallenge = SpotifyPkceUtils.generateCodeChallenge(codeVerifier),
            state = state
        )
        authStore.savePendingAuthorization(codeVerifier, state)
        Log.d(TAG, "Starting Spotify authorization flow")
        updateState(
            _connectionState.value?.copy(
                isBusy = true,
                errorMessage = null,
                statusMessage = "Connecting to Spotify…"
            ) ?: SpotifyConnectionState(isBusy = true, statusMessage = "Connecting to Spotify…")
        )
        return Intent(Intent.ACTION_VIEW, authorizeUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun reconnectSpotify(): Intent {
        authStore.clearTokens()
        authStore.clearPendingAuthorization()
        updateState(
            SpotifyConnectionState(
                isConnected = false,
                isBusy = false,
                statusMessage = "Sign in with Spotify to import your library and listening data."
            )
        )
        return connectSpotify()
    }

    fun disconnectSpotify() {
        authStore.clearTokens()
        authStore.clearPendingAuthorization()
        updateState(SpotifyConnectionState())
        Log.d(TAG, "Spotify disconnected")
    }

    suspend fun handleRedirect(uri: Uri): Result<Unit> {
        Log.d(TAG, "Handling Spotify redirect: $uri")
        updateState(
            (_connectionState.value ?: initialState()).copy(
                isBusy = true,
                errorMessage = null,
                statusMessage = "Connecting to Spotify…"
            )
        )
        return runCatching {
            val error = uri.getQueryParameter("error")
            if (!error.isNullOrBlank()) {
                throw IllegalStateException("Spotify authorization failed: $error")
            }

            val returnedState = uri.getQueryParameter("state")
            val expectedState = authStore.getPendingState()
            if (returnedState.isNullOrBlank() || expectedState.isNullOrBlank() || returnedState != expectedState) {
                throw IllegalStateException("Spotify authorization state validation failed")
            }

            val code = uri.getQueryParameter("code")
                ?: throw IllegalStateException("Missing Spotify authorization code")
            val codeVerifier = authStore.getPendingCodeVerifier()
                ?: throw IllegalStateException("Missing PKCE code verifier")

            val tokens = withContext(Dispatchers.IO) {
                apiClient.exchangeCodeForTokens(code, codeVerifier)
            }
            authStore.saveTokens(tokens)
            authStore.clearPendingAuthorization()
            withContext(Dispatchers.IO) {
                syncSpotifyData()
            }
        }.onFailure { throwable ->
            Log.e(TAG, "Failed to handle Spotify redirect", throwable)
            authStore.clearPendingAuthorization()
            updateState(
                (_connectionState.value ?: initialState()).copy(
                    isBusy = false,
                    isConnected = authStore.getTokens() != null,
                    errorMessage = throwable.localizedMessage ?: "Spotify connection failed",
                    statusMessage = if (authStore.getTokens() != null) {
                        "Spotify account connected. Tap to refresh your data."
                    } else {
                        "Sign in with Spotify to import your library and listening data."
                    }
                )
            )
        }.map { Unit }
    }

    suspend fun refreshSpotifyData(): Result<SpotifyUserData> {
        val currentState = _connectionState.value ?: initialState()
        val tokens = authStore.getTokens()
        if (tokens == null) {
            updateState(
                currentState.copy(
                    isConnected = false,
                    isBusy = false,
                    errorMessage = null,
                    userData = null,
                    statusMessage = "Sign in with Spotify to import your library and listening data."
                )
            )
            return Result.failure(IllegalStateException("Spotify is not connected"))
        }

        updateState(
            currentState.copy(
                isBusy = true,
                errorMessage = null,
                statusMessage = "Refreshing your Spotify data…"
            )
        )

        return runCatching {
            withContext(Dispatchers.IO) {
                syncSpotifyData(tokens)
            }
        }.onFailure { throwable ->
            Log.e(TAG, "Failed to refresh Spotify data", throwable)
            updateState(
                (_connectionState.value ?: initialState()).copy(
                    isBusy = false,
                    errorMessage = throwable.localizedMessage ?: "Failed to refresh Spotify data",
                    statusMessage = if (authStore.getTokens() != null) {
                        "Spotify account connected. Tap to refresh your data."
                    } else {
                        "Sign in with Spotify to import your library and listening data."
                    }
                )
            )
        }
    }

    fun currentUserData(): SpotifyUserData? = _connectionState.value?.userData

    suspend fun fetchPlaylistDetails(playlistId: String): Result<SpotifyPlaylistDetails> {
        return runCatching {
            withContext(Dispatchers.IO) {
                val data = currentUserData()
                    ?: refreshSpotifyData().getOrThrow()
                val playlist = data.playlists.firstOrNull { it.id == playlistId }
                    ?: throw IllegalStateException("Playlist not found")
                val validToken = ensureValidAccessToken(authStore.getTokens())
                Log.d(
                    TAG,
                    "Opening playlist $playlistId for current user ${data.profile.id}, playlist owner ${playlist.ownerId}"
                )
                runCatching {
                    apiClient.fetchPlaylistDetails(validToken.accessToken, playlistId)
                }.recoverCatching { throwable ->
                    if (throwable is SpotifyApiException) {
                        Log.e(
                            TAG,
                            "Playlist detail request failed for playlist $playlistId with ${throwable.statusCode}: ${throwable.responseBody}"
                        )
                    }
                    val tracks = runCatching {
                        apiClient.fetchPlaylistTracks(validToken.accessToken, playlistId)
                    }.getOrElse { trackThrowable ->
                        if (trackThrowable is SpotifyApiException) {
                            Log.e(
                                TAG,
                                "Playlist tracks request failed for playlist $playlistId with ${trackThrowable.statusCode}: ${trackThrowable.responseBody}"
                            )
                        }
                        throw IllegalStateException(
                            buildPlaylistAccessErrorMessage(
                                playlistId = playlistId,
                                currentUserId = data.profile.id,
                                playlistOwnerId = playlist.ownerId,
                                throwable = trackThrowable
                            ),
                            trackThrowable
                        )
                    }
                    SpotifyPlaylistDetails(
                        playlist = playlist,
                        tracks = tracks
                    )
                }.getOrThrow()
            }
        }.onFailure { throwable ->
            Log.e(TAG, "Failed to fetch playlist details for $playlistId", throwable)
        }
    }

    private fun buildPlaylistAccessErrorMessage(
        playlistId: String,
        currentUserId: String,
        playlistOwnerId: String?,
        throwable: Throwable
    ): String {
        val ownerText = playlistOwnerId ?: "unknown"
        return if (throwable is SpotifyApiException) {
            "Spotify API request failed (${throwable.statusCode})\n" +
                "playlistId=$playlistId\n" +
                "signedInAs=$currentUserId\n" +
                "playlistOwner=$ownerText\n" +
                "response=${throwable.responseBody.ifBlank { "empty" }}"
        } else {
            (throwable.localizedMessage ?: "Failed to load playlist") +
                "\nplaylistId=$playlistId\n" +
                "signedInAs=$currentUserId\n" +
                "playlistOwner=$ownerText"
        }
    }

    private fun syncSpotifyData(): SpotifyUserData = syncSpotifyData(authStore.getTokens())

    private fun syncSpotifyData(tokens: SpotifyTokens?): SpotifyUserData {
        val validToken = ensureValidAccessToken(tokens)
        val profile = apiClient.fetchCurrentUserProfile(validToken.accessToken)
        val playlists = apiClient.fetchPlaylists(validToken.accessToken)
        val recentlyPlayed = apiClient.fetchRecentlyPlayed(validToken.accessToken)
        val topTracks = apiClient.fetchTopTracks(validToken.accessToken)
        val topArtists = apiClient.fetchTopArtists(validToken.accessToken)
        val savedTracks = apiClient.fetchSavedTracks(validToken.accessToken)

        val userData = SpotifyUserData(
            profile = profile,
            playlists = playlists,
            recentlyPlayed = recentlyPlayed,
            topTracks = topTracks,
            topArtists = topArtists,
            savedTracks = savedTracks
        )

        updateState(
            SpotifyConnectionState(
                isConnected = true,
                isBusy = false,
                errorMessage = null,
                statusMessage = "Spotify account connected. Tap to refresh your data.",
                userData = userData
            )
        )
        Log.d(TAG, "Spotify data synced for ${profile.id}")
        return userData
    }

    private fun ensureValidAccessToken(tokens: SpotifyTokens?): SpotifyTokens {
        val existingTokens = tokens ?: throw IllegalStateException("Spotify is not connected")
        if (existingTokens.expiresAtMillis > System.currentTimeMillis() + TOKEN_EXPIRY_BUFFER_MS) {
            return existingTokens
        }

        val refreshToken = existingTokens.refreshToken
            ?: throw IllegalStateException("Spotify refresh token is missing")
        Log.d(TAG, "Refreshing Spotify access token")
        val refreshedTokens = apiClient.refreshAccessToken(refreshToken)
        val resolvedTokens = if (refreshedTokens.refreshToken == null) {
            refreshedTokens.copy(refreshToken = refreshToken)
        } else {
            refreshedTokens
        }
        authStore.saveTokens(resolvedTokens)
        return resolvedTokens
    }

    private fun initialState(): SpotifyConnectionState {
        return if (authStore.getTokens() == null) {
            SpotifyConnectionState()
        } else {
            SpotifyConnectionState(
                isConnected = true,
                statusMessage = "Spotify account connected. Tap to refresh your data."
            )
        }
    }

    private fun updateState(state: SpotifyConnectionState) {
        _connectionState.postValue(state)
    }

    companion object {
        @Volatile
        private var instance: SpotifyRepository? = null

        fun getInstance(context: Context): SpotifyRepository {
            return instance ?: synchronized(this) {
                instance ?: SpotifyRepository(context).also { instance = it }
            }
        }
    }
}
