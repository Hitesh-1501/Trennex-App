package com.example.trennex.ui.product

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.size
import com.example.trennex.R
import com.example.trennex.databinding.FragmentHomeBinding
import com.example.trennex.databinding.FragmentProductDetailBinding
import com.example.trennex.ui.product.adapter.ColorVariantAdapter
import com.example.trennex.ui.product.adapter.ProductImageAdapter
import com.example.trennex.ui.product.adapter.VariantAdapter
import com.example.trennex.ui.product.model.ProductColorModel
import com.example.trennex.ui.product.model.VariantModel
import com.google.android.material.tabs.TabLayoutMediator


class ProductDetailFragment : Fragment(R.layout.fragment_product_detail) {
    private var _binding: FragmentProductDetailBinding? = null
    private val binding get()  = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentProductDetailBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupImageBanner()
        setupColorVariants()
        setUpVariants()
    }

    private fun setupImageBanner() {
        val images = listOf<Int>(
            R.drawable.product_img,
            R.drawable.product_img_two,
            R.drawable.product_img_three,
            R.drawable.product_img_four,
        )
        binding.productBanners.apply {
            adapter = ProductImageAdapter(images)
        }
        TabLayoutMediator(binding.bannerIndicator, binding.productBanners) { tab, position -> }.attach()
        for(i in 0 until binding.bannerIndicator.tabCount){
            val tab = binding.bannerIndicator.getTabAt(i)
            tab?.customView =
                layoutInflater.inflate(R.layout.product_banner_dot, binding.bannerIndicator, false)
        }
    }

    private fun setupColorVariants(){
        val variants = listOf(
            ProductColorModel(1,R.drawable.samsung_mobile,"Onyx Black",true),
            ProductColorModel(2,R.drawable.samsung_mobile,"Amber Yellow"),
            ProductColorModel(2,R.drawable.samsung_mobile,"Cobalt Violet"),
            ProductColorModel(2,R.drawable.samsung_mobile,"Marble Gray"),

        )
        binding.rvColorVariants.apply {
            adapter = ColorVariantAdapter(colorVariantList = variants,{
                binding.tvSelectedColor.text = it }, {selected ->

            })
        }
    }

    private fun setUpVariants(){
        val variants = listOf(
            VariantModel(1,"128GB + 8GB","₹40,999",true),
            VariantModel(2,"256GB + 8GB","₹45,999")
        )
        binding.rvVariants.apply {
            adapter = VariantAdapter(variants = variants,{
                binding.tvVariant.text = it
            },{

            })
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}