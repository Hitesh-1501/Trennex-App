package com.example.trennex.ui.product

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.trennex.R
import com.example.trennex.databinding.FragmentProductDetailBinding
import com.example.trennex.ui.product.adapter.ColorVariantAdapter
import com.example.trennex.ui.product.adapter.ProductImageAdapter
import com.example.trennex.ui.product.adapter.ReviewAdapter
import com.example.trennex.ui.product.adapter.VariantAdapter
import com.example.trennex.ui.product.model.ProductColorModel
import com.example.trennex.ui.product.model.ReviewModel
import com.example.trennex.ui.product.model.VariantModel
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.transition.Hold
import androidx.core.graphics.toColorInt
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.widget.ViewPager2
import com.example.trennex.data.model.ProductResponse
import com.example.trennex.utils.cart.CartStore
import com.example.trennex.ui.cart.model.CartItemModel
import com.example.trennex.ui.product.model.SpecDetailAdapter
import com.example.trennex.ui.product.model.SpecDetailItem
import com.example.trennex.utils.wishlist.WishListStore
import com.example.trennex.ui.wishlist.model.WishlistItemsModel
import com.example.trennex.utils.CurrencyFormator
import com.example.trennex.viewmodel.product.ProductDetailViewModel
import kotlinx.coroutines.launch
import kotlin.math.abs


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
    private var selectedColor = ""
    private var selectedVariant = ""
    private var selectedColorPosition = 0
    private var selectedVariantPosition = 0
    private var bannerMediator: TabLayoutMediator? = null


    private val viewModel : ProductDetailViewModel by viewModels()

    private val args: ProductDetailFragmentArgs by navArgs()

    private var baseTitle = ""
    private var baseDescription = ""
    private lateinit var reviewAdapter: ReviewAdapter
    private lateinit var topFeaturesAdapter: SpecDetailAdapter

    private var currentVariants : List<VariantModel> = emptyList()
    private var currentColorOptions: List<ProductColorModel> = emptyList()



    private var cartProductId: Int = 0
    private var cartPrice: Double = 0.0
    private var cartMrp: Double = 0.0
    private var cartRating: Double = 0.0
    private var cartRatingCount: Int = 0
    private var cartReturnPolicy: String = ""
    private var cartDeliveryDetails: String = ""
    private var currentProduct: ProductResponse? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentProductDetailBinding.inflate(layoutInflater)
        savedInstanceState?.let {
            currentBannerPosition = it.getInt("banner_position",0)
            selectedColor = it.getString("selected_color", "")
            selectedVariant = it.getString("selected_variant", "")
            selectedColorPosition = it.getInt("selected_color_position", 0)
            selectedVariantPosition = it.getInt("selected_variant_position", 0)
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val id = args.productId
        viewModel.fetchProductDetail(id)
        reviewAdapter = ReviewAdapter(emptyList())
        binding.rvReviews.isNestedScrollingEnabled = false
        binding.rvReviews.layoutManager = LinearLayoutManager(requireContext())
        binding.rvReviews.adapter = reviewAdapter

        topFeaturesAdapter = SpecDetailAdapter()
        binding.rvTopFeatures.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTopFeatures.adapter = topFeaturesAdapter


        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED){
                viewModel.products.collect { response ->
                    response?.let {product ->
                        currentProduct = product
                        val price = product.price
                        val discount = product.discountPercentage
                        val mrp = price / (1.0 - discount / 100.0)
                        cartProductId = product.id
                        cartPrice = price
                        cartMrp = mrp
                        cartRatingCount = product.reviews.size
                        cartReturnPolicy = product.returnPolicy
                        cartDeliveryDetails = product.shippingInformation
                        baseTitle = product.title
                        baseDescription = product.description
                        binding.tvTitle.text = product.title
                        binding.tvPrice.text = CurrencyFormator.formatInr(price)
                        binding.tvMrp.text = CurrencyFormator.formatInr(mrp)
                        binding.tvDescription.text = product.description
                        binding.tvMrp.paintFlags =
                            binding.tvMrp.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                        val imageList = product.images
                        currentImages = imageList
                        imageAdapter.updateImages(imageList)
                        binding.productBanners.setCurrentItem(0,false)
                        binding.tvDelivery.text = product.shippingInformation
                        binding.tvPriceDetails.text = CurrencyFormator.formatInr(product.price)
                        binding.tvManufacture.text = product.warrantyInformation
                        binding.tvReturnPolicy.text = product.returnPolicy
                        attachBannerDots()
                        updateMainTitle()
                        bindTopFeatures(product)
                        setUpReviews(product)
                        configureVariantAndColorData(product,mrp)
                        setupColorVariants()
                        setUpVariants()
                    }
                }
            }
        }
        setupImageBanner()
        binding.productBanners.offscreenPageLimit = 1
        binding.productBanners.setPageTransformer { page , position ->
            page.alpha = 0.5f +(1- abs(position))
        }

        setupwishList()
        observeWishlistState()
        setUpCart()
        observeCartState()
        binding.btnShowMoreSpecs.setOnClickListener {
            val direction = ProductDetailFragmentDirections.actionProductDetailFragmentToProductSpecFragment(productId = args.productId)
            findNavController().navigate(direction)
        }

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
                selectedColor = color.orEmpty()
                updateMainTitle()
                colorAdapter.setSelectedColorPosition(selectedColorPosition)
                binding.tvSelectedColor.text = color
            }

        findNavController().currentBackStackEntry
            ?.savedStateHandle
            ?.getLiveData<Int>("selected_color_position")
            ?.observe(viewLifecycleOwner){position ->
                selectedColorPosition = position
                colorAdapter.setSelectedColorPosition(selectedColorPosition)
            }

        findNavController().currentBackStackEntry
            ?.savedStateHandle
            ?.getLiveData<Int>("selected_variant_position")
            ?.observe(viewLifecycleOwner){variantPosition ->
                selectedVariantPosition = variantPosition
                variantAdapter.setSelectedVariantPosition(selectedVariantPosition)
                val currentVariant = currentVariants.getOrNull(variantPosition)
                currentVariant?.let {
                    selectedVariant = it.variant
                    binding.tvVariant.text = it.variant
                    binding.tvPrice.text = it.Price
                    binding.tvMrp.text = it.mrpPrice
                }
                updateMainTitle()
            }

        binding.tvMrp.paintFlags = binding.tvMrp.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
    }

    private fun setupwishList(){
        binding.wishlist.setOnClickListener {
            val product = currentProduct
            if(product == null){
                Toast.makeText(requireContext(), "Product details not loaded yet", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if(!isWishlisted){
                WishListStore.addOrUpdate(
                    WishlistItemsModel(
                        id = product.id,
                        imageUrl = product.thumbnail,
                        title = product.title,
                        price = product.price
                    )
                )
                isWishlisted = true
                binding.wishlist.setImageResource(R.drawable.wishlist_filled)
                Toast.makeText(requireContext(),"Added to Wishlist", Toast.LENGTH_SHORT).show()
            }else{
                val location = IntArray(2)
                binding.wishlist.getLocationOnScreen(location)
                val dialog = WishListDialogBinding.newInstance(
                    location[0],
                    location[1],
                    currentImages.firstOrNull()
                )
                dialog.listener = this
                dialog.show(parentFragmentManager,"wishlist_dialog")
            }
        }
    }

    override fun removeFromWishlist() {
        currentProduct?.let {
            WishListStore.removeItem(it.id)
        }
        isWishlisted = false
        binding.wishlist.setImageResource(R.drawable.wishlist_product)
        Toast.makeText(requireContext(), "Removed from Wishlist", Toast.LENGTH_SHORT).show()
    }

    override fun goToWishlist() {
        findNavController().navigate(R.id.action_productDetailFragment_to_wishlistFragment)
    }

    private fun observeWishlistState(){
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                WishListStore.items.collect {items->
                    isWishlisted = items.any{it.id == args.productId}
                    binding.wishlist.setImageResource(
                        if(isWishlisted) R.drawable.wishlist_filled else R.drawable.wishlist_product
                    )
                }
            }
        }
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
        colorAdapter = ColorVariantAdapter(colorVariantList = currentColorOptions,{
            binding.tvSelectedColor.text = it }, {selected ,position->
            selectedColor = selected.modelName
            selectedColorPosition = position
            updateMainTitle()

        })
        binding.rvColorVariants.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL,false)
            adapter = colorAdapter
        }
        if(currentColorOptions.isNotEmpty()){
            colorAdapter.setSelectedColorPosition(selectedColorPosition)
        }
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
            "selected_color_position" to selectedColorPosition,
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
                it.rating.toFloat(),
                it.date,
                it.comment,
                it.reviewerName
            )
        }
        Log.d("REVIEW_CHECK", "Reviews size: ${product.reviews.size}")
        binding.tvRatings.text = "${reviewsList.size} Ratings"
        binding.tvReviews.text = "| ${reviewsList.size} Reviews"
        val averageRatings = if(reviewsList.isNotEmpty()){
            reviewsList.map { it.rating }.average()
        }else 0.0
        binding.totalRatings.text = String.format("%.1f", averageRatings)
        cartRating = averageRatings
        val limitedReviews = reviewsList.take(4)
        reviewAdapter.updateList(limitedReviews)

        if (reviewsList.size <= 4) {
            binding.tvViewAll.visibility = View.GONE
        } else {
            binding.tvViewAll.visibility = View.VISIBLE
            binding.tvViewAll.text = "Show ${reviewsList.size - 4} More"
        }
        var isExpandable = false
        binding.tvViewAll.setOnClickListener {
            if(!isExpandable){
                reviewAdapter.updateList(reviewsList)
                binding.tvViewAll.text = "Show less"
            }else{
                reviewAdapter.updateList(reviewsList.take(4))
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
        binding.totalRatings.backgroundTintList = ColorStateList.valueOf(backgroundColor)
        binding.averageRatings.text = String.format("%.1f", averageRatings)
        binding.ratings.text = "| ${reviewsList.size}"
    }

    private fun setUpCart(){
        binding.addToCart.setOnClickListener {
            if(!isCart) {
                isCart = true
                updateCartIcon()
                CartStore.addItem(
                    CartItemModel(
                        id = cartProductId,
                        title = baseTitle,
                        description = baseDescription,
                        mrp = cartMrp,
                        price = cartPrice,
                        rating = cartRating,
                        ratingCount = cartRatingCount,
                        returnPolicy = cartReturnPolicy,
                        deliveryDetails = cartDeliveryDetails,
                        imageUrl = currentImages.firstOrNull(),
                        quantity = 1,
                        isSelected = true
                    )
                )
                val addToCartSheet = AddToCartSheet.newInstance(
                    imageUrl = currentImages.firstOrNull(),
                    productTitle = baseTitle
                )
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

    private fun observeCartState(){
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED){
                CartStore.items.collect {items->
                    isCart = items.any{it.id == args.productId}
                    updateCartIcon()
                }
            }
        }
    }

    private fun updateCartIcon(){
        binding.addToCart.setImageResource(
            if(isCart) R.drawable.cart_filled else R.drawable.cart
        )
    }

    private fun setUpVariants(){
        variantAdapter = VariantAdapter(variants = currentVariants,{
            selectedVariant = it
            binding.tvVariant.text = it
            updateMainTitle()
        },{ variant , position ->
            binding.tvVariant.text = variant.variant
            selectedVariantPosition = position
            binding.tvPrice.text = variant.Price
            binding.tvMrp.text = variant.mrpPrice
        })
        binding.rvVariants.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL,false)
            adapter = variantAdapter
        }
        if(currentVariants.isNotEmpty()){
            variantAdapter.setSelectedVariantPosition(selectedVariantPosition)
        }

    }

    private fun updateMainTitle(){
        val selectedInfo = listOfNotNull(
            selectedColor.takeIf { it.isNotBlank() },
            selectedVariant.takeIf { it.isNotBlank() }
        )
        val description = if(selectedInfo.isEmpty()){
            baseDescription
        }else{
            "$baseDescription (${selectedInfo.joinToString {", "}})"
        }
        binding.tvDescription.text = description
        binding.tvTitle.text = baseTitle
    }


    private fun configureVariantAndColorData(product: ProductResponse, mrp: Double){
        currentColorOptions = product.colors.orEmpty()
            .distinct()
            .mapIndexed { index , colorName ->
                ProductColorModel(
                    id = index+1,
                    imageUrl = product.thumbnail,
                    modelName = colorName
                )
            }
        currentVariants = product.variants.orEmpty()
            .distinct()
            .mapIndexed { index, variantName ->
                VariantModel(
                    id = index + 1,
                    variant = variantName,
                    Price = CurrencyFormator.formatInr(product.price),
                    mrpPrice = CurrencyFormator.formatInr(mrp)
                )
            }
        val hasColors = currentColorOptions.isNotEmpty()
        val hasVariants = currentVariants.isNotEmpty()
        binding.layoutColorSection.visibility = if(hasColors) View.VISIBLE else View.GONE
        binding.rvColorVariants.visibility = if(hasColors) View.VISIBLE else View.GONE
        binding.layoutVariantSection.visibility = if (hasVariants) View.VISIBLE else View.GONE
        binding.rvVariants.visibility = if (hasVariants) View.VISIBLE else View.GONE
        binding.layoutVaiants.visibility = if(hasColors || hasVariants) View.VISIBLE else View.GONE

        if(hasColors){
            selectedColorPosition = selectedColorPosition.coerceIn(0,currentColorOptions.lastIndex)
            selectedColor =  selectedColor.ifBlank { currentColorOptions[selectedColorPosition].modelName }
            binding.tvSelectedColor.text = selectedColor
        }else{
            selectedColor = ""
        }
        if (hasVariants) {
            selectedVariantPosition = selectedVariantPosition.coerceIn(0, currentVariants.lastIndex)
            val selectedVariantModel = currentVariants[selectedVariantPosition]
            selectedVariant = selectedVariant.ifBlank { selectedVariantModel.variant }
            binding.tvVariant.text = selectedVariantModel.variant
            binding.tvPrice.text = selectedVariantModel.Price
            binding.tvMrp.text = selectedVariantModel.mrpPrice
        } else {
            selectedVariant = ""
            selectedVariantPosition = 0
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("banner_position", currentBannerPosition)
        outState.putString("selected_color", selectedColor)
        outState.putString("selected_variant", selectedVariant)
        outState.putInt("selected_color_position", selectedColorPosition)
        outState.putInt("selected_variant_position", selectedVariantPosition)
    }

    private fun bindTopFeatures(product: ProductResponse){
        val dimensions = product.dimensions
        val dimensionValue = listOfNotNull(
            dimensions?.width?.let { "W: $it" },
            dimensions?.height?.let { "H: $it" },
            dimensions?.depth?.let { "D: $it" }
        ).joinToString(" | ")
        val topFeatures = listOf(
            SpecDetailItem("Brand",product.brand.orEmpty()),
            SpecDetailItem("Dimensions",dimensionValue),
            SpecDetailItem("Category", product.category.orEmpty()),
            SpecDetailItem("Shipping", product.shippingInformation),
            SpecDetailItem("Warranty", product.warrantyInformation),
            SpecDetailItem("Price", CurrencyFormator.formatInr(product.price))
        ).filter {
            it.value.isNotBlank()
        }
        topFeaturesAdapter.submitList(topFeatures.take(5))
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }


}