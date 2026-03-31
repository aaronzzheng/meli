package com.example.meli.ui.list

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.meli.databinding.FragmentListSortBottomSheetBinding
import com.example.meli.model.RankingSortOption
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ListSortBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentListSortBottomSheetBinding? = null
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
        _binding = FragmentListSortBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.sortHighToLowRow.setOnClickListener {
            publishSelection(RankingSortOption.HIGHEST_RATED)
        }
        binding.sortLowToHighRow.setOnClickListener {
            publishSelection(RankingSortOption.LOWEST_RATED)
        }
        binding.sortRecentRow.setOnClickListener {
            publishSelection(RankingSortOption.RECENT)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun publishSelection(option: RankingSortOption) {
        parentFragmentManager.setFragmentResult(
            RESULT_KEY,
            Bundle().apply { putString(ARG_SORT_OPTION, option.name) }
        )
        dismiss()
    }

    companion object {
        const val TAG = "list_sort_bottom_sheet"
        const val RESULT_KEY = "list_sort_result"
        const val ARG_SORT_OPTION = "sortOption"
    }
}
