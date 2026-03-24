package com.example.meli.ui.settings

import android.app.AlertDialog
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.meli.R
import com.example.meli.data.repository.AuthRepository
import com.example.meli.databinding.FragmentAccountSettingsBinding
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import kotlinx.coroutines.launch

class AccountSettingsFragment : Fragment() {

    private var _binding: FragmentAccountSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val authRepository = AuthRepository()
        val currentUser = authRepository.getCurrentUser()

        binding.nameInput.setText(currentUser?.displayName.orEmpty())
        binding.emailInput.setText(currentUser?.email.orEmpty())

        binding.buttonBackAccount.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.saveNameButton.setOnClickListener {
            val newName = binding.nameInput.text?.toString()?.trim().orEmpty()
            if (newName.isBlank()) {
                binding.nameInputLayout.error = "Name cannot be empty"
                return@setOnClickListener
            }
            binding.nameInputLayout.error = null
            runUpdate("Name updated") {
                authRepository.updateDisplayName(newName)
            }
        }

        binding.saveEmailButton.setOnClickListener {
            val newEmail = binding.emailInput.text?.toString()?.trim().orEmpty()
            if (!Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
                binding.emailInputLayout.error = "Enter a valid email"
                return@setOnClickListener
            }
            binding.emailInputLayout.error = null
            runUpdate("Email updated") {
                authRepository.updateEmail(newEmail)
            }
        }

        binding.savePasswordButton.setOnClickListener {
            val newPassword = binding.passwordInput.text?.toString().orEmpty()
            if (newPassword.length < 6) {
                binding.passwordInputLayout.error = "Password must be at least 6 characters"
                return@setOnClickListener
            }
            binding.passwordInputLayout.error = null
            runUpdate("Password updated") {
                authRepository.updatePassword(newPassword)
            }
            binding.passwordInput.setText("")
        }

        binding.deleteAccountButton.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Delete account?")
                .setMessage("This permanently deletes your account and profile data. This cannot be undone.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete") { _, _ ->
                    runDeleteAccount(authRepository)
                }
                .show()
        }
    }

    private fun runUpdate(
        successMessage: String,
        request: suspend () -> Result<Unit>
    ) {
        setLoading(true)
        viewLifecycleOwner.lifecycleScope.launch {
            request().onSuccess {
                Toast.makeText(requireContext(), successMessage, Toast.LENGTH_SHORT).show()
            }.onFailure { throwable ->
                val message = if (throwable is FirebaseAuthRecentLoginRequiredException) {
                    "Please log in again before changing this setting."
                } else {
                    throwable.localizedMessage ?: "Update failed"
                }
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
            }
            setLoading(false)
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.saveNameButton.isEnabled = !isLoading
        binding.saveEmailButton.isEnabled = !isLoading
        binding.savePasswordButton.isEnabled = !isLoading
        binding.deleteAccountButton.isEnabled = !isLoading
    }

    private fun runDeleteAccount(authRepository: AuthRepository) {
        setLoading(true)
        viewLifecycleOwner.lifecycleScope.launch {
            authRepository.deleteAccount()
                .onSuccess {
                    Toast.makeText(requireContext(), "Account deleted", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(
                        R.id.action_accountSettingsFragment_to_navigation_login
                    )
                }
                .onFailure { throwable ->
                    val message = if (throwable is FirebaseAuthRecentLoginRequiredException) {
                        "Please log in again before deleting your account."
                    } else {
                        throwable.localizedMessage ?: "Failed to delete account."
                    }
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                }
            setLoading(false)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
