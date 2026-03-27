package com.example.meli.spotify

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.meli.MainActivity
import com.example.meli.data.repository.SpotifyRepository
import kotlinx.coroutines.launch

private const val TAG = "SpotifyCallback"

class SpotifyAuthCallbackActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val callbackUri = intent?.data
        if (callbackUri == null) {
            Log.e(TAG, "Spotify callback missing URI")
            finishToMainActivity()
            return
        }

        lifecycleScope.launch {
            val repository = SpotifyRepository.getInstance(applicationContext)
            repository.handleRedirect(callbackUri)
                .onSuccess {
                    Toast.makeText(
                        this@SpotifyAuthCallbackActivity,
                        "Spotify connected successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .onFailure { throwable ->
                    Toast.makeText(
                        this@SpotifyAuthCallbackActivity,
                        throwable.localizedMessage ?: "Spotify connection failed",
                        Toast.LENGTH_LONG
                    ).show()
                }
            finishToMainActivity()
        }
    }

    private fun finishToMainActivity() {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
        )
        finish()
    }
}
