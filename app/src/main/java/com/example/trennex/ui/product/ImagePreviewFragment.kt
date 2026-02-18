package com.example.trennex.ui.product

import android.os.Bundle
import android.transition.Transition
import android.transition.TransitionInflater
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.example.trennex.R
import com.example.trennex.databinding.FragmentImagePreviewBinding
import com.example.trennex.databinding.FragmentProductDetailBinding
import com.example.trennex.ui.product.adapter.FullImageAdapter

class ImagePreviewFragment : Fragment(R.layout.fragment_image_preview) {
    private var _binding: FragmentImagePreviewBinding? = null
    private val binding get()  = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentImagePreviewBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedElementEnterTransition = TransitionInflater.from(requireContext())
            .inflateTransition(android.R.transition.move)

        sharedElementReturnTransition = TransitionInflater.from(requireContext())
                .inflateTransition(android.R.transition.move)

        val images = listOf(
            R.drawable.product_img,
            R.drawable.product_img_two,
            R.drawable.product_img_three,
            R.drawable.product_img_four
        )
        binding.imgPreview.adapter = FullImageAdapter(images)

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }
}