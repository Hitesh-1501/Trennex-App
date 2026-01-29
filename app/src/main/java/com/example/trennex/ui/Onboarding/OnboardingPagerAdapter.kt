package com.example.trennex.ui.Onboarding

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.trennex.ui.Onboarding.model.OnboardingPage

class OnboardingPagerAdapter(
    fragment: Fragment,
    private val pages: List<OnboardingPage>
): FragmentStateAdapter(fragment){
    override fun createFragment(position: Int): Fragment {
        val page = pages[position]
        return OnboardingPageFragment.newInstance(
            page.imagesRes,
            page.title,
            page.description
        )
    }
    override fun getItemCount() = pages.size
}