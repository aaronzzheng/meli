package com.example.meli.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import coil.load
import com.example.meli.R
import com.example.meli.databinding.FragmentTrackDetailBinding

class TrackDetailFragment : Fragment() {

    private var _binding: FragmentTrackDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrackDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonBackTrackDetail.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.trackDetailCoverImage.load(arguments?.getString(ARG_TRACK_IMAGE_URL)) {
            crossfade(true)
            placeholder(R.drawable.bg_profile_album_placeholder)
            error(R.drawable.bg_profile_album_placeholder)
        }
        binding.trackDetailTitleText.text = arguments?.getString(ARG_TRACK_NAME).orEmpty()
        binding.trackDetailArtistText.text = arguments?.getString(ARG_TRACK_ARTISTS).orEmpty()
        binding.trackDetailAlbumText.text = arguments?.getString(ARG_TRACK_ALBUM).orEmpty()
        binding.trackDetailRankButton.setOnClickListener {
            Toast.makeText(requireContext(), "Ranking integration coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
