package com.example.trennex.ui.home

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import com.example.trennex.R
import com.example.trennex.databinding.FragmentHomeBinding
import com.example.trennex.databinding.FragmentOnboardingBinding
import com.example.trennex.ui.home.adapters.BannerAdapter
import com.example.trennex.ui.home.adapters.CategoryAdapter
import com.example.trennex.ui.home.adapters.ProductAdapter
import com.example.trennex.ui.home.model.BannerModel
import com.example.trennex.ui.home.model.CategoryModel
import com.example.trennex.ui.home.model.ProductModel


class HomeFragment : Fragment(R.layout.fragment_home) {
    private var _binding: FragmentHomeBinding? = null
    private val binding get()  = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentHomeBinding.inflate(layoutInflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCategories()
        setupBanners()
        setupPopularProducts()
    }

    private fun setupCategories(){
        val categories = listOf<CategoryModel>(
            CategoryModel(R.drawable.for_you,"For you"),
            CategoryModel(R.drawable.for_you,"Fashion"),
            CategoryModel(R.drawable.for_you,"Electronics"),
            CategoryModel(R.drawable.for_you,"Mobiles"),
            CategoryModel(R.drawable.for_you,"Appliances"),
            CategoryModel(R.drawable.for_you,"Beauty"),
            CategoryModel(R.drawable.for_you,"Home"),
            CategoryModel(R.drawable.for_you,"Furniture"),
            CategoryModel(R.drawable.for_you,"Toys"),
            CategoryModel(R.drawable.for_you,"Sports"),
        )
        binding.rvCategories.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL,false)
            adapter = CategoryAdapter(categories)
        }
    }
    private fun setupBanners() {
        val Banners = listOf<BannerModel>(
            BannerModel(R.drawable.samsung_banner),
            BannerModel(R.drawable.samsung_banner),
            BannerModel(R.drawable.samsung_banner),
            BannerModel(R.drawable.samsung_banner),
        )
        val snapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(binding.rvBanners)
        binding.rvBanners.apply {
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = BannerAdapter(Banners)
        }
    }

    private fun setupPopularProducts(){
        val products = listOf<ProductModel>(
            ProductModel(R.drawable.product_image,"U.S Polo Jacket","2000"),
            ProductModel(R.drawable.product_image,"U.S Polo Jacket","2000"),
            ProductModel(R.drawable.product_image,"U.S Polo Jacket","2000"),
            ProductModel(R.drawable.product_image,"U.S Polo Jacket","2000"),
            ProductModel(R.drawable.product_image,"U.S Polo Jacket","2000"),
            ProductModel(R.drawable.product_image,"U.S Polo Jacket","2000"),
            ProductModel(R.drawable.product_image,"U.S Polo Jacket","2000")
        )
        binding.rvProducts.apply {
            layoutManager = GridLayoutManager(requireContext(),3)
            binding.rvProducts.isNestedScrollingEnabled = false
            adapter = ProductAdapter(products)
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}