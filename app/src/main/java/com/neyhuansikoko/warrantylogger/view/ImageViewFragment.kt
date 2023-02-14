package com.neyhuansikoko.warrantylogger.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.navigation.fragment.findNavController
import com.neyhuansikoko.warrantylogger.databinding.FragmentImageViewBinding

class ImageViewFragment : Fragment() {

    private var _binding: FragmentImageViewBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentImageViewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val imageUri = arguments?.getString("imageUri")

        if (imageUri.isNullOrBlank()) {
            findNavController().navigateUp()
        } else {
            binding.ivImageImage.setImageURI(imageUri.toUri())
        }
    }
}