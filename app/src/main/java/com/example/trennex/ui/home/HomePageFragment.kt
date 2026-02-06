package com.example.trennex.ui.home

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import com.example.trennex.R
import com.example.trennex.databinding.FragmentHomePageBinding

class HomePageFragment : Fragment(R.layout.fragment_home_page) {
    private var _binding : FragmentHomePageBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomePageBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val banner = requireArguments().getInt("banner")
        binding.pageBanner.setImageResource(banner)
    }

    companion object {
        fun newInstance(banner: Int): HomePageFragment {
            val fragment = HomePageFragment()
            fragment.arguments = bundleOf(
                "banner" to banner
            )
            return fragment
        }
    }
}