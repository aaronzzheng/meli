package com.example.meli.spotify

data class SpotifyTokens(
    val accessToken: String,
    val refreshToken: String?,
    val expiresInSeconds: Long,
    val expiresAtMillis: Long
)

data class SpotifyProfile(
    val id: String,
    val displayName: String,
    val email: String?,
    val country: String?,
    val product: String?,
    val imageUrl: String?
)

data class SpotifyPlaylist(
    val id: String,
    val name: String,
    val description: String?,
    val ownerId: String?,
    val ownerName: String?,
    val totalTracks: Int,
    val imageUrl: String?
)

data class SpotifyTrack(
    val id: String,
    val name: String,
    val artistNames: List<String>,
    val albumName: String?,
    val imageUrl: String?,
    val playedAt: String? = null,
    val addedAt: String? = null
)

data class SpotifyArtist(
    val id: String,
    val name: String,
    val genres: List<String>,
    val imageUrl: String?
)

data class SpotifyUserData(
    val profile: SpotifyProfile,
    val playlists: List<SpotifyPlaylist>,
    val recentlyPlayed: List<SpotifyTrack>,
    val topTracks: List<SpotifyTrack>,
    val topArtists: List<SpotifyArtist>,
    val savedTracks: List<SpotifyTrack>
)

data class SpotifyPlaylistDetails(
    val playlist: SpotifyPlaylist,
    val tracks: List<SpotifyTrack>
)

data class SpotifyConnectionState(
    val isConnected: Boolean = false,
    val isBusy: Boolean = false,
    val statusMessage: String = "Sign in with Spotify to import your library and listening data.",
    val errorMessage: String? = null,
    val userData: SpotifyUserData? = null
)
