package com.example.trennex.ui.product

import android.content.res.ColorStateList
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.size
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.trennex.R
import com.example.trennex.databinding.FragmentProductDetailBinding
import com.example.trennex.databinding.WishlistDialogBinding
import com.example.trennex.ui.product.adapter.ColorVariantAdapter
import com.example.trennex.ui.product.adapter.ProductImageAdapter
import com.example.trennex.ui.product.adapter.ReviewAdapter
import com.example.trennex.ui.product.adapter.VariantAdapter
import com.example.trennex.ui.product.model.ProductColorModel
import com.example.trennex.ui.product.model.ReviewModel
import com.example.trennex.ui.product.model.VariantModel
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.transition.Hold
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColorInt
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.trennex.data.model.ProductResponse
import com.example.trennex.viewmodel.ProductDetailViewModel
import kotlinx.coroutines.launch


class ProductDetailFragment : Fragment(R.layout.fragment_product_detail), WishListDialogBinding.WishlistActionListener, AddToCartSheet.AddToCartActionListener{
    private var _binding: FragmentProductDetailBinding? = null
    private val binding get()  = _binding!!
    private lateinit var imageAdapter: ProductImageAdapter
    private lateinit var colorAdapter: ColorVariantAdapter
    private lateinit var  variantAdapter : VariantAdapter
    private var currentImages:List<String> = emptyList()
    private var isSpecExpandable = false
    private var isReviewExpandable = false
    private var isWishlisted = false
    private var isCart = false

    private var currentBannerPosition = 0
    private var selectedColor = "Onyx Black"
    private var selectedVariant = "128GB + 8GB"
    private var selectedColorPosition = 0
    private var selectedVariantPosition = 0
    private var bannerMediator: TabLayoutMediator? = null


    private val viewModel : ProductDetailViewModel by viewModels()

    private val args: ProductDetailFragmentArgs by navArgs()

    private var baseTitle = ""
    private var baseDescription = ""

    val variants = listOf(
        VariantModel(1,"128GB + 8GB","₹40,999","₹75,000",true),
        VariantModel(2,"256GB + 8GB","₹45,999","₹79,000")
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentProductDetailBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val id = args.productId
        viewModel.fetchProductDetail(id)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED){
                viewModel.products.collect { response ->
                    response?.let {product ->
                        val price = product.price
                        val discount = product.discountPercentage
                        val mrp = price / (1.0 - discount / 100.0)
                        baseTitle = product.title
                        baseDescription = product.description
                        binding.tvTitle.text = product.title
                        binding.tvPrice.text = "₹$price"
                        binding.tvMrp.text = "₹${String.format("%.2f", mrp)}"
                        binding.tvDescription.text = product.description
                        binding.tvMrp.paintFlags =
                            binding.tvMrp.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                        val imageList = product.images
                        currentImages = imageList
                        imageAdapter.updateImages(imageList)
                        binding.productBanners.setCurrentItem(0,false)
                        attachBannerDots()
                        updateMainTitle()
                        setUpReviews(product)
                    }
                }
            }
        }


        setupImageBanner()
        binding.productBanners.offscreenPageLimit = 1
        binding.productBanners.setPageTransformer { page , position ->
            page.alpha = 0.5f +(1-kotlin.math.abs(position))
        }
        setupColorVariants()
        setUpVariants()
        setupwishList()
        setUpCart()

        binding.specheader.setOnClickListener {
            isSpecExpandable = !isSpecExpandable
            binding.specContent.visibility = if(isSpecExpandable) View.VISIBLE else View.GONE
            binding.imgSpecArrow.animate()
                .rotation(if (isSpecExpandable) 180f else 0f)
                .setDuration(200)
                .start()
        }

        binding.reviewheader.setOnClickListener {
            isReviewExpandable = !isReviewExpandable
            binding.reviewContent.visibility = if(isReviewExpandable) View.VISIBLE else View.GONE
            binding.imgReviewArrow.animate()
                .rotation(if (isReviewExpandable) 180f else 0f)
                .setDuration(200)
                .start()
        }

        binding.productBanners.registerOnPageChangeCallback(
            object: ViewPager2.OnPageChangeCallback(){
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    currentBannerPosition = position
                }
            }
        )

        findNavController().currentBackStackEntry
            ?.savedStateHandle
            ?.getLiveData<Int>("selected_position")
            ?.observe(viewLifecycleOwner){position ->
                currentBannerPosition = position
                binding.productBanners.post {
                    binding.productBanners.setCurrentItem(position,false)
                }
            }

        findNavController().currentBackStackEntry
            ?.savedStateHandle
            ?.getLiveData<String>("selected_color")
            ?.observe(viewLifecycleOwner){color ->
                selectedColor = color
//                val index = colorImageMap.keys.indexOf(color)
//                selectedColorPosition = index
                updateMainTitle()
                colorAdapter.setSelectedColorPosition(selectedColorPosition)
//                updateProductImages(color)
                binding.tvSelectedColor.text = color
            }

        findNavController().currentBackStackEntry
            ?.savedStateHandle
            ?.getLiveData<Int>("selected_variant_position")
            ?.observe(viewLifecycleOwner){variantPosition ->
                selectedVariantPosition = variantPosition
                variantAdapter.setSelectedVariantPosition(selectedVariantPosition)
                val currentVariant = variants[variantPosition]
                selectedVariant = currentVariant.variant
                binding.tvVariant.text = currentVariant.variant
                binding.tvPrice.text = currentVariant.Price
                binding.tvMrp.text = currentVariant.mrpPrice
                updateMainTitle()
            }

        binding.tvMrp.paintFlags = binding.tvMrp.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
    }

    private fun setUpCart(){
        binding.addToCart.setOnClickListener {
            if(!isCart) {
                isCart = true
                binding.addToCart.setImageResource(R.drawable.cart_filled)
                val addToCartSheet = AddToCartSheet()
                addToCartSheet.listener = this
                addToCartSheet.show(parentFragmentManager, "add_to_cart_sheet")
                Toast.makeText(requireContext(),"Added to cart", Toast.LENGTH_SHORT).show()
            }else{
                findNavController().navigate(R.id.action_productDetailFragment_to_cartFragment)
            }
        }
    }
    override fun gotoCart() {
        findNavController().navigate(R.id.action_productDetailFragment_to_cartFragment)
    }
    private fun setupwishList(){
        binding.wishlist.setOnClickListener {
            if(!isWishlisted){
                isWishlisted = true
                binding.wishlist.setImageResource(R.drawable.wishlist_filled)
                Toast.makeText(requireContext(),"Added to Wishlist", Toast.LENGTH_SHORT).show()
            }else{
                val location = IntArray(2)
                binding.wishlist.getLocationOnScreen(location)
                val dialog = WishListDialogBinding.newInstance(
                    location[0],
                    location[1]
                )
                dialog.listener = this
                dialog.show(parentFragmentManager,"wishlist_dialog")
            }
        }
    }

    override fun removeFromWishlist() {
        isWishlisted = false
        binding.wishlist.setImageResource(R.drawable.wishlist_product)
        Toast.makeText(requireContext(), "Removed from Wishlist", Toast.LENGTH_SHORT).show()
    }

    override fun goToWishlist() {
        findNavController().navigate(R.id.action_productDetailFragment_to_wishlistFragment)
    }


    private fun setupImageBanner() {
        currentImages = emptyList()
        imageAdapter = ProductImageAdapter(currentImages) { clickedImg, _ ->
            openFullScreen(clickedImg, currentBannerPosition, currentImages)
        }
        binding.productBanners.adapter = imageAdapter
        binding.productBanners.setCurrentItem(currentBannerPosition,false)

        attachBannerDots()
    }
    private fun attachBannerDots(){
        bannerMediator?.detach()
        bannerMediator = TabLayoutMediator(binding.bannerIndicator, binding.productBanners) { tab, position -> }
        bannerMediator?.attach()
        for(i in 0 until binding.bannerIndicator.tabCount){
            val tab = binding.bannerIndicator.getTabAt(i)
            tab?.customView =
                layoutInflater.inflate(R.layout.product_banner_dot, binding.bannerIndicator, false)
        }
    }

    private fun setupColorVariants(){
        val variants = listOf(
            ProductColorModel(1,R.drawable.samsung_mobile,"Onyx Black",true),
            ProductColorModel(2,R.drawable.amber_yellow,"Amber Yellow"),
            ProductColorModel(2,R.drawable.cobalt_violet,"Cobalt Violet"),
            ProductColorModel(2,R.drawable.marble_gray,"Marble Gray"),

        )
        colorAdapter = ColorVariantAdapter(colorVariantList = variants,{
            binding.tvSelectedColor.text = it }, {selected ,position->
            selectedColor = selected.modelName
            selectedColorPosition = position
            updateMainTitle()

        })
        binding.rvColorVariants.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL,false)
            adapter = colorAdapter
        }
        colorAdapter.setSelectedColorPosition(selectedColorPosition)
    }

    private fun openFullScreen(imageView: ImageView, position : Int,images: List<String>){
        exitTransition = Hold().apply {
            duration = 300L
        }
        reenterTransition = Hold().apply {
            duration = 300L
        }

        val extras = FragmentNavigatorExtras(
            imageView to "product_image"
        )
        val bundle = bundleOf(
            "start_position" to position,
            "images" to images.toTypedArray(),
            "selected_color" to selectedColor,
            "selected_variant_pos" to selectedVariantPosition
        )
        findNavController()
            .navigate(R.id.action_productDetailFragment_to_imagePreviewFragment,
                bundle,
                null,
                extras)
    }
    private fun setUpReviews(product : ProductResponse){
        val reviews = product.reviews
        val reviewsList = reviews.map {
            ReviewModel(
                it.ratings.toFloat(),
                it.date,
                it.comment,
                it.reviewerName
            )
        }
        binding.tvReviews.text = "${reviewsList.size} Reviews"
        val averageRatings = reviewsList.map { it.rating }.average()
        binding.totalRatings.text = String.format("%.1f", averageRatings)
        val limitedReviews = reviewsList.take(4)
        val adapter = ReviewAdapter(limitedReviews)
        binding.rvReviews.adapter = adapter
        var isExpandable = false
        binding.tvViewAll.setOnClickListener {
            if(!isExpandable){
                adapter.updateList(reviewsList)
                binding.tvViewAll.text = "Show less"
            }else{
                adapter.updateList(reviewsList.take(4))
                binding.tvViewAll.text = "Show ${reviewsList.size - 4} More "
            }
            isExpandable = !isExpandable
        }
        val backgroundColor = when{
            averageRatings >= 4.0 -> "#21AD60".toColorInt()
            averageRatings >= 3.0 -> "#FBC02D".toColorInt()
            else -> "#D32F2F".toColorInt()
        }
        binding.llReview.backgroundTintList = ColorStateList.valueOf(backgroundColor)
        binding.averageRatings.text = String.format("%.1f", averageRatings)
        binding.ratings.text = "| ${reviewsList.size}"
    }
    private fun setUpVariants(){
        variantAdapter = VariantAdapter(variants = variants,{
            selectedVariant = it
            binding.tvVariant.text = it
            updateMainTitle()
        },{ variant , position ->
            binding.tvVariant.text = variant.variant
            selectedVariantPosition = position
        })
        binding.rvVariants.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL,false)
            adapter = variantAdapter
        }
        variantAdapter.setSelectedVariantPosition(selectedVariantPosition)
    }

    private fun updateMainTitle(){
        val description = "$baseDescription($selectedColor, $selectedVariant)"
        binding.tvDescription.text = description
        binding.tvTitle.text = baseTitle
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }


}