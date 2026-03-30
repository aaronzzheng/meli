package com.example.meli.ui.feed

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.meli.data.repository.ProfileRankingRepository
import com.example.meli.databinding.FragmentFeedCommentsSheetBinding
import com.example.meli.model.FeedComment
import com.example.meli.model.ProfileRankingActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class FeedCommentsBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentFeedCommentsSheetBinding? = null
    private val binding get() = _binding!!
    private val repository = ProfileRankingRepository()
    private lateinit var adapter: FeedCommentsAdapter
    private var activityItem: ProfileRankingActivity? = null
    private var replyToComment: FeedComment? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return BottomSheetDialog(requireContext(), theme).apply {
            behavior.peekHeight = (resources.displayMetrics.heightPixels * 0.55f).toInt()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeedCommentsSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activityItem = ProfileRankingActivity(
            id = requireArguments().getString(ARG_ID).orEmpty(),
            actorUid = requireArguments().getString(ARG_ACTOR_UID).orEmpty(),
            actorName = requireArguments().getString(ARG_ACTOR_NAME).orEmpty(),
            listId = requireArguments().getString(ARG_LIST_ID).orEmpty(),
            entryId = requireArguments().getString(ARG_ENTRY_ID).orEmpty(),
            trackTitle = requireArguments().getString(ARG_TRACK_TITLE).orEmpty(),
            artistText = requireArguments().getString(ARG_ARTIST_TEXT).orEmpty(),
            rankingScore = requireArguments().getString(ARG_SCORE)?.toDoubleOrNull(),
            updatedAtMillis = requireArguments().getLong(ARG_UPDATED_AT),
            listName = requireArguments().getString(ARG_LIST_NAME),
            imageUrl = requireArguments().getString(ARG_IMAGE_URL),
            notes = requireArguments().getString(ARG_NOTES).orEmpty(),
            likeCount = requireArguments().getInt(ARG_LIKE_COUNT),
            commentCount = requireArguments().getInt(ARG_COMMENT_COUNT),
            likedByCurrentUser = requireArguments().getBoolean(ARG_LIKED)
        )

        adapter = FeedCommentsAdapter(
            onReplyClicked = { comment ->
                replyToComment = comment
                binding.commentsReplyingToText.visibility = View.VISIBLE
                binding.commentsReplyingToText.text = "Replying to ${comment.actorName}"
                binding.commentsInputEditText.setText("@${comment.actorName} ")
                binding.commentsInputEditText.setSelection(binding.commentsInputEditText.text.length)
            },
            onLikeClicked = { comment ->
                val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return@FeedCommentsAdapter
                val item = activityItem ?: return@FeedCommentsAdapter
                lifecycleScope.launch {
                    repository.toggleCommentLike(item, comment, currentUid)
                        .onSuccess {
                            loadComments()
                            setFragmentResult(RESULT_KEY, bundleOf(ARG_ID to item.id))
                        }
                        .onFailure { error ->
                            Toast.makeText(requireContext(), error.localizedMessage ?: "Failed to like comment.", Toast.LENGTH_SHORT).show()
                        }
                }
            }
        )

        binding.commentsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.commentsRecyclerView.adapter = adapter
        binding.commentsSheetTitleText.text = activityItem?.trackTitle ?: "Comments"
        binding.commentsPostButton.setOnClickListener {
            val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnClickListener
            val item = activityItem ?: return@setOnClickListener
            val text = binding.commentsInputEditText.text?.toString().orEmpty()
            if (text.isBlank()) return@setOnClickListener
            lifecycleScope.launch {
                repository.addComment(item, currentUid, text, replyToComment?.actorName)
                    .onSuccess {
                        binding.commentsInputEditText.setText("")
                        binding.commentsReplyingToText.visibility = View.GONE
                        replyToComment = null
                        loadComments()
                        setFragmentResult(RESULT_KEY, bundleOf(ARG_ID to item.id))
                    }
                    .onFailure { error ->
                        Toast.makeText(requireContext(), error.localizedMessage ?: "Failed to post comment.", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        loadComments()
    }

    private fun loadComments() {
        val item = activityItem ?: return
        lifecycleScope.launch {
            repository.loadComments(item)
                .onSuccess { adapter.submitList(it) }
                .onFailure { error ->
                    Toast.makeText(requireContext(), error.localizedMessage ?: "Failed to load comments.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val RESULT_KEY = "feed_comments_changed"
        private const val ARG_ID = "id"
        private const val ARG_ACTOR_UID = "actorUid"
        private const val ARG_ACTOR_NAME = "actorName"
        private const val ARG_LIST_ID = "listId"
        private const val ARG_ENTRY_ID = "entryId"
        private const val ARG_TRACK_TITLE = "trackTitle"
        private const val ARG_ARTIST_TEXT = "artistText"
        private const val ARG_SCORE = "score"
        private const val ARG_UPDATED_AT = "updatedAt"
        private const val ARG_LIST_NAME = "listName"
        private const val ARG_IMAGE_URL = "imageUrl"
        private const val ARG_NOTES = "notes"
        private const val ARG_LIKE_COUNT = "likeCount"
        private const val ARG_COMMENT_COUNT = "commentCount"
        private const val ARG_LIKED = "likedByCurrentUser"

        fun newInstance(item: ProfileRankingActivity): FeedCommentsBottomSheetDialogFragment {
            return FeedCommentsBottomSheetDialogFragment().apply {
                arguments = bundleOf(
                    ARG_ID to item.id,
                    ARG_ACTOR_UID to item.actorUid,
                    ARG_ACTOR_NAME to item.actorName,
                    ARG_LIST_ID to item.listId,
                    ARG_ENTRY_ID to item.entryId,
                    ARG_TRACK_TITLE to item.trackTitle,
                    ARG_ARTIST_TEXT to item.artistText,
                    ARG_SCORE to item.rankingScore?.toString(),
                    ARG_UPDATED_AT to item.updatedAtMillis,
                    ARG_LIST_NAME to item.listName,
                    ARG_IMAGE_URL to item.imageUrl,
                    ARG_NOTES to item.notes,
                    ARG_LIKE_COUNT to item.likeCount,
                    ARG_COMMENT_COUNT to item.commentCount,
                    ARG_LIKED to item.likedByCurrentUser
                )
            }
        }
    }
}
