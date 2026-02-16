package com.example.trennex.ui.splash

import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavArgument
import androidx.navigation.fragment.findNavController
import com.example.trennex.R
import com.example.trennex.databinding.FragmentSplashBinding

class SplashFragment : Fragment(R.layout.fragment_splash) {
    private var _binding: FragmentSplashBinding? = null
    private val binding get()  = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSplashBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val window = activity?.window ?: return
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.show(WindowInsetsCompat.Type.systemBars())
        controller.isAppearanceLightStatusBars = true
        binding.trennexLogo.translationX = 1000f

        binding.trennexLogo.animate()
            .translationX(0f)
            .setDuration(1000)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                binding.trennexLogo.postDelayed({
                    binding.trennexLogo.animate().alpha(0f).setDuration(500).start()

                    binding.trennexTitle.animate()
                        .alpha(1.0f)
                        .setDuration(800)
                        .withEndAction {
                            binding.trennexTitle.postDelayed({
                                if(isAdded){
                                    findNavController().navigate(R.id.action_splashFragment_to_onboardingFragment)
                                }
                            },1000)
                        }

                },1000)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}