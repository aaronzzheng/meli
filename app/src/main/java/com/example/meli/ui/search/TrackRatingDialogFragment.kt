package com.example.meli.ui.search

import android.app.Dialog
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.example.meli.R
import com.example.meli.databinding.FragmentTrackRatingDialogBinding
import com.example.meli.model.ComparisonChoice
import com.example.meli.model.RatingSentiment

class TrackRatingDialogFragment : DialogFragment() {

    private var _binding: FragmentTrackRatingDialogBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TrackRatingViewModel by viewModels()
    private var syncingNotes = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            setCanceledOnTouchOutside(true)
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrackRatingDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonBackTrackDetail.setOnClickListener {
            dismiss()
        }

        binding.choiceLoveCard.setOnClickListener { viewModel.selectSentiment(RatingSentiment.LOVE) }
        binding.choiceFineCard.setOnClickListener { viewModel.selectSentiment(RatingSentiment.FINE) }
        binding.choiceDislikeCard.setOnClickListener { viewModel.selectSentiment(RatingSentiment.DISLIKE) }

        binding.compareCurrentButton.setOnClickListener {
            viewModel.submitComparison(ComparisonChoice.CURRENT)
        }
        binding.compareOtherButton.setOnClickListener {
            viewModel.submitComparison(ComparisonChoice.OTHER)
        }
        binding.compareTieButton.setOnClickListener {
            viewModel.submitComparison(ComparisonChoice.TIE)
        }
        binding.trackDetailDeleteButton.setOnClickListener {
            viewModel.deleteRating()
        }

        binding.notesEditText.addTextChangedListener { editable ->
            if (!syncingNotes) {
                viewModel.updateNotes(editable?.toString().orEmpty())
            }
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            binding.trackDetailTitleText.text = state.trackTitle
            binding.trackDetailArtistText.text = state.artistText
            binding.trackDetailAlbumText.text =
                state.albumTitle.ifBlank { getString(R.string.rating_album_unknown) }
            binding.trackDetailHelperText.text = state.helperText.orEmpty()
            binding.trackDetailHelperText.visibility =
                if (state.helperText.isNullOrBlank()) View.GONE else View.VISIBLE
            binding.trackDetailProgress.visibility =
                if (state.isLoading || state.isSaving) View.VISIBLE else View.GONE
            binding.ratingComposerGroup.visibility = if (state.showComposer) View.VISIBLE else View.GONE
            binding.comparisonCard.visibility = if (state.showComposer) View.VISIBLE else View.GONE
            binding.trackDetailDeleteButton.visibility = if (state.currentRating != null) View.VISIBLE else View.GONE

            val comparisonTrack = state.comparisonTrack
            binding.compareCurrentButton.text = buildComparisonButtonText(
                title = state.trackTitle.ifBlank { getString(R.string.rating_this_song) },
                score = state.currentRating?.formattedScore,
                colorRes = state.selectedSentiment?.toScoreColorRes() ?: R.color.meli_green
            )
            binding.compareOtherButton.text = comparisonTrack?.let {
                buildComparisonButtonText(
                    title = it.trackTitle,
                    score = it.formattedScore,
                    colorRes = it.sentiment.toScoreColorRes()
                )
            } ?: ""
            binding.compareOtherButton.isEnabled = comparisonTrack != null
            binding.compareOtherButton.visibility = if (comparisonTrack == null) View.GONE else View.VISIBLE
            binding.compareOrText.visibility = if (comparisonTrack == null) View.GONE else View.VISIBLE
            binding.comparisonEmptyText.visibility = if (comparisonTrack == null) View.VISIBLE else View.GONE
            binding.comparisonEmptyText.text = if (comparisonTrack == null) {
                state.helperText.orEmpty()
            } else {
                getString(R.string.rating_compare_against, comparisonTrack.trackTitle)
            }

            setChoiceState(binding.choiceLoveCard, state.selectedSentiment == RatingSentiment.LOVE)
            setChoiceState(binding.choiceFineCard, state.selectedSentiment == RatingSentiment.FINE)
            setChoiceState(binding.choiceDislikeCard, state.selectedSentiment == RatingSentiment.DISLIKE)
            setButtonState(binding.compareCurrentButton, state.selectedComparisonChoice == ComparisonChoice.CURRENT)
            setButtonState(binding.compareOtherButton, state.selectedComparisonChoice == ComparisonChoice.OTHER)
            setButtonState(binding.compareTieButton, state.selectedComparisonChoice == ComparisonChoice.TIE)

            if (binding.notesEditText.text?.toString() != state.notes) {
                syncingNotes = true
                binding.notesEditText.setText(state.notes)
                binding.notesEditText.setSelection(binding.notesEditText.text?.length ?: 0)
                syncingNotes = false
            }

            if (!state.message.isNullOrBlank()) {
                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                viewModel.consumeMessage()
                if (state.message == "Ranking saved." || state.message == "Ranking deleted.") {
                    setFragmentResult(
                        RESULT_KEY,
                        bundleOf(ARG_TRACK_ID to state.trackId)
                    )
                    dismiss()
                }
            }
        }

        viewModel.initialize(
            trackId = arguments?.getString(ARG_TRACK_ID).orEmpty(),
            trackTitle = arguments?.getString(ARG_TRACK_NAME).orEmpty(),
            artistText = arguments?.getString(ARG_TRACK_ARTISTS).orEmpty(),
            albumTitle = arguments?.getString(ARG_TRACK_ALBUM).orEmpty(),
            imageUrl = arguments?.getString(ARG_TRACK_IMAGE_URL).orEmpty()
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setChoiceState(
        card: com.google.android.material.card.MaterialCardView,
        isSelected: Boolean
    ) {
        card.strokeWidth = if (isSelected) 4 else 2
        card.setCardBackgroundColor(
            resources.getColor(
                if (isSelected) R.color.meli_surface_subtle else R.color.meli_surface,
                null
            )
        )
    }

    private fun setButtonState(
        button: com.google.android.material.button.MaterialButton,
        isSelected: Boolean
    ) {
        button.isChecked = isSelected
    }

    private fun buildComparisonButtonText(title: String, score: String?, colorRes: Int): CharSequence {
        if (score.isNullOrBlank()) return title
        val fullText = "$title\n$score"
        return SpannableString(fullText).apply {
            val scoreStart = fullText.lastIndexOf(score)
            if (scoreStart >= 0) {
                setSpan(
                    ForegroundColorSpan(requireContext().getColor(colorRes)),
                    scoreStart,
                    scoreStart + score.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
    }

    private fun RatingSentiment.toScoreColorRes(): Int {
        return when (this) {
            RatingSentiment.LOVE -> R.color.meli_green
            RatingSentiment.FINE -> R.color.meli_amber
            RatingSentiment.DISLIKE -> R.color.meli_red
        }
    }

    companion object {
        const val TAG = "track_rating_dialog"
        const val RESULT_KEY = "track_rating_changed"

        fun newInstance(args: Bundle): TrackRatingDialogFragment {
            return TrackRatingDialogFragment().apply {
                arguments = Bundle(args)
            }
        }
    }
}
