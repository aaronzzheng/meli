package com.example.meli.ui.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.meli.R
import com.example.meli.databinding.FragmentListBinding
import com.example.meli.model.RankingSortOption
import com.example.meli.ui.search.ARG_TRACK_ALBUM
import com.example.meli.ui.search.ARG_TRACK_ARTISTS
import com.example.meli.ui.search.ARG_TRACK_ID
import com.example.meli.ui.search.ARG_TRACK_IMAGE_URL
import com.example.meli.ui.search.ARG_TRACK_NAME
import com.example.meli.ui.search.TrackRatingDialogFragment
import com.example.meli.ui.viewmodel.ListViewModel

class ListFragment : Fragment() {

    private var _binding: FragmentListBinding? = null
    private val binding get() = _binding!!
    private lateinit var listViewModel: ListViewModel
    private lateinit var ratingAdapter: TrackRatingAdapter
    private var shouldScrollToTopAfterSort = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        listViewModel = ViewModelProvider(this)[ListViewModel::class.java]
        ratingAdapter = TrackRatingAdapter(onTrackSelected = { rating ->
            findNavController().navigate(
                R.id.trackDetailFragment,
                Bundle().apply {
                    putString(ARG_TRACK_ID, rating.trackId)
                    putString(ARG_TRACK_NAME, rating.trackTitle)
                    putString(ARG_TRACK_ARTISTS, rating.artistText)
                    putString(ARG_TRACK_ALBUM, rating.albumTitle.orEmpty())
                    putString(ARG_TRACK_IMAGE_URL, rating.imageUrl.orEmpty())
                }
            )
        })

        _binding = FragmentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.listRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.listRecyclerView.adapter = ratingAdapter
        setFragmentResultListener(TrackRatingDialogFragment.RESULT_KEY) { _, _ ->
            listViewModel.refresh()
        }
        setFragmentResultListener(ListSortBottomSheetFragment.RESULT_KEY) { _, bundle ->
            val sortName = bundle.getString(ListSortBottomSheetFragment.ARG_SORT_OPTION).orEmpty()
            RankingSortOption.entries.firstOrNull { it.name == sortName }?.let { sortOption ->
                shouldScrollToTopAfterSort = true
                listViewModel.setSortOption(sortOption)
            }
        }

        binding.sortTriggerButton.setOnClickListener {
            ListSortBottomSheetFragment()
                .show(parentFragmentManager, ListSortBottomSheetFragment.TAG)
        }

        listViewModel.ratings.observe(viewLifecycleOwner) { ratings ->
            ratingAdapter.submitList(ratings) {
                if (shouldScrollToTopAfterSort) {
                    binding.listRecyclerView.scrollToPosition(0)
                    shouldScrollToTopAfterSort = false
                }
            }
            binding.emptyListText.visibility = if (ratings.isEmpty()) View.VISIBLE else View.GONE
        }

        listViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.listLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        listViewModel.sortOption.observe(viewLifecycleOwner) { sortOption ->
            binding.sortTriggerButton.text = when (sortOption) {
                RankingSortOption.HIGHEST_RATED -> "High-Low"
                RankingSortOption.LOWEST_RATED -> "Low-High"
                RankingSortOption.RECENT -> "Recent"
            }
        }

        listViewModel.message.observe(viewLifecycleOwner) { message ->
            if (!message.isNullOrBlank()) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                listViewModel.consumeMessage()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        listViewModel.refresh()
    }
}
