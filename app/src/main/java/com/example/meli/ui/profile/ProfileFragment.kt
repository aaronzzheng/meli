package com.example.meli.ui.profile

import android.graphics.Bitmap
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
import com.example.meli.model.FriendshipStatus
import com.example.meli.model.UserProfileSummary
import com.google.firebase.auth.FirebaseAuth
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlinx.coroutines.launch

private const val TAG = "ProfileLifecycle"
private const val ARG_PROFILE_UID = "profileUid"

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
    private val currentUid get() = FirebaseAuth.getInstance().currentUser?.uid
    private val viewedUid get() = arguments?.getString(ARG_PROFILE_UID) ?: currentUid
    private val isOwnProfile get() = viewedUid == currentUid

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "ProfileFragment onCreateView")
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        binding.profileMenuButton.setOnClickListener {
            if (isOwnProfile) {
                findNavController().navigate(R.id.action_navigation_profile_to_settingsFragment)
            } else {
                findNavController().navigateUp()
            }
        }
        binding.profileFriendCountText.setOnClickListener {
            if (isOwnProfile) {
                findNavController().navigate(R.id.action_navigation_profile_to_friendsFragment)
            }
        }
        binding.profileAvatarImage.setOnClickListener {
            if (isOwnProfile) {
                imagePicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
        }
        binding.profileAddFriendButton.setOnClickListener {
            when (viewModel.profile.value?.friendshipStatus) {
                FriendshipStatus.REQUESTED -> viewModel.cancelFriendRequest(currentUid, viewedUid)
                FriendshipStatus.NONE -> viewModel.sendFriendRequest(currentUid, viewedUid)
                else -> Unit
            }
        }
        binding.profileActivityRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.profileActivityRecyclerView.adapter = rankingAdapter
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeRankings()
        viewModel.loadProfile(viewedUid, currentUid)
        loadProfileImageFromCloud(viewedUid)
        viewModel.loadRankings(viewedUid, allowSeed = isOwnProfile)
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

        viewModel.profile.observe(viewLifecycleOwner) { profile ->
            bindProfileHeader(profile)
        }

        viewModel.friendActionMessage.observe(viewLifecycleOwner) { message ->
            if (!message.isNullOrBlank()) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                viewModel.clearFriendActionMessage()
            }
        }
    }

    private fun bindProfileHeader(profile: UserProfileSummary?) {
        val summary = profile ?: return
        binding.profileNameText.text = summary.displayName
        binding.profileUsernameText.text = "@${summary.username.ifBlank { summary.email.substringBefore("@") }}"
        binding.profileFriendCountText.text = "${summary.friendCount} friends"
        binding.profileActivityHeaderText.text = if (isOwnProfile) "Your Activity" else "Recent Activity"
        binding.profileAddFriendButton.visibility = if (isOwnProfile) View.GONE else View.VISIBLE
        binding.profileAddFriendButton.text = friendButtonLabel(summary.friendshipStatus)
        binding.profileAddFriendButton.isEnabled =
            summary.friendshipStatus == FriendshipStatus.NONE ||
                summary.friendshipStatus == FriendshipStatus.REQUESTED
        binding.profileMenuButton.setImageResource(
            if (isOwnProfile) R.drawable.menu_24dp_000000_fill0_wght400_grad0_opsz24
            else android.R.drawable.ic_media_previous
        )
        binding.profileAvatarImage.isEnabled = isOwnProfile
        binding.profileFriendCountText.isClickable = isOwnProfile
        binding.profileFriendCountText.isFocusable = isOwnProfile
    }

    private fun uploadProfileImage(uri: Uri) {
        _binding?.profileAvatarImage?.setImageURI(uri)
        _binding?.profileAvatarImage?.isEnabled = false
        _binding?.profileAvatarImage?.alpha = 0.65f

        lifecycleScope.launch {
            if (!isAdded) return@launch
            val compressed = readCompressedImageBytes(uri)
            if (compressed == null) {
                Toast.makeText(requireContext(), "Could not read selected image", Toast.LENGTH_LONG).show()
                _binding?.profileAvatarImage?.isEnabled = true
                _binding?.profileAvatarImage?.alpha = 1f
                loadProfileImageFromCloud(viewedUid)
                return@launch
            }

            authRepository.uploadProfilePhoto(compressed).onSuccess {
                Toast.makeText(requireContext(), "Profile picture updated", Toast.LENGTH_SHORT).show()
                loadProfileImageFromCloud(viewedUid)
            }.onFailure { error ->
                Toast.makeText(
                    requireContext(),
                    error.localizedMessage ?: "Failed to upload profile picture",
                    Toast.LENGTH_LONG
                ).show()
                loadProfileImageFromCloud(viewedUid)
            }
            _binding?.profileAvatarImage?.isEnabled = true
            _binding?.profileAvatarImage?.alpha = 1f
        }
    }

    private fun readCompressedImageBytes(uri: Uri): ByteArray? {
        val resolver = requireContext().contentResolver

        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOptions)
        } ?: return null

        val maxDim = max(boundsOptions.outWidth, boundsOptions.outHeight)
        if (maxDim <= 0) return null

        var sampleSize = 1
        while (maxDim / sampleSize > 768) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val decodedBitmap = resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        } ?: return null

        val scaledBitmap = if (max(decodedBitmap.width, decodedBitmap.height) > 512) {
            val ratio = 512f / max(decodedBitmap.width, decodedBitmap.height).toFloat()
            val targetW = (decodedBitmap.width * ratio).toInt().coerceAtLeast(1)
            val targetH = (decodedBitmap.height * ratio).toInt().coerceAtLeast(1)
            android.graphics.Bitmap.createScaledBitmap(decodedBitmap, targetW, targetH, true)
        } else {
            decodedBitmap
        }

        val output = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 72, output)
        return output.toByteArray()
    }

    private fun loadProfileImageFromCloud(uid: String?) {
        lifecycleScope.launch {
            if (!isAdded) return@launch
            authRepository.getProfilePhotoBytes(uid).onSuccess { bytes ->
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
        viewModel.loadProfile(viewedUid, currentUid)
        loadProfileImageFromCloud(viewedUid)
        viewModel.loadRankings(viewedUid, allowSeed = isOwnProfile)
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

    private fun friendButtonLabel(status: FriendshipStatus): String {
        return when (status) {
            FriendshipStatus.NONE -> "Add Friend"
            FriendshipStatus.REQUESTED -> "Requested"
            FriendshipStatus.RECEIVED -> "Requested You"
            FriendshipStatus.FRIENDS -> "Friends"
            FriendshipStatus.SELF -> ""
        }
    }
}
