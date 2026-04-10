package com.example.trennex.ui.product

import android.os.Bundle
import androidx.transition.TransitionInflater
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.transition.ChangeBounds
import androidx.transition.ChangeImageTransform
import androidx.transition.ChangeTransform
import androidx.transition.TransitionSet
import androidx.viewpager2.widget.ViewPager2
import com.example.trennex.R
import com.example.trennex.databinding.FragmentImagePreviewBinding
import com.example.trennex.ui.product.adapter.FullImageAdapter

class ImagePreviewFragment : Fragment(R.layout.fragment_image_preview) {
    private var _binding: FragmentImagePreviewBinding? = null
    private val binding get()  = _binding!!
    private var currentPosition = 0
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentImagePreviewBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        postponeEnterTransition()
        super.onViewCreated(view, savedInstanceState)

        binding.imgPreview.post {
            startPostponedEnterTransition()
        }

        sharedElementEnterTransition = TransitionSet().apply {
            addTransition(ChangeBounds())
            duration = 300L
            addTransition(ChangeTransform())
            addTransition(ChangeImageTransform())
        }

        sharedElementReturnTransition = TransitionSet().apply {
            addTransition(ChangeBounds())
            duration = 300L
            addTransition(ChangeTransform())
            addTransition(ChangeImageTransform())
        }

        val startPosition = arguments?.getInt("start_position") ?:0
        val images = arguments?.getStringArray("images")?.toList() ?: emptyList()

        binding.imgPreview.adapter = FullImageAdapter(images,startPosition)
        binding.imgPreview.setCurrentItem(startPosition,false)
        currentPosition = startPosition

        binding.imgPreview.registerOnPageChangeCallback(
         object : ViewPager2.OnPageChangeCallback() {
             override fun onPageSelected(position: Int) {
                 super.onPageSelected(position)
                 currentPosition = position
                }
            }
         )

        binding.btnBack.setOnClickListener {
            findNavController().previousBackStackEntry
                ?.savedStateHandle
                ?.set("selected_position",currentPosition)

            findNavController().previousBackStackEntry
                ?.savedStateHandle
                ?.set("selected_color", arguments?.getString("selected_color"))

            findNavController().previousBackStackEntry
                ?.savedStateHandle
                ?.set("selected_color_position", arguments?.getInt("selected_color_position")?: 0)

            findNavController().previousBackStackEntry
                ?.savedStateHandle
                ?.set("selected_variant_position", arguments?.getInt("selected_variant_pos")?:0)

            findNavController().popBackStack()
        }
    }
}