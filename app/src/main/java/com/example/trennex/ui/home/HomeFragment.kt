package com.example.trennex.ui.home

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.example.trennex.R
import com.example.trennex.databinding.FragmentHomeBinding
import com.example.trennex.ui.home.adapters.CategoryAdapter
import com.example.trennex.ui.home.adapters.HomeFragmentPagerAdapter
import com.example.trennex.ui.home.adapters.ProductAdapter
import com.example.trennex.ui.home.model.BannerModel
import com.example.trennex.ui.home.model.CategoryModel
import com.example.trennex.ui.home.model.ProductModel
import com.example.trennex.viewmodel.ProductViewModel
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch


class HomeFragment : Fragment(R.layout.fragment_home) {
    private var _binding: FragmentHomeBinding? = null
    private val binding get()  = _binding!!
    private val viewModel:  ProductViewModel by viewModels()
    private var bannerMediator: TabLayoutMediator? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentHomeBinding.inflate(layoutInflater,container,false)
        return binding.root
    }

    private val bannerHandler = Handler(Looper.getMainLooper())
    private var bannerPageCallbacks: ViewPager2.OnPageChangeCallback? = null
    private val bannerRunnable = object : Runnable{
        override fun run() {
            val nextItem = binding.rvBanners.currentItem+1
            binding.rvBanners.setCurrentItem(nextItem,true)
            bannerHandler.postDelayed(this,3000)
        }

    }

    override fun onResume() {
        super.onResume()
        bannerHandler.postDelayed(bannerRunnable,3000)
    }

    override fun onPause() {
        super.onPause()
        bannerHandler.removeCallbacks(bannerRunnable)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.fetchProducts()
        viewModel.fetchCategories()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                launch {
                    viewModel.products.collect { apiList ->
                        val list = apiList.map {
                            ProductModel(
                                id = it.id,
                                image = it.thumbnail,
                                name  = it.title,
                                price = it.price
                            )
                        }
                        setupProducts(list)
                        setupBanners(apiList.map{ it.thumbnail }.filter { it.isNotBlank() })
                    }
                }
                launch {
                    viewModel.categories.collect {categoryList ->
                        val categories = buildList {
                                add(CategoryModel(id = 0, title = "All",slug = null))
                                addAll(categoryList.mapIndexed { index , category->
                                    CategoryModel(
                                        id = index + 1,
                                        title = category.name,
                                        slug = category.slug
                                    )
                                })

                        }
                        setupCategories(categories)
                    }
                }
            }
        }

    }

    private fun setupCategories(categories: List<CategoryModel>){
        binding.rvCategories.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL,false)
            adapter = CategoryAdapter(categories){
                Toast.makeText(context, "Selected: ${it.title}", Toast.LENGTH_SHORT).show()
                if(it.slug.isNullOrBlank()){
                    viewModel.fetchProducts()
                }else{
                    viewModel.fetchProductsByCategory(it.slug)
                }
            }
        }
    }

    private fun setupProducts(list: List<ProductModel>){
        binding.rvProducts.apply {
            layoutManager = GridLayoutManager(requireContext(),3)
            isNestedScrollingEnabled = false
            adapter = ProductAdapter(list) {product->
                val action = HomeFragmentDirections.actionHomeFragmentToProductDetailFragment(product.id)
                findNavController().navigate(action)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupBanners(images: List<String>) {
        bannerPageCallbacks?.let { binding.rvBanners.unregisterOnPageChangeCallback(it) }
        bannerMediator?.detach()

        if(images.isEmpty()){
            return
        }
        val banner = images.take(5).mapIndexed { index, image ->
            BannerModel(index + 1 , image)
        }

        if(banner.size == 1){
            val adapter = HomeFragmentPagerAdapter(this,banner)
            binding.rvBanners.adapter = adapter
            binding.bannerIndicator.removeAllTabs()
            return
        }

        val bannerList = mutableListOf<BannerModel>()
        bannerList.add(banner.last())
        bannerList.addAll(banner)
        bannerList.add(banner.first())
        val adapter = HomeFragmentPagerAdapter(this, bannerList)
        binding.rvBanners.adapter = adapter
        binding.rvBanners.setCurrentItem(1, false)
        bannerMediator =  TabLayoutMediator(binding.bannerIndicator, binding.rvBanners) { tab, position ->
            if (position == 0 || position == bannerList.size - 1) {
                tab.view.visibility = View.GONE
            }
        }
        bannerMediator?.attach()
        for (i in 0 until binding.bannerIndicator.tabCount) {
            val tab = binding.bannerIndicator.getTabAt(i)
            tab?.customView =
                layoutInflater.inflate(R.layout.banner_dot_tab, binding.bannerIndicator, false)
        }
        val callback = object : ViewPager2.OnPageChangeCallback() {

                override fun onPageScrollStateChanged(state: Int) {
                    if (state == ViewPager2.SCROLL_STATE_IDLE) {
                        val position = binding.rvBanners.currentItem

                        when (position) {
                            0 -> binding.rvBanners.setCurrentItem(
                                bannerList.size - 2,
                                false
                            )

                            bannerList.size - 1 -> binding.rvBanners.setCurrentItem(
                                1,
                                false
                            )
                        }
                    }
                }
            }
        bannerPageCallbacks = callback
        binding.rvBanners.registerOnPageChangeCallback(callback)
        binding.rvBanners.getChildAt(0)
                .setOnTouchListener { _,event ->
                    when(event.action){
                        MotionEvent.ACTION_DOWN -> bannerHandler.removeCallbacks(bannerRunnable)
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                            bannerHandler.postDelayed(bannerRunnable,3000)
                    }
                    false
                }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bannerPageCallbacks?.let { binding.rvBanners.unregisterOnPageChangeCallback(it) }
        bannerPageCallbacks = null
        bannerMediator?.detach()
        bannerMediator = null
        _binding = null
    }
}