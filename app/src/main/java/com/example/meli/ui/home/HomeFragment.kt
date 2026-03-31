package com.example.meli.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.meli.R
import com.example.meli.databinding.FragmentHomeBinding
import com.example.meli.model.ProfileRankingActivity
import com.example.meli.ui.feed.FeedCommentsBottomSheetDialogFragment
import com.example.meli.ui.profile.ProfileRankingAdapter
import com.google.firebase.auth.FirebaseAuth

private const val TAG = "HomeLifecycle"
private const val ARG_PROFILE_UID = "profileUid"

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val viewModel: HomeViewModel by viewModels()
    private val rankingAdapter = ProfileRankingAdapter(
        onLikeClicked = { activity ->
            viewModel.toggleLike(activity, FirebaseAuth.getInstance().currentUser?.uid)
        },
        onCommentClicked = { activity ->
            showCommentsDialog(activity)
        }
    )
    private val userSearchAdapter = HomeUserSearchAdapter(
        onUserClicked = { user ->
            findNavController().navigate(
                R.id.action_navigation_home_to_userProfileFragment,
                bundleOf(ARG_PROFILE_UID to user.uid)
            )
        },
        onAddFriendClicked = { user ->
            when (user.friendshipStatus) {
                com.example.meli.model.FriendshipStatus.REQUESTED ->
                    viewModel.cancelFriendRequest(user.uid, FirebaseAuth.getInstance().currentUser?.uid)
                com.example.meli.model.FriendshipStatus.NONE ->
                    viewModel.sendFriendRequest(user.uid, FirebaseAuth.getInstance().currentUser?.uid)
                else -> Unit
            }
        }
    )

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private var collapsibleSectionHeight = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "HomeFragment onCreate")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "HomeFragment onCreateView")
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        binding.homeFeedRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.homeFeedRecyclerView.adapter = rankingAdapter
        binding.homeSearchResultsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.homeSearchResultsRecyclerView.adapter = userSearchAdapter
        binding.homeCollapsibleSection.doOnLayout {
            collapsibleSectionHeight = it.height
            updateCollapsedHeader(binding.homeFeedRecyclerView.computeVerticalScrollOffset())
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeFeed()
        setFragmentResultListener(FeedCommentsBottomSheetDialogFragment.RESULT_KEY) { _, _ ->
            viewModel.loadFeed(FirebaseAuth.getInstance().currentUser?.uid)
        }
        binding.buttonSettings.setOnClickListener {
            findNavController().navigate(R.id.navigation_home_to_settingsFragment)
        }
        binding.notificationButton.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_home_to_notificationFragment)
        }
        binding.homeSearchEditText.doAfterTextChanged { text ->
            viewModel.searchUsers(
                query = text?.toString().orEmpty(),
                currentUid = FirebaseAuth.getInstance().currentUser?.uid
            )
        }
        binding.homeFeedRecyclerView.addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
                updateCollapsedHeader(recyclerView.computeVerticalScrollOffset())
            }
        })
        viewModel.loadFeed(FirebaseAuth.getInstance().currentUser?.uid)
    }

    private fun observeFeed() {
        viewModel.activities.observe(viewLifecycleOwner) { activities ->
            rankingAdapter.submitList(activities)
            val showEmpty = activities.isEmpty() && viewModel.isLoading.value != true
            binding.homeFeedEmptyText.visibility = if (showEmpty) View.VISIBLE else View.GONE
        }

        viewModel.notificationCount.observe(viewLifecycleOwner) { count ->
            binding.notificationBadgeText.visibility = if (count > 0) View.VISIBLE else View.GONE
            binding.notificationBadgeText.text = count.toString()
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            val showEmpty = !loading && viewModel.activities.value.isNullOrEmpty()
            binding.homeFeedEmptyText.visibility = if (showEmpty) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (!error.isNullOrBlank()) {
                binding.homeFeedEmptyText.text = error
                binding.homeFeedEmptyText.visibility = View.VISIBLE
            }
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
            if (!error.isNullOrBlank()) {
                binding.homeSearchEmptyText.text = error
            } else {
                binding.homeSearchEmptyText.text = "No users found."
            }
            renderSearchState()
        }

        viewModel.friendActionMessage.observe(viewLifecycleOwner) { message ->
            if (!message.isNullOrBlank()) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                viewModel.clearFriendActionMessage()
            }
        }
    }

    private fun renderSearchState() {
        val query = binding.homeSearchEditText.text?.toString().orEmpty().trim()
        val shouldShowDropdown = query.length >= 2
        val hasResults = viewModel.searchResults.value?.isNotEmpty() == true
        val isLoading = viewModel.searchLoading.value == true

        binding.homeSearchDropdownCard.visibility =
            if (shouldShowDropdown) View.VISIBLE else View.GONE
        binding.homeSearchResultsRecyclerView.visibility =
            if (shouldShowDropdown && hasResults) View.VISIBLE else View.GONE

        val emptyMessageVisible = shouldShowDropdown && !hasResults && !isLoading
        binding.homeSearchEmptyText.visibility =
            if (emptyMessageVisible) View.VISIBLE else View.GONE
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "HomeFragment onResume")
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        viewModel.loadFeed(uid)
        viewModel.refreshNotificationCount(uid)
    }

    override fun onPause() {
        Log.d(TAG, "HomeFragment onPause")
        super.onPause()
    }

    override fun onDestroyView() {
        Log.d(TAG, "HomeFragment onDestroy")
        super.onDestroyView()
        binding.homeSearchResultsRecyclerView.adapter = null
        binding.homeFeedRecyclerView.adapter = null
        _binding = null
    }

    private fun updateCollapsedHeader(scrollOffset: Int) {
        if (_binding == null || collapsibleSectionHeight == 0) return
        val collapse = scrollOffset.coerceIn(0, collapsibleSectionHeight)
        binding.homeCollapsibleSection.layoutParams = binding.homeCollapsibleSection.layoutParams.apply {
            height = (collapsibleSectionHeight - collapse).coerceAtLeast(0)
        }
        binding.homeCollapsibleSection.alpha =
            ((collapsibleSectionHeight - collapse).toFloat() / collapsibleSectionHeight).coerceIn(0f, 1f)
        binding.homeCollapsibleSection.requestLayout()
    }

    override fun onDestroy() {
        Log.d(TAG, "HomeFragment onDestroy")
        super.onDestroy()
    }

    private fun showCommentsDialog(activity: ProfileRankingActivity) {
        FeedCommentsBottomSheetDialogFragment
            .newInstance(activity)
            .show(childFragmentManager, "feed_comments")
    }
}
