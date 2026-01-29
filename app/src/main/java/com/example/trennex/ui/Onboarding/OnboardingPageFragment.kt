package com.example.trennex.ui.Onboarding

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import com.example.trennex.R
import com.example.trennex.databinding.FragmentOnboardingPageBinding
import com.example.trennex.databinding.FragmentSplashBinding

class OnboardingPageFragment : Fragment(R.layout.fragment_onboarding_page) {
    private var _binding: FragmentOnboardingPageBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentOnboardingPageBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val image = requireArguments().getInt("image")
        val title = requireArguments().getString("title")
        val desc = requireArguments().getString("desc")

        binding.onboardingImg.setImageResource(image)
        binding.txTitle.text = title
        binding.txtDesc.text = desc
    }
    companion object{
        fun newInstance(image: Int, title: String, desc: String) : OnboardingPageFragment{
            val fragment = OnboardingPageFragment()
            fragment.arguments = bundleOf(
                "image" to image,
                "title" to title,
                "desc" to desc
            )
            return fragment
        }
    }
}