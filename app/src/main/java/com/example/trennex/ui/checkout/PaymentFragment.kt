package com.example.trennex.ui.checkout

import android.os.Bundle
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.trennex.R
import com.example.trennex.databinding.FragmentPaymentBinding
import com.example.trennex.ui.cart.CartUiState
import com.example.trennex.ui.main.MainActivity
import com.example.trennex.ui.main.ToolBarType
import com.example.trennex.utils.CurrencyFormator
import com.example.trennex.viewmodel.cart.CartViewModel
import kotlinx.coroutines.launch

class PaymentFragment : Fragment(R.layout.fragment_payment) {

    private var _binding: FragmentPaymentBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CartViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPaymentBinding.bind(view)

        (activity as? MainActivity)?.showToolBar(ToolBarType.CART, "Payment")
        (activity as? MainActivity)?.toggleCartProgress(true)
        (activity as? MainActivity)?.updateCartStep(3)

        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.cardTotalAmount.setOnClickListener {
            toggleAmountDetailsCard()
        }

        val optionClickListener = View.OnClickListener { v ->
            val optionName = when (v.id) {
                R.id.optionSaved -> "Saved Payment Options"
                R.id.optionUpi -> "UPI"
                R.id.optionCard -> "Credit / Debit / ATM Card"
                R.id.optionEmi -> "EMI"
                R.id.optionNetBanking -> "Net Banking"
                R.id.optionCod -> "Cash on Delivery"
                else -> "Payment Option"
            }
            Toast.makeText(requireContext(), "$optionName selected", Toast.LENGTH_SHORT).show()
        }

        binding.optionSaved.setOnClickListener(optionClickListener)
        binding.optionUpi.setOnClickListener(optionClickListener)
        binding.optionCard.setOnClickListener(optionClickListener)
        binding.optionEmi.setOnClickListener(optionClickListener)
        binding.optionNetBanking.setOnClickListener(optionClickListener)
        binding.optionCod.setOnClickListener(optionClickListener)
    }

    private fun toggleAmountDetailsCard() {
        val isExpanded = binding.layoutAmountDetails.isVisible

        TransitionManager.beginDelayedTransition(
            binding.cardTotalAmount,
            AutoTransition().apply {
                duration = 250
            }
        )

        if (isExpanded) {
            binding.layoutAmountDetails.visibility = View.GONE
            binding.ivChevron.setImageResource(R.drawable.ic_chevron_down)
        } else {
            binding.layoutAmountDetails.visibility = View.VISIBLE
            binding.ivChevron.setImageResource(R.drawable.ic_chevron_up)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    render(state)
                }
            }
        }
    }

    private fun render(state: CartUiState) {
        binding.tvMrp.text = CurrencyFormator.formatInr(state.totalMrp)
        binding.tvPlatformFee.text = "₹0"
        binding.tvDiscount.text = "-${CurrencyFormator.formatInr(state.totalDiscount)}"
        binding.tvTotalAmount.text = CurrencyFormator.formatInr(state.totalPrice)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
