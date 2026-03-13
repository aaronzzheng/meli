package com.example.meli.ui.list

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.meli.databinding.FragmentListBinding
import com.example.meli.ui.viewmodel.ListViewModel

private const val TAG = "ListLifecycle"

class ListFragment : Fragment() {

    private var _binding: FragmentListBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "ListFragment onCreate")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "ListFragment onCreateView")
        val listViewModel = ViewModelProvider(this)[ListViewModel::class.java]

        _binding = FragmentListBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textList
        listViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        listViewModel.addResult.observe(viewLifecycleOwner) { message ->
            if (!message.isNullOrBlank()) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                if (message.startsWith("Added")) {
                    binding.inputListItem.text?.clear()
                }
                listViewModel.onAddResultConsumed()
            }
        }

        binding.buttonAddItem.setOnClickListener {
            listViewModel.addItem(binding.inputListItem.text?.toString().orEmpty())
        }

        binding.buttonUpdateLatest.setOnClickListener {
            listViewModel.updateLatestItem(binding.inputListItem.text?.toString().orEmpty())
        }

        binding.buttonDeleteLatest.setOnClickListener {
            listViewModel.deleteLatestItem()
        }
        return root
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "ListFragment onResume")
    }

    override fun onPause() {
        Log.d(TAG, "ListFragment onPause")
        super.onPause()
    }

    override fun onDestroyView() {
        Log.d(TAG, "ListFragment onDestroy")
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        Log.d(TAG, "ListFragment onDestroy")
        super.onDestroy()
    }
}
