package com.example.meli.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import coil.load
import com.example.meli.R
import com.example.meli.data.repository.TrackRatingRepository
import com.example.meli.databinding.FragmentTrackDetailBinding
import com.example.meli.model.FriendTrackRating
import kotlinx.coroutines.launch

class TrackDetailFragment : Fragment() {

    private var _binding: FragmentTrackDetailBinding? = null
    private val binding get() = _binding!!
    private val repository = TrackRatingRepository()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrackDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonBackTrackDetail.setOnClickListener {
            findNavController().navigateUp()
        }
        binding.trackDetailAddButton.setOnClickListener {
            TrackRatingDialogFragment.newInstance(
                bundleOf(
                    ARG_TRACK_ID to arguments?.getString(ARG_TRACK_ID).orEmpty(),
                    ARG_TRACK_NAME to arguments?.getString(ARG_TRACK_NAME).orEmpty(),
                    ARG_TRACK_ARTISTS to arguments?.getString(ARG_TRACK_ARTISTS).orEmpty(),
                    ARG_TRACK_ALBUM to arguments?.getString(ARG_TRACK_ALBUM).orEmpty(),
                    ARG_TRACK_IMAGE_URL to arguments?.getString(ARG_TRACK_IMAGE_URL).orEmpty()
                )
            ).show(parentFragmentManager, TrackRatingDialogFragment.TAG)
        }

        setFragmentResultListener(TrackRatingDialogFragment.RESULT_KEY) { _, _ ->
            loadTrackSummary()
        }

        binding.trackDetailCoverImage.load(arguments?.getString(ARG_TRACK_IMAGE_URL)) {
            crossfade(true)
            placeholder(R.drawable.bg_profile_album_placeholder)
            error(R.drawable.bg_profile_album_placeholder)
        }
        binding.trackDetailTitleText.text = arguments?.getString(ARG_TRACK_NAME).orEmpty()
        binding.trackDetailArtistText.text = arguments?.getString(ARG_TRACK_ARTISTS).orEmpty()
        binding.trackDetailAlbumText.text = arguments?.getString(ARG_TRACK_ALBUM).orEmpty()

        loadTrackSummary()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        loadTrackSummary()
    }

    private fun loadTrackSummary() {
        val trackId = arguments?.getString(ARG_TRACK_ID).orEmpty()
        if (trackId.isBlank() || _binding == null) return

        viewLifecycleOwner.lifecycleScope.launch {
            repository.getRating(trackId)
                .onSuccess { rating ->
                    val scoreText = rating?.formattedScore ?: getString(R.string.spotify_not_ranked)
                    val metaText = if (rating == null) {
                        getString(R.string.track_detail_score_empty)
                    } else {
                        getString(
                            R.string.track_detail_score_meta,
                            rating.sentiment.label,
                            rating.comparisonCount
                        )
                    }
                    binding.trackDetailCurrentScoreValue.text = scoreText
                    binding.trackDetailCurrentScoreValue.setTextColor(
                        requireContext().getColor(
                            when (rating?.sentiment) {
                                com.example.meli.model.RatingSentiment.LOVE -> R.color.meli_green
                                com.example.meli.model.RatingSentiment.FINE -> R.color.meli_amber
                                com.example.meli.model.RatingSentiment.DISLIKE -> R.color.meli_red
                                null -> R.color.meli_text_muted
                            }
                        )
                    )
                    binding.trackDetailCurrentScoreMeta.text = metaText
                    binding.trackDetailNotesText.text = rating?.notes?.takeIf { it.isNotBlank() }
                        ?: getString(R.string.track_detail_notes_empty)
                    binding.trackDetailAddButton.text = if (rating == null) {
                        getString(R.string.rating_plus)
                    } else {
                        getString(R.string.track_detail_rerank)
                    }
                }
            repository.loadFriendRatingsForTrack(trackId)
                .onSuccess { friendRatings ->
                    binding.trackDetailFriendsText.text = formatFriendRatings(friendRatings)
                }
        }
    }

    private fun formatFriendRatings(friendRatings: List<FriendTrackRating>): String {
        if (friendRatings.isEmpty()) {
            return getString(R.string.track_detail_friends_empty)
        }
        return friendRatings.take(5).joinToString(separator = "\n") { friendRating ->
            "${friendRating.userName}: ${friendRating.formattedScore}"
        }
    }
}
