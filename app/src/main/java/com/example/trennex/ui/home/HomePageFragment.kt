package com.example.trennex.ui.home

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import com.bumptech.glide.Glide
import com.example.trennex.R
import com.example.trennex.analytics.AppAnalytics
import com.example.trennex.analytics.AnalyticsEvent
import com.example.trennex.analytics.AnalyticsNames
import com.example.trennex.analytics.AnalyticsParams
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
        val banner = requireArguments().getString("banner").orEmpty()
        binding.pageBanner.setOnClickListener {
            AppAnalytics.get().track(AnalyticsEvent(AnalyticsNames.SELECT_PROMOTION,
                mapOf(AnalyticsParams.PROMOTION_ID to
                    "home_banner_${requireArguments().getInt("banner_id")}")))
        }
        Glide.with(this)
            .load(banner)
            .placeholder(R.drawable.banner_drawable)
            .error(R.drawable.banner_drawable)
            .into(binding.pageBanner)
    }

    companion object {
        fun newInstance(banner: String, bannerId: Int): HomePageFragment {
            val fragment = HomePageFragment()
            fragment.arguments = bundleOf(
                "banner" to banner,
                "banner_id" to bannerId
            )
            return fragment
        }
    }
}
