package com.example.trennex.ui.main

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Color.parseColor
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.activity.enableEdgeToEdge
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
import androidx.core.graphics.toColorInt
import com.example.trennex.databinding.LayoutAddToCartBinding
import com.example.trennex.databinding.ToolbarCartBinding


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var  navController: NavController
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.statusBarColor = Color.TRANSPARENT
        setupWindowInsets()
        val navhostFragement =  supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navhostFragement.navController

        initializeBottomMenu()

        binding.curveBottomNav.setupWithNavController(navController)

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
            }
        }
    }
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    private fun setupWindowInsets(){
        ViewCompat.setOnApplyWindowInsetsListener(binding.root){ view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.appBarLayout.setPadding(0, systemBars.top, 0, 0)
            binding.root.setPadding(0, 0, 0, systemBars.bottom)

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
                binding.appBarLayout.setBackgroundColor(Color.WHITE)
            }
            ToolBarType.PRODUCT -> {
                val toolbarBinding = ToolbarProductScreenBinding.inflate(layoutInflater)
                binding.toolbarContainer.addView(toolbarBinding.root)
                toolbarBinding.backArrow.setOnClickListener {
                    navController.popBackStack()
                }
                binding.appBarLayout.setBackgroundColor("#4D2962FF".toColorInt())
            }
            ToolBarType.CART ->{
                val toolbarBinding = ToolbarCartBinding.inflate(layoutInflater)
                binding.toolbarContainer.addView(toolbarBinding.root)
                toolbarBinding.backArrow.setOnClickListener { navController.popBackStack() }
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
                pbCart.progressTintList = ColorStateList.valueOf("#21AD60".toColorInt())
                statusCart.setImageResource(R.drawable.ic_step_done)

                pbAddress.isIndeterminate = true
                pbAddress.indeterminateTintList = ColorStateList.valueOf("#2962FF".toColorInt())
                statusAddress.setImageResource(R.drawable.ic_step_active)

                pbPayment.isIndeterminate = false
                pbPayment.progress = 0
                statusPayment.setImageResource(R.drawable.ic_step_inactive)

            }

            3 ->{
                pbCart.isIndeterminate = false
                pbCart.progress = 100
                pbCart.progressTintList = ColorStateList.valueOf("#21AD60".toColorInt())
                statusCart.setImageResource(R.drawable.ic_step_done)

                pbAddress.isIndeterminate = false
                pbAddress.progress = 100
                pbAddress.progressTintList = ColorStateList.valueOf("#21AD60".toColorInt())
                statusAddress.setImageResource(R.drawable.ic_step_done)

                pbPayment.isIndeterminate = true
                pbPayment.indeterminateTintList = ColorStateList.valueOf("#2962FF".toColorInt())
                statusPayment.setImageResource(R.drawable.ic_step_active)
            }
        }
    }
}