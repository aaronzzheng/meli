package com.example.meli.ui.friends

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.meli.databinding.FragmentFriendsBinding
import com.google.firebase.auth.FirebaseAuth
import com.example.meli.R
import androidx.core.os.bundleOf

class FriendsFragment : Fragment() {

    private var _binding: FragmentFriendsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FriendsViewModel by viewModels()
    private val friendsAdapter = FriendsAdapter(
        onFriendClicked = { friend ->
            findNavController().navigate(
                R.id.action_friendsFragment_to_userProfileFragment,
                bundleOf("profileUid" to friend.uid)
            )
        },
        onUnfriendClicked = { friend ->
            viewModel.unfriend(FirebaseAuth.getInstance().currentUser?.uid, friend.uid)
        }
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFriendsBinding.inflate(inflater, container, false)
        binding.friendsRecyclerView.adapter = friendsAdapter
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeFriends()
        viewModel.loadFriends(FirebaseAuth.getInstance().currentUser?.uid)
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadFriends(FirebaseAuth.getInstance().currentUser?.uid)
    }

    private fun observeFriends() {
        viewModel.friends.observe(viewLifecycleOwner) { friends ->
            friendsAdapter.submitList(friends)
            binding.friendsPlaceholderText.visibility =
                if (friends.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (!error.isNullOrBlank()) {
                binding.friendsPlaceholderText.text = error
                binding.friendsPlaceholderText.visibility = View.VISIBLE
            }
        }

        viewModel.actionMessage.observe(viewLifecycleOwner) { message ->
            if (!message.isNullOrBlank()) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                viewModel.clearActionMessage()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.friendsRecyclerView.adapter = null
        _binding = null
    }
}
