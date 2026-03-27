package com.example.meli.ui.settings

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.meli.R
import com.example.meli.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private var lastSpotifyError: String? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val TAG = "SettingsLifecycle"
    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "SettingsFragment onCreate")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "SettingsFragment onCreateView")
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.spotifyState.observe(viewLifecycleOwner) { state ->
            val titleRes = if (state.isConnected) {
                R.string.spotify_connected_title
            } else {
                R.string.spotify_connect_title
            }
            binding.importOptionText.setText(titleRes)
            binding.importStatusText.text = state.statusMessage
            binding.importOptionRow.isEnabled = !state.isBusy
            binding.importOptionRow.alpha = if (state.isBusy) 0.6f else 1f

            state.errorMessage?.takeIf { it.isNotBlank() }?.let { message ->
                if (message != lastSpotifyError) {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                    lastSpotifyError = message
                }
            } ?: run {
                lastSpotifyError = null
            }
        }

        binding.buttonCloseSettings.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.settingsOptionRow.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_settingsDetailFragment)
        }
        binding.faqOptionRow.setOnClickListener {
            Toast.makeText(requireContext(), "FAQ coming soon", Toast.LENGTH_SHORT).show()
        }
        binding.importOptionRow.setOnClickListener {
            if (viewModel.spotifyState.value?.isConnected == true) {
                showSpotifyActions()
            } else {
                startActivity(viewModel.connectSpotify())
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "SettingsFragment onResume")
        viewModel.refreshSpotifyData()
    }

    override fun onPause() {
        Log.d(TAG, "SettingsFragment onPause")
        super.onPause()
    }

    override fun onDestroyView() {
        Log.d(TAG, "SettingsFragment onDestroy")
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        Log.d(TAG, "SettingsFragment onDestroy")
        super.onDestroy()
    }

    private fun showSpotifyActions() {
        AlertDialog.Builder(requireContext())
            .setTitle("Spotify")
            .setItems(
                arrayOf("Refresh data", "Reconnect Spotify", "Disconnect Spotify")
            ) { _, which ->
                when (which) {
                    0 -> viewModel.refreshSpotifyData()
                    1 -> startActivity(viewModel.reconnectSpotify())
                    2 -> viewModel.disconnectSpotify()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
