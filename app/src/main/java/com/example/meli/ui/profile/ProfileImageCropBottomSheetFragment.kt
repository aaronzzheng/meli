package com.example.meli.ui.profile

import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.meli.databinding.FragmentProfileImageCropBinding
import java.io.ByteArrayOutputStream

class ProfileImageCropBottomSheetFragment : DialogFragment() {

    private var _binding: FragmentProfileImageCropBinding? = null
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
        _binding = FragmentProfileImageCropBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val uriString = requireArguments().getString(ARG_URI).orEmpty()
        val bitmap = decodeBitmap(Uri.parse(uriString))
        if (bitmap != null) {
            binding.profileImageCropView.setImageBitmap(bitmap)
        }

        binding.cropCancelButton.setOnClickListener { dismiss() }
        binding.cropSaveButton.setOnClickListener {
            val cropped = binding.profileImageCropView.exportCroppedBitmap() ?: return@setOnClickListener
            val bytes = ByteArrayOutputStream().use { output ->
                cropped.compress(Bitmap.CompressFormat.JPEG, 82, output)
                output.toByteArray()
            }
            parentFragmentManager.setFragmentResult(
                RESULT_KEY,
                bundleOf(ARG_IMAGE_BYTES to bytes)
            )
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun decodeBitmap(uri: Uri): Bitmap? {
        val resolver = requireContext().contentResolver
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(resolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val maxDim = maxOf(info.size.width, info.size.height)
                if (maxDim > 2048) {
                    val ratio = 2048f / maxDim.toFloat()
                    decoder.setTargetSize(
                        (info.size.width * ratio).toInt().coerceAtLeast(1),
                        (info.size.height * ratio).toInt().coerceAtLeast(1)
                    )
                }
            }
        } else {
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        }
    }

    companion object {
        const val TAG = "profile_image_crop"
        const val RESULT_KEY = "profile_image_crop_result"
        const val ARG_URI = "uri"
        const val ARG_IMAGE_BYTES = "imageBytes"

        fun newInstance(uri: Uri): ProfileImageCropBottomSheetFragment {
            return ProfileImageCropBottomSheetFragment().apply {
                arguments = bundleOf(ARG_URI to uri.toString())
            }
        }
    }
}
