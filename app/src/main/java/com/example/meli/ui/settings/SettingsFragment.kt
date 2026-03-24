package com.example.meli.ui.settings

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.meli.R
import com.example.meli.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val TAG = "SettingsLifecycle"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "SettingsFragment onCreate")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "SettingsFragment onCreateView")
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonCloseSettings.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.settingsOptionRow.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_settingsDetailFragment)
        }
        binding.faqOptionRow.setOnClickListener {
            Toast.makeText(requireContext(), "FAQ coming soon", Toast.LENGTH_SHORT).show()
        }
        binding.importOptionRow.setOnClickListener {
            Toast.makeText(requireContext(), "Import Songs coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "SettingsFragment onResume")
    }

    override fun onPause() {
        Log.d(TAG, "SettingsFragment onPause")
        super.onPause()
    }

    override fun onDestroyView() {
        Log.d(TAG, "SettingsFragment onDestroy")
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        Log.d(TAG, "SettingsFragment onDestroy")
        super.onDestroy()
    }
}
