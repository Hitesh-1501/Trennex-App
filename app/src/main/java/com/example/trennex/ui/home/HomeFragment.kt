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
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentHomeBinding.inflate(layoutInflater,container,false)
        return binding.root
    }

    private val bannerHandler = Handler(Looper.getMainLooper())
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
        setupBanners()
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
                                price = it.price.toString()
                            )
                        }
                        binding.rvProducts.apply {
                            layoutManager = GridLayoutManager(requireContext(),3)
                            isNestedScrollingEnabled = false
                            adapter = ProductAdapter(list) {product->
                                val action = HomeFragmentDirections.actionHomeFragmentToProductDetailFragment(product.id)
                                findNavController().navigate(action)
                            }
                        }
                    }
                }
                launch {
                    viewModel.categories.collect {categoryList ->
                        val categories  = categoryList.mapIndexed { index, name ->
                            CategoryModel(
                                id = index,
                                title = name
                            )
                        }
                        setupCategories(categories)
                    }
                }
            }
        }

    }

    private fun setupCategories(categories: List<CategoryModel>){
//        val categories = listOf<CategoryModel>(
//            CategoryModel(1,R.drawable.for_you,"For you"),
//            CategoryModel(2,R.drawable.fashion_category,"Fashion"),
//            CategoryModel(3,R.drawable.electronics,"Electronics"),
//            CategoryModel(4,R.drawable.mobile,"Mobiles"),
//            CategoryModel(5,R.drawable.appliances,"Appliances"),
//            CategoryModel(6,R.drawable.beauty,"Beauty"),
//            CategoryModel(7,R.drawable.home_category,"Home"),
//            CategoryModel(8,R.drawable.furniture,"Furniture"),
//            CategoryModel(9,R.drawable.toys,"Toys"),
//            CategoryModel(10,R.drawable.sports,"Sports"),
//        )
        binding.rvCategories.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL,false)
            adapter = CategoryAdapter(categories){
                Toast.makeText(context, "Selected: ${it.title}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    @SuppressLint("ClickableViewAccessibility")
    private fun setupBanners() {
        val Banners = listOf<BannerModel>(
            BannerModel(1,R.drawable.samsung_banner),
            BannerModel(2,R.drawable.shoes_banner),
            BannerModel(3,R.drawable.samsung_banner),
            BannerModel(4,R.drawable.shoes_banner),
        )
        val bannerList = mutableListOf<BannerModel>()
        bannerList.add(Banners.last())
        bannerList.addAll(Banners)
        bannerList.add(Banners.first())
        val adapter = HomeFragmentPagerAdapter(this, bannerList)
        binding.rvBanners.adapter = adapter
        binding.rvBanners.setCurrentItem(1, false)
        TabLayoutMediator(binding.bannerIndicator, binding.rvBanners) { tab, position ->
            if (position == 0 || position == bannerList.size - 1) {
                tab.view.visibility = View.GONE
            }
        }.attach()
        for (i in 0 until binding.bannerIndicator.tabCount) {
            val tab = binding.bannerIndicator.getTabAt(i)
            tab?.customView =
                layoutInflater.inflate(R.layout.banner_dot_tab, binding.bannerIndicator, false)
        }
        binding.rvBanners.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {

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
        )
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

//    private fun setupPopularProducts(){
//        val products = listOf<ProductModel>(
//            ProductModel(1,R.drawable.product_image,"U.S Polo Jacket","2000"),
//            ProductModel(2,R.drawable.tshirt,"Mortex Blue Jacket","1000"),
//            ProductModel(3,R.drawable.printeed_tshirt,"Roadster printed..","600"),
//            ProductModel(4,R.drawable.samsung_mobile,"Samsung S24 onyx Black","40,999"),
//            ProductModel(5,R.drawable.tv,"Lg  Smart  Tv 55 inch ","90,000"),
//            ProductModel(6,R.drawable.watch,"Fastrack watch","800"),
//            ProductModel(7,R.drawable.laptop,"Asus A15 Laptop","55,000")
//        )
//        binding.rvProducts.apply {
//            layoutManager = GridLayoutManager(requireContext(),3)
//            binding.rvProducts.isNestedScrollingEnabled = false
//            adapter = ProductAdapter(products){
//                findNavController().navigate(R.id.action_homeFragment_to_productDetailFragment)
//            }
//        }
//    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}