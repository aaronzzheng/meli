package com.example.meli.ui.notification

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.meli.R
import com.example.meli.databinding.FragmentNotificationBinding

class NotificationFragment : Fragment(){
    private var _binding: FragmentNotificationBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val TAG = "NotificationLifecycle"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "NotificationFragment onCreate")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "NotificationFragment onCreateView")
        val settingsViewModel =
            ViewModelProvider(this)[NotificationViewModel::class.java]

        _binding = FragmentNotificationBinding.inflate(inflater, container, false)
        val root: View = binding.root

        settingsViewModel.text.observe(viewLifecycleOwner) {
            binding.textNotification.text = it
        }
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonCloseNotification.setOnClickListener {
            findNavController().navigateUp()
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