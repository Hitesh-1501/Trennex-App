package com.example.trennex.ui.checkout

import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.trennex.R
import com.example.trennex.databinding.FragmentPaymentSuccessBinding
import com.example.trennex.ui.main.MainActivity
import com.example.trennex.ui.main.ToolBarType
import com.example.trennex.viewmodel.cart.CartViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PaymentSuccessFragment : Fragment(R.layout.fragment_payment_success) {

    private var _binding: FragmentPaymentSuccessBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: CartViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPaymentSuccessBinding.bind(view)

        (activity as? MainActivity)?.showToolBar(ToolBarType.NONE)
        
        // Start animation
        startSuccessAnimation()
        
        // Remove only the items that were purchased (selected in cart)
        viewModel.deleteSelectedItems()

        // Navigate away after delay
        viewLifecycleOwner.lifecycleScope.launch {
            delay(2500)
            findNavController().navigate(R.id.action_paymentSuccessFragment_to_ordersFragment)
        }
    }

    private fun startSuccessAnimation() {
        binding.ivSuccessCheck.scaleX = 0f
        binding.ivSuccessCheck.scaleY = 0f
        binding.ivSuccessCheck.alpha = 0f
        
        binding.ivSuccessCheck.animate()
            .scaleX(1.2f)
            .scaleY(1.2f)
            .alpha(1f)
            .setDuration(500)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                binding.ivSuccessCheck.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(200)
                    .start()
            }
            .start()

        binding.tvSuccessTitle.alpha = 0f
        binding.tvSuccessTitle.translationY = 20f
        binding.tvSuccessTitle.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(400)
            .setDuration(400)
            .start()

        binding.tvSuccessSubtitle.alpha = 0f
        binding.tvSuccessSubtitle.translationY = 20f
        binding.tvSuccessSubtitle.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(600)
            .setDuration(400)
            .start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
