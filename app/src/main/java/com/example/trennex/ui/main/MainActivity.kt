package com.example.trennex.ui.main

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.trennex.R
import com.example.trennex.databinding.ActivityMainBinding
import com.example.trennex.databinding.ItemsToolbarBinding
import com.example.trennex.databinding.NavigationIconToolbarBinding
import com.example.trennex.databinding.SearchToolbarBinding
import com.example.trennex.databinding.TitleToolbarBinding
import com.example.trennex.databinding.ToolbarHomeBinding
import com.example.trennex.databinding.ToolbarProductScreenBinding
import np.com.susanthapa.curved_bottom_navigation.CbnMenuItem
import com.example.trennex.databinding.CategoryToolbarBinding
import com.example.trennex.databinding.ToolbarCartBinding
import com.example.trennex.databinding.WishlistToolbarBinding
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.trennex.databinding.ToolbarCollectionSelectionBinding
import com.example.trennex.utils.cart.CartStore
import kotlinx.coroutines.launch


import androidx.activity.viewModels
import com.example.trennex.viewmodel.main.MainViewModel
import android.widget.TextView
import kotlinx.coroutines.flow.collectLatest

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var  navController: NavController
    private val viewModel: MainViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupWindowInsets()
        val navhostFragement =  supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navhostFragement.navController

        initializeBottomMenu()

        binding.curveBottomNav.setupWithNavController(navController)
        observeViewModel()

        navController.addOnDestinationChangedListener {_, destination, _ ->
            when(destination.id){
                R.id.splashFragment, R.id.onboardingFragment, R.id.loginFragment,R.id.imagePreviewFragment ->{
                    showToolBar(ToolBarType.NONE)
                    setLightStatusBar(false)
                    binding.curveBottomNav.visibility = View.GONE

                }
                R.id.otpFragment ->{
                    showToolBar(ToolBarType.OTP)
                    binding.curveBottomNav.visibility = View.GONE
                   setLightStatusBar(false)
                }
                R.id.homeFragment -> {
                    showToolBar(ToolBarType.HOME)
                    setLightStatusBar(false)
                    binding.curveBottomNav.visibility = View.VISIBLE
                }
                R.id.productDetailFragment ->{
                    showToolBar(ToolBarType.PRODUCT)
                    setLightStatusBar(false)
                    binding.curveBottomNav.visibility = View.GONE
                }
                R.id.cartFragment ->{
                    showToolBar(ToolBarType.CART)
                    setLightStatusBar(true)
                    binding.curveBottomNav.visibility = View.VISIBLE
                }
                R.id.profileFragment ->{
                    showToolBar(ToolBarType.TITLE,"Profile")
                    setLightStatusBar(true)
                    binding.curveBottomNav.visibility = View.VISIBLE
                }
                R.id.accountDetailsFragment -> {
                    showToolBar(ToolBarType.TITLE,"Manage Your Account")
                    setLightStatusBar(true)
                    binding.curveBottomNav.visibility = View.GONE
                }
                R.id.wishlistFragment -> {
                    showToolBar(ToolBarType.WISHLIST,"Wishlist")
                    setLightStatusBar(true)
                    binding.curveBottomNav.visibility = View.GONE
                }
                R.id.categoryFragment ->{
                    showToolBar(ToolBarType.CATEGORIES)
                    setLightStatusBar(true)
                    binding.curveBottomNav.visibility = View.VISIBLE
                }
                R.id.productSpecFragment -> {
                    showToolBar(ToolBarType.TITLE,"All Details")
                    setLightStatusBar(true)
                    binding.curveBottomNav.visibility = View.GONE
                }
                R.id.collectionFragment -> {
                    showToolBar(ToolBarType.TITLE,"Collections")
                    setLightStatusBar(true)
                    binding.curveBottomNav.visibility = View.GONE
                }
                R.id.collectionSelectionFragment->{
                    showToolBar(ToolBarType.COLLECTION_SELECTION)
                    setLightStatusBar(true)
                    binding.curveBottomNav.visibility = View.GONE
                }
                R.id.collectionItemsFragment ->{
                    showToolBar(ToolBarType.COLLECTION_ITEMS)
                    setLightStatusBar(true)
                    binding.curveBottomNav.visibility = View.GONE
                }
                R.id.searchFragment -> {
                    showToolBar(ToolBarType.SEARCH)
                    setLightStatusBar(true)
                    binding.curveBottomNav.visibility = View.GONE
                }
                R.id.notificationFragment -> {
                    showToolBar(ToolBarType.NOTIFICATIONS, "Notifications")
                    setLightStatusBar(true)
                    binding.curveBottomNav.visibility = View.GONE
                }
                R.id.addNewAddressFragment -> {
                    showToolBar(ToolBarType.TITLE,"Add New Address")
                    setLightStatusBar(true)
                    binding.curveBottomNav.visibility = View.GONE
                }
            }
            renderCartBadge(viewModel.cartCount.value)
        }
    }

    private fun observeViewModel(){
        lifecycleScope.launch {
            viewModel.cartCount.collect { count->
                renderCartBadge(count)
            }
        }
        lifecycleScope.launch {
            viewModel.selectedAddress.collectLatest { address ->
                updateLocationText(address)
            }
        }
    }

    private fun updateLocationText(address: String?) {
        val locationAddress = binding.toolbarContainer.findViewById<TextView>(R.id.location_address)
        locationAddress?.text = address ?: "Your delivery address"
    }

    private fun renderCartBadge(count: Int){
        val badge = binding.tvCartCountBadge
        val show = binding.curveBottomNav.isVisible && count > 0
        if(show){
            badge.text = if(count > 99) "99+" else count.toString()
            badge.visibility = View.VISIBLE
        }else{
            badge.visibility = View.GONE
        }
    }


    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    private fun setupWindowInsets(){
        ViewCompat.setOnApplyWindowInsetsListener(binding.root){ _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.appBarLayout.setPadding(0, systemBars.top, 0, 0)
            binding.curveBottomNav.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }
    }
    private fun setLightStatusBar(light: Boolean) {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = light
            isAppearanceLightNavigationBars = false
        }
        window.navigationBarColor = Color.BLACK
    }

    fun showToolBar(type: ToolBarType, title: String? = null){
        binding.toolbarContainer.visibility = View.VISIBLE
        binding.toolbarContainer.removeAllViews()
        binding.appBarLayout.setBackgroundColor(Color.WHITE)
        when(type){
            ToolBarType.NONE -> {
                binding.toolbarContainer.visibility = View.GONE
            }
            ToolBarType.HOME ->{
                val toolbarBinding = ToolbarHomeBinding.inflate(layoutInflater)
                binding.toolbarContainer.addView(toolbarBinding.root)
                binding.appBarLayout.setBackgroundResource(R.drawable.toolbar_home_gradient_color)
                
                toolbarBinding.locationAddress.text = viewModel.selectedAddress.value ?: "Your delivery address"
                
                toolbarBinding.notificationBar.setOnClickListener {
                    if(navController.currentDestination?.id == R.id.homeFragment){
                        navController.navigate(R.id.action_homeFragment_to_notificationFragment)
                    }
                }
                toolbarBinding.wishlistBar.setOnClickListener {
                    if(navController.currentDestination?.id == R.id.homeFragment){
                        navController.navigate(R.id.action_homeFragment_to_wishlistFragment)
                    }
                }
                toolbarBinding.searchBar.setOnClickListener {
                    if(navController.currentDestination?.id == R.id.homeFragment){
                        navController.navigate(R.id.searchFragment)
                    }
                }
            }
            ToolBarType.TITLE -> {
                val toolbarBinding = TitleToolbarBinding.inflate(layoutInflater)
                binding.toolbarContainer.addView(toolbarBinding.root)
                toolbarBinding.pageTitle.text = title
                binding.appBarLayout.setBackgroundColor(getColor(R.color.toolbar_bg))
                toolbarBinding.backArrow.setOnClickListener {
                    navController.popBackStack()
                }
            }
            ToolBarType.ITEMS -> {
                val toolbarBinding = ItemsToolbarBinding.inflate(layoutInflater)
                binding.toolbarContainer.addView(toolbarBinding.root)
                toolbarBinding.pageTitle.text = title
            }

            ToolBarType.SEARCH -> {
                val toolbarBinding = SearchToolbarBinding.inflate(layoutInflater)
                binding.toolbarContainer.addView(toolbarBinding.root)
                binding.appBarLayout.setBackgroundColor(getColor(R.color.search_toolbar_bg))
                toolbarBinding.backArrow.setOnClickListener {
                    navController.popBackStack()
                }
            }
            ToolBarType.OTP -> {
                val toolbarBinding = NavigationIconToolbarBinding.inflate(layoutInflater)
                binding.toolbarContainer.addView(toolbarBinding.root)
                toolbarBinding.backArrow.setOnClickListener {
                    navController.popBackStack()
                }
                binding.appBarLayout.setBackgroundColor(Color.WHITE)
            }
            ToolBarType.PRODUCT -> {
                val toolbarBinding = ToolbarProductScreenBinding.inflate(layoutInflater)
                binding.toolbarContainer.addView(toolbarBinding.root)
                toolbarBinding.backArrow.setOnClickListener {
                    navController.popBackStack()
                }
                binding.appBarLayout.setBackgroundColor(getColor(R.color.product_toolbar_bg))
            }
            ToolBarType.CART ->{
                val toolbarBinding = ToolbarCartBinding.inflate(layoutInflater)
                binding.toolbarContainer.addView(toolbarBinding.root)
                binding.appBarLayout.setBackgroundColor(Color.WHITE)
                toolbarBinding.backArrow.setOnClickListener { navController.popBackStack() }
            }
            ToolBarType.WISHLIST -> {
                val toolbarBinding = WishlistToolbarBinding.inflate(layoutInflater)
                binding.toolbarContainer.addView(toolbarBinding.root)
                binding.appBarLayout.setBackgroundColor(getColor(R.color.toolbar_bg))
            }
            ToolBarType.CATEGORIES -> {
                val toolbarBinding = CategoryToolbarBinding.inflate(layoutInflater)
                binding.toolbarContainer.addView(toolbarBinding.root)
                binding.appBarLayout.setBackgroundColor(getColor(R.color.toolbar_bg))
                toolbarBinding.searchBar.setOnClickListener {
                    if (navController.currentDestination?.id == R.id.categoryFragment) {
                        navController.navigate(R.id.searchFragment)
                    }
                }
            }
            ToolBarType.COLLECTION_SELECTION -> {
                val toolbarBinding = ToolbarCollectionSelectionBinding.inflate(layoutInflater)
                binding.toolbarContainer.addView(toolbarBinding.root)
                binding.appBarLayout.setBackgroundColor(Color.WHITE)
            }
            ToolBarType.COLLECTION_ITEMS -> {
                val toolbarBinding = WishlistToolbarBinding.inflate(layoutInflater)
                binding.toolbarContainer.addView(toolbarBinding.root)
                binding.appBarLayout.setBackgroundColor(Color.WHITE)
            }
            ToolBarType.NOTIFICATIONS -> {
                val toolbarBinding = TitleToolbarBinding.inflate(layoutInflater)
                binding.toolbarContainer.addView(toolbarBinding.root)
                toolbarBinding.pageTitle.text = title
                binding.appBarLayout.setBackgroundColor(getColor(R.color.notification_toolbar_bg))
                toolbarBinding.backArrow.setOnClickListener {
                    navController.popBackStack()
                }
            }
        }
    }

    private fun  initializeBottomMenu(){
        val menuItems = arrayOf(
            CbnMenuItem(
                R.drawable.home,
                R.drawable.avd_home,
                R.id.homeFragment,
                "Home"
            ),
            CbnMenuItem(
                R.drawable.category_icon,
                R.drawable.avd_category,
                R.id.categoryFragment,
                "Category"
            ),
            CbnMenuItem(
                R.drawable.profile_icon,
                R.drawable.avd_profile,
                R.id.profileFragment,
                "Profile"
            ),
            CbnMenuItem(
                R.drawable.cart_icon,
                R.drawable.avd_cart,
                R.id.cartFragment,
                "Cart"
            )
        )
        binding.curveBottomNav.setMenuItems(menuItems, 0)

    }
    fun toggleCartProgress(isVisible : Boolean){
        val container = binding.toolbarContainer.findViewById<View>(R.id.progressContainer)
        container?.visibility = if(isVisible) View.VISIBLE else View.GONE
    }

    fun updateCartStep(step: Int){
        val root = binding.toolbarContainer
        val pbCart = root.findViewById<ProgressBar>(R.id.pbCart) ?: return
        val statusCart = root.findViewById<ImageView>(R.id.statusCart)

        val pbAddress = root.findViewById<ProgressBar>(R.id.pbAddress)
        val statusAddress = root.findViewById<ImageView>(R.id.statusAddress)

        val pbPayment = root.findViewById<ProgressBar>(R.id.pbPayment)
        val statusPayment = root.findViewById<ImageView>(R.id.statusPayment)

        when(step){
            1 -> {
                pbCart.isIndeterminate = true
                statusCart.setImageResource(R.drawable.ic_step_active)

                pbAddress.isIndeterminate = false
                pbAddress.progress = 0
                statusAddress.setImageResource(R.drawable.ic_step_inactive)

                pbPayment.isIndeterminate = false
                pbPayment.progress = 0
                statusPayment.setImageResource(R.drawable.ic_step_inactive)
            }
            2->{
                pbCart.isIndeterminate = false
                pbCart.progress = 100
                pbCart.progressTintList = ColorStateList.valueOf(getColor(R.color.colorProgress))
                statusCart.setImageResource(R.drawable.ic_step_done)

                pbAddress.isIndeterminate = true
                pbAddress.indeterminateTintList = ColorStateList.valueOf(getColor(R.color.colorPrimary))
                statusAddress.setImageResource(R.drawable.ic_step_active)

                pbPayment.isIndeterminate = false
                pbPayment.progress = 0
                statusPayment.setImageResource(R.drawable.ic_step_inactive)

            }

            3 ->{
                pbCart.isIndeterminate = false
                pbCart.progress = 100
                pbCart.progressTintList = ColorStateList.valueOf(getColor(R.color.colorProgress))
                statusCart.setImageResource(R.drawable.ic_step_done)

                pbAddress.isIndeterminate = false
                pbAddress.progress = 100
                pbAddress.progressTintList = ColorStateList.valueOf(getColor(R.color.colorProgress))
                statusAddress.setImageResource(R.drawable.ic_step_done)

                pbPayment.isIndeterminate = true
                pbPayment.indeterminateTintList = ColorStateList.valueOf(getColor(R.color.colorPrimary))
                statusPayment.setImageResource(R.drawable.ic_step_active)
            }
        }
    }
}
