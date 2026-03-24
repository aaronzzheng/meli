package com.example.meli.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.meli.R
import com.example.meli.data.repository.AuthRepository
import com.example.meli.databinding.FragmentSettingsDetailBinding

class SettingsDetailFragment : Fragment() {

    private var _binding: FragmentSettingsDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val authRepository = AuthRepository()

        binding.buttonBackSettings.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.accountButton.setOnClickListener {
            findNavController().navigate(R.id.action_settingsDetailFragment_to_accountSettingsFragment)
        }

        binding.logoutButton.setOnClickListener {
            authRepository.logout()
            findNavController().navigate(R.id.action_settingsDetailFragment_to_navigation_login)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
