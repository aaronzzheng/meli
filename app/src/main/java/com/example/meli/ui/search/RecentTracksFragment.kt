package com.example.meli.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.meli.R
import com.example.meli.databinding.FragmentRecentTracksBinding
import com.example.meli.spotify.SpotifyTrack

class RecentTracksFragment : Fragment() {

    private var _binding: FragmentRecentTracksBinding? = null
    private val binding get() = _binding!!
    private val spotifyViewModel: SpotifyLibraryViewModel by viewModels()

    private val recentTracksAdapter = SpotifyTrackGridAdapter(
        onCoverClicked = { track -> openTrackDetail(track) },
        onAddClicked = {
            Toast.makeText(requireContext(), "Ranking integration coming soon", Toast.LENGTH_SHORT).show()
        }
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecentTracksBinding.inflate(inflater, container, false)
        binding.recentTracksRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recentTracksRecyclerView.adapter = recentTracksAdapter
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.buttonBackRecentTracks.setOnClickListener {
            findNavController().navigateUp()
        }

        spotifyViewModel.spotifyState.observe(viewLifecycleOwner) { state ->
            recentTracksAdapter.submitList(
                state.userData?.recentlyPlayed
                    ?.distinctBy { it.id.ifBlank { "${it.name}-${it.artistNames.joinToString(",")}" } }
                    .orEmpty()
            )
            binding.recentTracksEmptyText.visibility =
                if (state.userData?.recentlyPlayed.isNullOrEmpty()) View.VISIBLE else View.GONE
        }

        spotifyViewModel.refreshSpotifyData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recentTracksRecyclerView.adapter = null
        _binding = null
    }

    private fun openTrackDetail(track: SpotifyTrack) {
        findNavController().navigate(
            R.id.trackDetailFragment,
            bundleOf(
                ARG_TRACK_ID to track.id,
                ARG_TRACK_NAME to track.name,
                ARG_TRACK_ARTISTS to track.artistNames.joinToString(", "),
                ARG_TRACK_ALBUM to track.albumName,
                ARG_TRACK_IMAGE_URL to track.imageUrl
            )
        )
    }
}
