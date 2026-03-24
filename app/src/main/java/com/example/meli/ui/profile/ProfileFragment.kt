package com.example.meli.ui.profile

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.meli.R
import com.example.meli.data.repository.AuthRepository
import com.example.meli.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

private const val TAG = "ProfileLifecycle"

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()
    private val authRepository = AuthRepository()
    private val rankingAdapter = ProfileRankingAdapter()
    private val imagePicker = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri ?: return@registerForActivityResult
        uploadProfileImage(uri)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "ProfileFragment onCreateView")
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        binding.profileMenuButton.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_profile_to_settingsFragment)
        }
        binding.profileFriendCountText.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_profile_to_friendsFragment)
        }
        binding.profileAvatarImage.setOnClickListener {
            imagePicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
        binding.profileActivityRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.profileActivityRecyclerView.adapter = rankingAdapter
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindProfileHeader()
        loadProfileImageFromCloud()
        observeRankings()
        viewModel.loadRankings(FirebaseAuth.getInstance().currentUser?.uid)
    }

    private fun observeRankings() {
        viewModel.activities.observe(viewLifecycleOwner) { activities ->
            rankingAdapter.submitList(activities)
            val showEmpty = activities.isEmpty() && viewModel.isLoading.value != true
            binding.profileActivityEmptyText.visibility = if (showEmpty) View.VISIBLE else View.GONE
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            val showEmpty = !loading && rankingAdapter.currentList.isEmpty()
            binding.profileActivityEmptyText.visibility = if (showEmpty) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (!error.isNullOrBlank()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun bindProfileHeader() {
        val user = FirebaseAuth.getInstance().currentUser
        val displayName = user?.displayName?.takeIf { it.isNotBlank() } ?: "Name"
        val emailPrefix = user?.email?.substringBefore("@")?.takeIf { it.isNotBlank() } ?: "username"
        binding.profileNameText.text = displayName
        binding.profileUsernameText.text = "@$emailPrefix"
    }

    private fun uploadProfileImage(uri: Uri) {
        _binding?.profileAvatarImage?.setImageURI(uri)
        _binding?.profileAvatarImage?.isEnabled = false
        _binding?.profileAvatarImage?.alpha = 0.65f

        lifecycleScope.launch {
            if (!isAdded) return@launch
            authRepository.uploadProfilePhoto(uri).onSuccess {
                Toast.makeText(requireContext(), "Profile picture updated", Toast.LENGTH_SHORT).show()
                loadProfileImageFromCloud()
            }.onFailure { error ->
                Toast.makeText(
                    requireContext(),
                    error.localizedMessage ?: "Failed to upload profile picture",
                    Toast.LENGTH_LONG
                ).show()
                loadProfileImageFromCloud()
            }
            _binding?.profileAvatarImage?.isEnabled = true
            _binding?.profileAvatarImage?.alpha = 1f
        }
    }

    private fun loadProfileImageFromCloud() {
        lifecycleScope.launch {
            if (!isAdded) return@launch
            authRepository.getProfilePhotoBytes().onSuccess { bytes ->
                if (bytes == null) {
                    _binding?.profileAvatarImage?.setImageResource(R.drawable.ic_profile_black_24dp)
                    return@onSuccess
                }
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bitmap != null) {
                    _binding?.profileAvatarImage?.setImageBitmap(bitmap)
                } else {
                    _binding?.profileAvatarImage?.setImageResource(R.drawable.ic_profile_black_24dp)
                }
            }.onFailure {
                _binding?.profileAvatarImage?.setImageResource(R.drawable.ic_profile_black_24dp)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "ProfileFragment onResume")
        bindProfileHeader()
        loadProfileImageFromCloud()
        viewModel.loadRankings(FirebaseAuth.getInstance().currentUser?.uid)
    }

    override fun onPause() {
        Log.d(TAG, "ProfileFragment onPause")
        super.onPause()
    }

    override fun onDestroyView() {
        Log.d(TAG, "ProfileFragment onDestroy")
        super.onDestroyView()
        binding.profileActivityRecyclerView.adapter = null
        _binding = null
    }

    override fun onDestroy() {
        Log.d(TAG, "ProfileFragment onDestroy")
        super.onDestroy()
    }
}
