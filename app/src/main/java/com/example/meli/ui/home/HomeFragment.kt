package com.example.meli.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.meli.databinding.FragmentHomeBinding
import androidx.navigation.fragment.findNavController
import com.example.meli.R

private const val TAG = "HomeLifecycle"

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "HomeFragment onCreate")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "HomeFragment onCreateView")
        val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textHome
        homeViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ** THIS IS THE CRITICAL STEP **
        // Assume your settings button in fragment_home.xml has the id "button_settings"
        binding.buttonSettings.setOnClickListener {
            // This line triggers the navigation action you defined in XML
            findNavController().navigate(R.id.navigation_home_to_settingsFragment)
        }
        binding.notificationButton.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_home_to_notificationFragment)
        }

    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "HomeFragment onResume")
    }

    override fun onPause() {
        Log.d(TAG, "HomeFragment onPause")
        super.onPause()
    }

    override fun onDestroyView() {
        Log.d(TAG, "HomeFragment onDestroy")
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        Log.d(TAG, "HomeFragment onDestroy")
        super.onDestroy()
    }
}