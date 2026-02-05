package com.example.trennex.ui.main

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.WindowInsetsController
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


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var  navController: NavController
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navhostFragement =  supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navhostFragement.navController

        navController.addOnDestinationChangedListener {_, destination, _ ->
            when(destination.id){
                R.id.splashFragment, R.id.onboardingFragment, R.id.loginFragment ->{
                    showToolBar(ToolBarType.NONE)
                    setFullScreen()
                }
                R.id.otpFragment ->{
                    showToolBar(ToolBarType.OTP)
                    setFullScreen()
                }
                R.id.homeFragment -> {
                    showToolBar(ToolBarType.HOME)
                    setNormalMode()
                }
            }
        }
    }
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
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
        }

    }
    private fun setNormalMode(){
        WindowCompat.setDecorFitsSystemWindows(window,true)
        WindowInsetsControllerCompat(window,window.decorView).apply {
            show(WindowInsetsCompat.Type.statusBars())
            isAppearanceLightStatusBars = true
        }
        window.statusBarColor = getColor(R.color.white)
    }

    private fun setFullScreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.statusBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        window.statusBarColor = Color.TRANSPARENT
    }

}