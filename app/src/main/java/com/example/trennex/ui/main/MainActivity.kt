package com.example.trennex.ui.main

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
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


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var  navController: NavController
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window,false)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.statusBarColor = Color.TRANSPARENT

        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightNavigationBars = false
        }

        val navhostFragement =  supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navhostFragement.navController

        initializeBottomMenu()

        binding.curveBottomNav.setupWithNavController(navController)

        setupWindowInsets()

        navController.addOnDestinationChangedListener {_, destination, _ ->
            when(destination.id){
                R.id.splashFragment, R.id.onboardingFragment, R.id.loginFragment ->{
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
                    setLightStatusBar(true)
                    binding.curveBottomNav.visibility = View.VISIBLE
                }
                R.id.productDetailFragment ->{
                    showToolBar(ToolBarType.PRODUCT)
                    setLightStatusBar(true)
                    binding.curveBottomNav.visibility = View.GONE
                }
            }
        }
    }
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    private fun setupWindowInsets(){
        ViewCompat.setOnApplyWindowInsetsListener(binding.root){ view, insets ->
            val statusBarHeight =  insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            val navBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            binding.toolbarContainer.setPadding(
                0,statusBarHeight,0,0)

            view.setPadding(
                0,
                0,
                0,
                navBarHeight
            )
            insets
        }
    }
    private fun setLightStatusBar(light: Boolean) {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = light
            isAppearanceLightNavigationBars = true
        }
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.BLACK
    }

    fun showToolBar(type: ToolBarType, title: String? = null){
        binding.toolbarContainer.visibility = View.VISIBLE
        binding.toolbarContainer.removeAllViews()
        when(type){
            ToolBarType.NONE -> {
                binding.toolbarContainer.visibility = View.GONE
            }
            ToolBarType.HOME ->{
                val toolbarBinding = ToolbarHomeBinding.inflate(layoutInflater)
                binding.toolbarContainer.addView(toolbarBinding.root)

            }
            ToolBarType.TITLE -> {
                val toolbarBinding = TitleToolbarBinding.inflate(layoutInflater)
                binding.toolbarContainer.addView(toolbarBinding.root)
                toolbarBinding.pageTitle.text = title
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
            }
            ToolBarType.OTP -> {
                val toolbarBinding = NavigationIconToolbarBinding.inflate(layoutInflater)
                binding.toolbarContainer.addView(toolbarBinding.root)
                toolbarBinding.backArrow.setOnClickListener {
                    navController.popBackStack()
                }
            }
            ToolBarType.PRODUCT -> {
                val toolbarBinding = ToolbarProductScreenBinding.inflate(layoutInflater)
                binding.toolbarContainer.addView(toolbarBinding.root)
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
}