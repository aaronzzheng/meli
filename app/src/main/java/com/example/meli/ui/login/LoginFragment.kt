package com.example.meli.ui.login

import android.os.Bundle
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
                    binding.actionButton.text = "Sign In"
                    binding.toggleButton.text = "Create an account"
                }
                LoginViewModel.LoginState.CREATE_ACCOUNT -> {
                    binding.nameInputLayout.isVisible = true
                    binding.actionButton.text = "Create Account"
                    binding.toggleButton.text = "Already have an account? Sign In"
                }
            }
        }

        viewModel.navigateToSignIn.observe(viewLifecycleOwner) { navigate ->
            if (navigate) {
                Toast.makeText(context, "Account created successfully!", Toast.LENGTH_SHORT).show()
                viewModel.onNavigationConsumed()
            }
        }

        viewModel.navigateToHome.observe(viewLifecycleOwner) { navigate ->
            if (navigate) {
                findNavController().navigate(R.id.action_navigation_login_to_navigation_home)
                viewModel.onNavigationConsumed()
            }
        }

        binding.actionButton.setOnClickListener {
            viewModel.onActionButtonClicked()
        }

        binding.toggleButton.setOnClickListener {
            viewModel.toggleState()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}