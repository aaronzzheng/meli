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
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.meli.R
import com.example.meli.databinding.FragmentSearchBinding
import com.example.meli.model.FriendshipStatus
import com.example.meli.ui.home.HomeUserSearchAdapter
import com.google.firebase.auth.FirebaseAuth

private const val TAG = "SearchLifecycle"
private const val ARG_PROFILE_UID = "profileUid"

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SearchViewModel by viewModels()
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
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeSearch()

        binding.membersTab.setOnClickListener {
            viewModel.setMode(SearchViewModel.SearchMode.MEMBERS)
        }
        binding.songsTab.setOnClickListener {
            viewModel.setMode(SearchViewModel.SearchMode.SONGS)
        }
        binding.searchEditText.doAfterTextChanged { text ->
            viewModel.searchUsers(
                query = text?.toString().orEmpty(),
                currentUid = FirebaseAuth.getInstance().currentUser?.uid
            )
            renderSearchState()
        }

        viewModel.setMode(SearchViewModel.SearchMode.MEMBERS)
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

    private fun renderSearchState() {
        val query = binding.searchEditText.text?.toString().orEmpty().trim()
        val mode = viewModel.mode.value ?: SearchViewModel.SearchMode.MEMBERS
        val shouldShowResults = query.length >= 2
        val hasResults = viewModel.searchResults.value?.isNotEmpty() == true
        val isLoading = viewModel.searchLoading.value == true
        val isMembers = mode == SearchViewModel.SearchMode.MEMBERS

        binding.searchResultsRecyclerView.visibility =
            if (isMembers && shouldShowResults && hasResults) View.VISIBLE else View.GONE
        binding.searchEmptyText.visibility =
            if (isMembers && shouldShowResults && !hasResults && !isLoading) View.VISIBLE else View.GONE

        binding.songPlaceholderCard.visibility =
            if (!isMembers && shouldShowResults) View.VISIBLE else View.GONE
        binding.songPlaceholderText.text = if (query.length < 2) {
            "Search for songs once Spotify search is connected."
        } else {
            "Song results for \"$query\" will appear here once Spotify search is connected."
        }
    }

    override fun onDestroyView() {
        Log.d(TAG, "SearchFragment onDestroy")
        super.onDestroyView()
        binding.searchResultsRecyclerView.adapter = null
        _binding = null
    }

    override fun onDestroy() {
        Log.d(TAG, "SearchFragment onDestroy")
        super.onDestroy()
    }
}
