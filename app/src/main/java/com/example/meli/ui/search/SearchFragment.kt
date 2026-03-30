package com.example.meli.ui.search

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.meli.R
import com.example.meli.databinding.FragmentSearchBinding
import com.example.meli.model.FriendshipStatus
import com.example.meli.spotify.SpotifyPlaylist
import com.example.meli.spotify.SpotifyTrack
import com.example.meli.ui.home.HomeUserSearchAdapter
import com.google.firebase.auth.FirebaseAuth

private const val TAG = "SearchLifecycle"
private const val ARG_PROFILE_UID = "profileUid"

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SearchViewModel by viewModels()
    private val spotifyViewModel: SpotifyLibraryViewModel by viewModels()
    private val userSearchAdapter = HomeUserSearchAdapter(
        onUserClicked = { user ->
            findNavController().navigate(
                R.id.action_navigation_search_to_userProfileFragment,
                bundleOf(ARG_PROFILE_UID to user.uid)
            )
        },
        onAddFriendClicked = { user ->
            when (user.friendshipStatus) {
                FriendshipStatus.REQUESTED ->
                    viewModel.cancelFriendRequest(user.uid, FirebaseAuth.getInstance().currentUser?.uid)
                FriendshipStatus.NONE ->
                    viewModel.sendFriendRequest(user.uid, FirebaseAuth.getInstance().currentUser?.uid)
                else -> Unit
            }
        }
    )
    private val recentTracksAdapter = SpotifyTrackGridAdapter(
        onCoverClicked = { track -> openTrackDetail(track) },
        onAddClicked = { track -> openTrackRatingOverlay(track) }
    )
    private val playlistsAdapter = SpotifyPlaylistGridAdapter(
        onCoverClicked = { playlist -> openPlaylistDetail(playlist) }
    )
    private val songSearchAdapter = SpotifyTrackListAdapter(
        onTrackClicked = { track -> openTrackDetail(track) },
        onAddClicked = { track -> openTrackRatingOverlay(track) }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "SearchFragment onCreate")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "SearchFragment onCreateView")
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        binding.searchResultsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.searchResultsRecyclerView.adapter = userSearchAdapter
        binding.recentlyPlayedRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recentlyPlayedRecyclerView.adapter = recentTracksAdapter
        binding.playlistsRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.playlistsRecyclerView.adapter = playlistsAdapter
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeSearch()
        observeSpotify()
        setFragmentResultListener(TrackRatingDialogFragment.RESULT_KEY) { _, _ ->
            spotifyViewModel.refreshRatedTrackIds()
        }

        binding.membersTab.setOnClickListener {
            viewModel.setMode(SearchViewModel.SearchMode.MEMBERS)
        }
        binding.songsTab.setOnClickListener {
            viewModel.setMode(SearchViewModel.SearchMode.SONGS)
            spotifyViewModel.refreshSpotifyData()
        }
        binding.searchEditText.doAfterTextChanged { text ->
            if (viewModel.mode.value == SearchViewModel.SearchMode.MEMBERS) {
                viewModel.searchUsers(
                    query = text?.toString().orEmpty(),
                    currentUid = FirebaseAuth.getInstance().currentUser?.uid
                )
            } else {
                spotifyViewModel.searchTracks(text?.toString().orEmpty())
            }
            renderSearchState()
        }
        binding.recentlyPlayedMoreText.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_search_to_recentTracksFragment)
        }
        binding.playlistsMoreText.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_search_to_playlistsFragment)
        }

        viewModel.setMode(SearchViewModel.SearchMode.SONGS)
        spotifyViewModel.refreshSpotifyData()
    }

    private fun observeSearch() {
        viewModel.mode.observe(viewLifecycleOwner) { mode ->
            val isMembers = mode == SearchViewModel.SearchMode.MEMBERS
            binding.membersUnderline.visibility = if (isMembers) View.VISIBLE else View.INVISIBLE
            binding.songsUnderline.visibility = if (isMembers) View.INVISIBLE else View.VISIBLE
            binding.membersTab.alpha = if (isMembers) 1f else 0.55f
            binding.songsTab.alpha = if (isMembers) 0.55f else 1f
            binding.searchEditText.hint = if (isMembers) {
                "Search members"
            } else {
                "Search songs"
            }
            renderSearchState()
        }

        viewModel.searchResults.observe(viewLifecycleOwner) { results ->
            userSearchAdapter.submitList(results) {
                renderSearchState()
            }
        }

        viewModel.searchLoading.observe(viewLifecycleOwner) {
            renderSearchState()
        }

        viewModel.searchError.observe(viewLifecycleOwner) { error ->
            binding.searchEmptyText.text = error ?: "No members found."
            renderSearchState()
        }

        viewModel.friendActionMessage.observe(viewLifecycleOwner) { message ->
            if (!message.isNullOrBlank()) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                viewModel.clearFriendActionMessage()
            }
        }
    }

    private fun observeSpotify() {
        spotifyViewModel.spotifyState.observe(viewLifecycleOwner) { state ->
            val userData = state.userData
            recentTracksAdapter.submitList(
                userData?.recentlyPlayed
                    ?.distinctBy { it.id.ifBlank { "${it.name}-${it.artistNames.joinToString(",")}" } }
                    ?.take(4)
                    .orEmpty()
            )
            playlistsAdapter.submitList(userData?.playlists?.take(4).orEmpty())
            binding.songPlaceholderText.text = state.errorMessage ?: getString(R.string.spotify_not_connected)
            binding.songPlaceholderCard.visibility =
                if (state.isConnected) View.GONE else View.VISIBLE
            binding.recentlyPlayedSection.visibility =
                if (state.isConnected && !userData?.recentlyPlayed.isNullOrEmpty()) View.VISIBLE else View.GONE
            binding.playlistsSection.visibility =
                if (state.isConnected && !userData?.playlists.isNullOrEmpty()) View.VISIBLE else View.GONE
            renderSearchState()
        }

        spotifyViewModel.trackSearchResults.observe(viewLifecycleOwner) { tracks ->
            songSearchAdapter.submitList(tracks)
            renderSearchState()
        }

        spotifyViewModel.trackSearchLoading.observe(viewLifecycleOwner) {
            renderSearchState()
        }

        spotifyViewModel.trackSearchError.observe(viewLifecycleOwner) { error ->
            if (viewModel.mode.value == SearchViewModel.SearchMode.SONGS) {
                binding.searchEmptyText.text = error ?: "No songs found."
            }
            renderSearchState()
        }

        spotifyViewModel.ratedTrackIds.observe(viewLifecycleOwner) { ratedTrackIds ->
            recentTracksAdapter.updateRatedTrackIds(ratedTrackIds)
            songSearchAdapter.updateRatedTrackIds(ratedTrackIds)
        }
    }

    private fun renderSearchState() {
        val query = binding.searchEditText.text?.toString().orEmpty().trim()
        val mode = viewModel.mode.value ?: SearchViewModel.SearchMode.MEMBERS
        val shouldShowResults = query.length >= 2
        val hasResults = viewModel.searchResults.value?.isNotEmpty() == true
        val isLoading = viewModel.searchLoading.value == true
        val isMembers = mode == SearchViewModel.SearchMode.MEMBERS
        val trackSearching = spotifyViewModel.trackSearchLoading.value == true
        val hasTrackResults = spotifyViewModel.trackSearchResults.value?.isNotEmpty() == true
        val isSpotifyConnected = spotifyViewModel.spotifyState.value?.isConnected == true
        val hasSpotifyContent = !spotifyViewModel.spotifyState.value?.userData?.recentlyPlayed.isNullOrEmpty() ||
            !spotifyViewModel.spotifyState.value?.userData?.playlists.isNullOrEmpty()

        if (isMembers) {
            binding.searchResultsRecyclerView.adapter = userSearchAdapter
            binding.searchResultsRecyclerView.visibility =
                if (shouldShowResults && hasResults) View.VISIBLE else View.GONE
            binding.searchEmptyText.visibility =
                if (shouldShowResults && !hasResults && !isLoading) View.VISIBLE else View.GONE
            binding.songContentScrollView.visibility = View.GONE
            return
        }

        val showingSongSearch = shouldShowResults
        binding.searchResultsRecyclerView.adapter = songSearchAdapter
        binding.searchResultsRecyclerView.visibility =
            if (showingSongSearch && hasTrackResults) View.VISIBLE else View.GONE
        binding.searchEmptyText.visibility =
            if (showingSongSearch && !hasTrackResults && !trackSearching) View.VISIBLE else View.GONE
        binding.songContentScrollView.visibility = if (showingSongSearch) View.GONE else View.VISIBLE

        if (!showingSongSearch && !isSpotifyConnected) {
            binding.songPlaceholderCard.visibility = View.VISIBLE
            binding.songPlaceholderText.text = getString(R.string.spotify_not_connected)
        } else if (!showingSongSearch && !hasSpotifyContent) {
            binding.songPlaceholderCard.visibility = View.VISIBLE
            binding.songPlaceholderText.text = getString(R.string.spotify_no_recent_tracks)
        }
    }

    override fun onDestroyView() {
        Log.d(TAG, "SearchFragment onDestroy")
        super.onDestroyView()
        binding.searchResultsRecyclerView.adapter = null
        binding.recentlyPlayedRecyclerView.adapter = null
        binding.playlistsRecyclerView.adapter = null
        _binding = null
    }

    override fun onDestroy() {
        Log.d(TAG, "SearchFragment onDestroy")
        super.onDestroy()
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
