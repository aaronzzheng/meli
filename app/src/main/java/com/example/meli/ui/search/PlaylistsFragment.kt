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
import com.example.meli.databinding.FragmentPlaylistsBinding
import com.example.meli.spotify.SpotifyPlaylist

class PlaylistsFragment : Fragment() {

    private var _binding: FragmentPlaylistsBinding? = null
    private val binding get() = _binding!!
    private val spotifyViewModel: SpotifyLibraryViewModel by viewModels()

    private val playlistsAdapter = SpotifyPlaylistGridAdapter(
        onCoverClicked = { playlist -> openPlaylistDetail(playlist) }
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlaylistsBinding.inflate(inflater, container, false)
        binding.playlistsRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.playlistsRecyclerView.adapter = playlistsAdapter
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.buttonBackPlaylists.setOnClickListener {
            findNavController().navigateUp()
        }

        spotifyViewModel.spotifyState.observe(viewLifecycleOwner) { state ->
            playlistsAdapter.submitList(state.userData?.playlists.orEmpty())
            binding.playlistsEmptyText.visibility =
                if (state.userData?.playlists.isNullOrEmpty()) View.VISIBLE else View.GONE
        }

        spotifyViewModel.refreshSpotifyData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.playlistsRecyclerView.adapter = null
        _binding = null
    }

    private fun openPlaylistDetail(playlist: SpotifyPlaylist) {
        findNavController().navigate(
            R.id.playlistDetailFragment,
            bundleOf(
                ARG_PLAYLIST_ID to playlist.id,
                ARG_PLAYLIST_NAME to playlist.name,
                ARG_PLAYLIST_IMAGE_URL to playlist.imageUrl.orEmpty(),
                ARG_PLAYLIST_OWNER_ID to playlist.ownerId.orEmpty(),
                ARG_PLAYLIST_OWNER to playlist.ownerName.orEmpty(),
                ARG_PLAYLIST_DESCRIPTION to playlist.description.orEmpty(),
                ARG_PLAYLIST_TRACK_COUNT to playlist.totalTracks
            )
        )
    }
}
