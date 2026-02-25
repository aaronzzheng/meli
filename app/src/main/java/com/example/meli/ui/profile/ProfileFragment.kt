package com.example.meli.ui.profile

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.meli.databinding.FragmentProfileBinding

private val TAG = "SearchLifecycle"

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "ProfileFragment onCreate")
        val profileViewModel =
            ViewModelProvider(this).get(ProfileViewModel::class.java)

        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        val root: View = binding.root

        profileViewModel.text.observe(viewLifecycleOwner) {
            binding.textProfile.text = it
        }

        return root
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "ProfileFragment onResume")
    }

    override fun onPause() {
        Log.d(TAG, "ProfileFragment onPause")
        super.onPause()
    }

    override fun onDestroyView() {
        Log.d(TAG, "ProfileFragment onDestroy")
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        Log.d(TAG, "ProfileFragment onDestroy")
        super.onDestroy()
    }
}
