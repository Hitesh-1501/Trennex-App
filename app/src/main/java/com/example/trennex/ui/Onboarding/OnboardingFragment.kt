package com.example.trennex.ui.Onboarding

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.example.trennex.R
import com.example.trennex.databinding.FragmentOnboardingBinding
import com.example.trennex.ui.Onboarding.model.OnboardingPage
import com.google.android.material.tabs.TabLayoutMediator

class OnboardingFragment : Fragment(R.layout.fragment_onboarding) {
    private var _binding: FragmentOnboardingBinding? = null
    private val binding get()  = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentOnboardingBinding.inflate(inflater,container,false)
        return binding.root
    }

     override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val window = activity?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.show(WindowInsetsCompat.Type.systemBars())
            controller.isAppearanceLightStatusBars = true
        }
        val pages = listOf<OnboardingPage>(
            OnboardingPage(R.drawable.onboarding1,getString(R.string.onboarding1_page_title), getString(R.string.onboarding1_page_description)),
            OnboardingPage(R.drawable.onboarding2,getString(R.string.onboarding2_page_title), getString(R.string.onboarding2_page_description)),
            OnboardingPage(R.drawable.onboarding3,getString(R.string.onboarding3_page_title), getString(R.string.onboarding3_page_description))
        )
        val adapter = OnboardingPagerAdapter(this,pages)
        binding.viewpager.adapter = adapter
        TabLayoutMediator(binding.tabLayout,binding.viewpager){_, _ -> }.attach()
        for (i in 0 until binding.tabLayout.tabCount) {
            val tab = binding.tabLayout.getTabAt(i)
            tab?.customView = layoutInflater.inflate(R.layout.dot_tab, binding.tabLayout,false)
        }
        binding.btnNext.visibility = View.INVISIBLE
        binding.btnNext.setOnClickListener {
            findNavController().navigate(R.id.action_onboardingFragment_to_loginFragment)
        }
        binding.viewpager.registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback(){
            override fun onPageSelected(position: Int) {
                if(position == pages.size-1){
                    binding.btnNext.visibility = View.VISIBLE
                    binding.btnNext.animate().alpha(1.0f).setDuration(500).start()
                }else{
                    binding.btnNext.visibility = View.INVISIBLE
                }
            }
        })
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}