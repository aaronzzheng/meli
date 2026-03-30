package com.example.meli.ui.search

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.example.meli.R
import com.example.meli.databinding.FragmentPlaylistDetailBinding
import com.example.meli.spotify.SpotifyTrack

private const val TAG = "PlaylistDetail"

class PlaylistDetailFragment : Fragment() {

    private var _binding: FragmentPlaylistDetailBinding? = null
    private val binding get() = _binding!!
    private val spotifyViewModel: SpotifyLibraryViewModel by viewModels()

    private val trackListAdapter = SpotifyTrackListAdapter(
        onTrackClicked = { track -> openTrackDetail(track) },
        onAddClicked = { track -> openTrackRatingOverlay(track) }
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlaylistDetailBinding.inflate(inflater, container, false)
        binding.playlistTracksRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.playlistTracksRecyclerView.adapter = trackListAdapter
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonBackPlaylistDetail.setOnClickListener {
            findNavController().navigateUp()
        }
        setFragmentResultListener(TrackRatingDialogFragment.RESULT_KEY) { _, _ ->
            spotifyViewModel.refreshRatedTrackIds()
        }
        binding.buttonOpenPlaylistInSpotify.setOnClickListener {
            openPlaylistInSpotify()
        }

        binding.playlistDetailCoverImage.load(arguments?.getString(ARG_PLAYLIST_IMAGE_URL)) {
            crossfade(true)
            placeholder(R.drawable.bg_profile_album_placeholder)
            error(R.drawable.bg_profile_album_placeholder)
        }
        binding.playlistDetailTitleText.text = arguments?.getString(ARG_PLAYLIST_NAME).orEmpty()
        binding.playlistDetailOwnerText.text = arguments?.getString(ARG_PLAYLIST_OWNER).orEmpty()
        binding.playlistDetailDescriptionText.text =
            arguments?.getString(ARG_PLAYLIST_DESCRIPTION).orEmpty()
        binding.playlistDetailMetaText.text =
            getString(R.string.spotify_playlist_track_count, arguments?.getInt(ARG_PLAYLIST_TRACK_COUNT) ?: 0)

        spotifyViewModel.playlistDetails.observe(viewLifecycleOwner) { details ->
            trackListAdapter.submitList(details?.tracks.orEmpty())
            binding.playlistDetailMetaText.text = getString(
                R.string.spotify_playlist_track_count,
                details?.playlist?.totalTracks ?: 0
            )
            val isEmpty = details?.tracks.isNullOrEmpty()
            binding.playlistTracksEmptyText.text = getString(R.string.spotify_no_playlist_tracks)
            binding.playlistTracksEmptyText.visibility = if (isEmpty) View.VISIBLE else View.GONE
            binding.buttonOpenPlaylistInSpotify.visibility = View.GONE
        }

        spotifyViewModel.playlistError.observe(viewLifecycleOwner) { error ->
            Log.e(
                TAG,
                "Playlist load error for id=${arguments?.getString(ARG_PLAYLIST_ID)} ownerId=${arguments?.getString(ARG_PLAYLIST_OWNER_ID)}: $error"
            )
            if (error?.contains("Spotify API request failed (403)") == true) {
                binding.playlistTracksEmptyText.text = getString(R.string.spotify_playlist_access_denied)
                binding.buttonOpenPlaylistInSpotify.visibility = View.VISIBLE
            } else {
                binding.playlistTracksEmptyText.text =
                    error ?: getString(R.string.spotify_no_playlist_tracks)
                binding.buttonOpenPlaylistInSpotify.visibility = View.GONE
            }
            binding.playlistTracksEmptyText.visibility = View.VISIBLE
        }

        spotifyViewModel.ratedTrackIds.observe(viewLifecycleOwner) { ratedTrackIds ->
            trackListAdapter.updateRatedTrackIds(ratedTrackIds)
        }

        spotifyViewModel.refreshRatedTrackIds()
        spotifyViewModel.loadPlaylistDetails(arguments?.getString(ARG_PLAYLIST_ID).orEmpty())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.playlistTracksRecyclerView.adapter = null
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

    private fun openTrackRatingOverlay(track: SpotifyTrack) {
        TrackRatingDialogFragment.newInstance(
            bundleOf(
                ARG_TRACK_ID to track.id,
                ARG_TRACK_NAME to track.name,
                ARG_TRACK_ARTISTS to track.artistNames.joinToString(", "),
                ARG_TRACK_ALBUM to track.albumName,
                ARG_TRACK_IMAGE_URL to track.imageUrl
            )
        ).show(parentFragmentManager, TrackRatingDialogFragment.TAG)
    }

    private fun openPlaylistInSpotify() {
        val playlistId = arguments?.getString(ARG_PLAYLIST_ID).orEmpty()
        if (playlistId.isBlank()) return

        val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("spotify:playlist:$playlistId"))
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://open.spotify.com/playlist/$playlistId"))

        val intent = if (appIntent.resolveActivity(requireContext().packageManager) != null) {
            appIntent
        } else {
            webIntent
        }
        startActivity(intent)
    }
}
