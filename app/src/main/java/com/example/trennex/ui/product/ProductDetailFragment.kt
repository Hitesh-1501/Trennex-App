package com.example.trennex.ui.product

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.size
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.trennex.R
import com.example.trennex.databinding.FragmentHomeBinding
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
import androidx.viewpager2.widget.ViewPager2


class ProductDetailFragment : Fragment(R.layout.fragment_product_detail), WishListDialogBinding.WishlistActionListener, AddToCartSheet.AddToCartActionListener{
    private var _binding: FragmentProductDetailBinding? = null
    private val binding get()  = _binding!!
    private lateinit var imageAdapter: ProductImageAdapter
    private lateinit var colorAdapter: ColorVariantAdapter
    private lateinit var  variantAdapter : VariantAdapter
    private var currentImages:List<Int> = emptyList()
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

    val variants = listOf(
        VariantModel(1,"128GB + 8GB","₹40,999","₹75,000",true),
        VariantModel(2,"256GB + 8GB","₹45,999","₹79,000")
    )

    private val colorImageMap = mapOf(
        "Onyx Black" to listOf(
            R.drawable.product_img,
            R.drawable.product_img_two,
            R.drawable.product_img_three,
            R.drawable.product_img_four
        ),
        "Amber Yellow" to listOf(
            R.drawable.amber_yellow_banner,
            R.drawable.product_img_two,
            R.drawable.product_img_three,
            R.drawable.product_img_four
        ),
        "Cobalt Violet" to listOf(
            R.drawable.cobalt_violet_banner,
            R.drawable.product_img_two,
            R.drawable.product_img_three,
            R.drawable.product_img_four
        ),
        "Marble Gray" to listOf(
            R.drawable.marble_gray_banner,
            R.drawable.product_img_two,
            R.drawable.product_img_three,
            R.drawable.product_img_four
        )
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
        setupImageBanner()
        binding.productBanners.offscreenPageLimit = 1
        binding.productBanners.setPageTransformer { page , position ->
            page.alpha = 0.5f +(1-kotlin.math.abs(position))
        }
        setupColorVariants()
        setUpVariants()
        setUpReviews()
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
                val index = colorImageMap.keys.indexOf(color)
                selectedColorPosition = index
                updateMainTitle()
                colorAdapter.setSelectedColorPosition(selectedColorPosition)
                updateProductImages(color)
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

//    @SuppressLint("ClickableViewAccessibility")
//    private fun showWishlistPopup(anchorView: View){
//        val popupBinding = WishlistDialogBinding.inflate(layoutInflater)
//        val widthInPx = (300 * resources.displayMetrics.density).toInt()
//        val popupWindow = PopupWindow(
//            popupBinding.root,
//            widthInPx,
//            ViewGroup.LayoutParams.WRAP_CONTENT,
//            true
//        )
//        popupWindow.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
//        popupWindow.isOutsideTouchable = false
//        popupWindow.isClippingEnabled  = false
//        popupWindow.isFocusable = true
//        popupWindow.setTouchInterceptor { _, event ->
//            event.action == MotionEvent.ACTION_OUTSIDE
//        }
//        popupWindow.animationStyle = R.style.PopupAnimation
//        popupWindow.elevation = 10f
//        popupBinding.root.measure(
//            View.MeasureSpec.makeMeasureSpec(widthInPx,View.MeasureSpec.EXACTLY),
//            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
//        )
//        val popupHeight = popupBinding.root.measuredHeight
//        val xOffset = anchorView.width / 2
//        val yOffset = -(popupHeight + anchorView.height)
//
//        popupBinding.closeIv.setOnClickListener {
//            popupWindow.dismiss()
//        }
//
//        popupBinding.goToWishlist.setOnClickListener {
//            isWishlisted = false
//            binding.wishlist.setImageResource(R.drawable.wishlist_product)
//            popupWindow.dismiss()
//        }
//
//        popupWindow.showAsDropDown(anchorView,xOffset,yOffset)
//        dimBehind(popupWindow)
//    }
//
//    private fun dimBehind(popupWindow: PopupWindow){
//        val container = popupWindow.contentView.rootView
//        val wm = requireContext().getSystemService(Context.WINDOW_SERVICE) as WindowManager
//        val params = container.layoutParams as WindowManager.LayoutParams
//        params.flags = params.flags or WindowManager.LayoutParams.FLAG_DIM_BEHIND
//        params.dimAmount = 0.5f
//        wm.updateViewLayout(container,params)
//
//    }

    private fun setupImageBanner() {

        currentImages = colorImageMap["Onyx Black"]!!

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
            updateProductImages(selected.modelName)
        })
        binding.rvColorVariants.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL,false)
            adapter = colorAdapter
        }
        colorAdapter.setSelectedColorPosition(selectedColorPosition)
    }

    private fun updateProductImages(colorName : String){
        val newImages = colorImageMap[colorName] ?: return
        currentImages  = newImages
        currentBannerPosition = 0
        imageAdapter.updateImages(newImages)
        binding.productBanners.setCurrentItem(0,false)
        attachBannerDots()
    }

    private fun openFullScreen(imageView: ImageView, position : Int,images: List<Int>){
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
            "images" to images.toIntArray(),
            "selected_color" to selectedColor,
            "selected_variant_pos" to selectedVariantPosition
        )
        findNavController()
            .navigate(R.id.action_productDetailFragment_to_imagePreviewFragment,
                bundle,
                null,
                extras)
    }
    private fun setUpReviews(){
        val reviews = listOf(
            ReviewModel(4.5f, "1 month ago", "Excellent performance and great display."),
            ReviewModel(4.1f, "2 months ago", "Camera quality is outstanding."),
            ReviewModel(3.5f, "3 months ago", "Battery life is good for daily usage.")
        )
        binding.rvReviews.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL,false)
            adapter = ReviewAdapter(reviews)
            isNestedScrollingEnabled = false
        }
    }

    private fun setUpVariants(){
        variantAdapter = VariantAdapter(variants = variants,{
            selectedVariant = it
            binding.tvVariant.text = it
            updateMainTitle()
        },{ variant , position ->
            binding.tvPrice.text = variant.Price
            binding.tvMrp.text = variant.mrpPrice
            selectedVariantPosition = position
        })
        binding.rvVariants.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL,false)
            adapter = variantAdapter
        }
        variantAdapter.setSelectedVariantPosition(selectedVariantPosition)
    }

    private fun updateMainTitle(){
        val title = "Galaxy S24 5G Snapdragon ($selectedColor, $selectedVariant)"
        binding.tvProductTitle.text = title
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }


}