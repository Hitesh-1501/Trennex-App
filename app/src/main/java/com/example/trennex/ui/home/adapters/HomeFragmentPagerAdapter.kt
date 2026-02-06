package com.example.trennex.ui.home.adapters

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.trennex.ui.home.HomePageFragment
import com.example.trennex.ui.home.model.BannerModel

class HomeFragmentPagerAdapter(
    fragment: Fragment,
    private val banners: List<BannerModel>
): FragmentStateAdapter(fragment){
    override fun createFragment(position: Int): Fragment {
        val page = banners[position]
        return HomePageFragment.newInstance(page.banner)
    }

    override fun getItemCount(): Int {
        return banners.size
    }
}