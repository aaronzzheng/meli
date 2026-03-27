package com.example.meli.spotify

import android.content.Context

class SpotifyAuthStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun savePendingAuthorization(codeVerifier: String, state: String) {
        prefs.edit()
            .putString(KEY_CODE_VERIFIER, codeVerifier)
            .putString(KEY_STATE, state)
            .apply()
    }

    fun getPendingCodeVerifier(): String? = prefs.getString(KEY_CODE_VERIFIER, null)

    fun getPendingState(): String? = prefs.getString(KEY_STATE, null)

    fun clearPendingAuthorization() {
        prefs.edit()
            .remove(KEY_CODE_VERIFIER)
            .remove(KEY_STATE)
            .apply()
    }

    fun saveTokens(tokens: SpotifyTokens) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, tokens.accessToken)
            .putString(KEY_REFRESH_TOKEN, tokens.refreshToken)
            .putLong(KEY_EXPIRES_AT, tokens.expiresAtMillis)
            .putLong(KEY_EXPIRES_IN, tokens.expiresInSeconds)
            .apply()
    }

    fun getTokens(): SpotifyTokens? {
        val accessToken = prefs.getString(KEY_ACCESS_TOKEN, null) ?: return null
        val expiresAtMillis = prefs.getLong(KEY_EXPIRES_AT, 0L)
        val expiresInSeconds = prefs.getLong(KEY_EXPIRES_IN, 0L)
        return SpotifyTokens(
            accessToken = accessToken,
            refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null),
            expiresInSeconds = expiresInSeconds,
            expiresAtMillis = expiresAtMillis
        )
    }

    fun clearTokens() {
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_EXPIRES_AT)
            .remove(KEY_EXPIRES_IN)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "spotify_auth"
        private const val KEY_CODE_VERIFIER = "code_verifier"
        private const val KEY_STATE = "oauth_state"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_EXPIRES_AT = "expires_at"
        private const val KEY_EXPIRES_IN = "expires_in"
    }
}
