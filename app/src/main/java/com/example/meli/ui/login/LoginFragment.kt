package com.example.meli.ui.login

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.meli.R
import com.example.meli.databinding.FragmentLoginBinding

private const val TAG = "LoginFragment"

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                LoginViewModel.LoginState.SIGN_IN -> {
                    binding.nameInputLayout.isVisible = false
                    binding.createUsernameInputLayout.isVisible = false
                    binding.actionButton.text = "Sign In"
                    binding.usernameInputLayout.hint = "Enter email"
                    binding.signInTabUnderline.isVisible = true
                    binding.createAccountTabUnderline.isVisible = false
                    binding.signInTabText.alpha = 1f
                    binding.createAccountTabText.alpha = 0.55f
                }
                LoginViewModel.LoginState.CREATE_ACCOUNT -> {
                    binding.nameInputLayout.isVisible = true
                    binding.createUsernameInputLayout.isVisible = true
                    binding.actionButton.text = "Create Account"
                    binding.usernameInputLayout.hint = "Enter email"
                    binding.signInTabUnderline.isVisible = false
                    binding.createAccountTabUnderline.isVisible = true
                    binding.signInTabText.alpha = 0.55f
                    binding.createAccountTabText.alpha = 1f
                }
            }
        }

        viewModel.navigateToHome.observe(viewLifecycleOwner) { navigate ->
            if (navigate) {
                findNavController().navigate(R.id.action_navigation_login_to_navigation_home)
                viewModel.onNavigationConsumed()
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.actionButton.isEnabled = !isLoading
            binding.signInTabText.isEnabled = !isLoading
            binding.createAccountTabText.isEnabled = !isLoading
        }

        binding.actionButton.setOnClickListener {
            val email = binding.usernameEditText.text.toString()
            val pass = binding.passwordEditText.text.toString()
            val name = binding.nameEditText.text.toString()
            val username = binding.createUsernameEditText.text.toString()
            viewModel.onActionButtonClicked(email, pass, name, username)
        }

        binding.signInTabText.setOnClickListener {
            if (viewModel.state.value != LoginViewModel.LoginState.SIGN_IN) {
                viewModel.toggleState()
            }
        }

        binding.createAccountTabText.setOnClickListener {
            if (viewModel.state.value != LoginViewModel.LoginState.CREATE_ACCOUNT) {
                viewModel.toggleState()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
