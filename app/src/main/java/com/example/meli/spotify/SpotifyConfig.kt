package com.example.meli.spotify

import android.net.Uri

object SpotifyConfig {
    const val clientId = "ef6bb3efb83d4d48a7e2ab29b943177d"
    const val redirectUri = "meli://callback"
    const val authorizationEndpoint = "https://accounts.spotify.com/authorize"
    const val tokenEndpoint = "https://accounts.spotify.com/api/token"
    const val apiBaseUrl = "https://api.spotify.com/v1"

    val scopes = listOf(
        "user-read-recently-played",
        "user-top-read",
        "playlist-read-private",
        "playlist-read-collaborative",
        "user-library-read",
        "user-read-email",
        "user-read-private"
    )

    fun buildAuthorizationUri(
        codeChallenge: String,
        state: String
    ): Uri {
        return Uri.parse(authorizationEndpoint)
            .buildUpon()
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("redirect_uri", redirectUri)
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("code_challenge", codeChallenge)
            .appendQueryParameter("state", state)
            .appendQueryParameter("scope", scopes.joinToString(" "))
            .build()
    }
}
