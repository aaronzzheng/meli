package com.example.meli.spotify

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

object SpotifyPkceUtils {
    private val secureRandom = SecureRandom()

    fun generateCodeVerifier(): String = generateRandomUrlSafeValue(64)

    fun generateState(): String = generateRandomUrlSafeValue(32)

    fun generateCodeChallenge(codeVerifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(codeVerifier.toByteArray(Charsets.US_ASCII))
        return Base64.encodeToString(
            digest,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
    }

    private fun generateRandomUrlSafeValue(byteCount: Int): String {
        val bytes = ByteArray(byteCount)
        secureRandom.nextBytes(bytes)
        return Base64.encodeToString(
            bytes,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
    }
}
