package com.example.meli.ui.search

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import com.example.meli.databinding.FragmentTrackNotesBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class TrackNotesBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentTrackNotesBottomSheetBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            setCanceledOnTouchOutside(true)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrackNotesBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.trackNotesEditText.setText(arguments?.getString(ARG_NOTES).orEmpty())
        binding.trackNotesSaveButton.setOnClickListener {
            parentFragmentManager.setFragmentResult(
                RESULT_KEY,
                bundleOf(ARG_NOTES to binding.trackNotesEditText.text?.toString().orEmpty())
            )
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "track_notes_bottom_sheet"
        const val RESULT_KEY = "track_notes_result"
        const val ARG_NOTES = "notes"

        fun newInstance(notes: String): TrackNotesBottomSheetFragment {
            return TrackNotesBottomSheetFragment().apply {
                arguments = bundleOf(ARG_NOTES to notes)
            }
        }
    }
}
