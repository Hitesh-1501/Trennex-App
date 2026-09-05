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

        binding.optionSaved.setOnClickListener { toggleAccordion(0) }
        binding.optionUpi.setOnClickListener { toggleAccordion(1) }
        binding.optionCard.setOnClickListener { toggleAccordion(2) }
        binding.optionEmi.setOnClickListener { toggleAccordion(3) }
        binding.optionNetBanking.setOnClickListener { toggleAccordion(4) }
        binding.optionCod.setOnClickListener { toggleAccordion(5) }
    }

    private var currentlyExpandedIndex = -1

    private fun toggleAccordion(index: Int) {
        val detailsLayouts = listOf(
            binding.root.findViewById<View>(R.id.layoutSavedDetails),
            binding.root.findViewById<View>(R.id.layoutUpiDetails),
            binding.root.findViewById<View>(R.id.layoutCardDetails),
            binding.root.findViewById<View>(R.id.layoutEmiDetails),
            binding.root.findViewById<View>(R.id.layoutNetBankingDetails),
            binding.root.findViewById<View>(R.id.layoutCodDetails)
        )
        val chevrons = listOf(
            binding.ivChevronSaved,
            binding.ivChevronUpi,
            binding.ivChevronCard,
            binding.ivChevronEmi,
            binding.ivChevronNetBanking,
            binding.ivChevronCod
        )

        TransitionManager.beginDelayedTransition(
            binding.rootContainer,
            AutoTransition().apply { duration = 250 }
        )

        // If clicking the currently open one, collapse it
        if (currentlyExpandedIndex == index) {
            detailsLayouts[index].visibility = View.GONE
            chevrons[index].setImageResource(R.drawable.ic_chevron_down)
            currentlyExpandedIndex = -1
        } else {
            // Collapse previous if any
            if (currentlyExpandedIndex != -1) {
                detailsLayouts[currentlyExpandedIndex].visibility = View.GONE
                chevrons[currentlyExpandedIndex].setImageResource(R.drawable.ic_chevron_down)
            }
            // Expand new one
            detailsLayouts[index].visibility = View.VISIBLE
            chevrons[index].setImageResource(R.drawable.ic_chevron_up)
            currentlyExpandedIndex = index
        }
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
        
        val totalFormatted = CurrencyFormator.formatInr(state.totalPrice)
        binding.root.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnPaySaved)?.text = "Pay $totalFormatted"
        binding.root.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnPayUpi)?.text = "Pay $totalFormatted"
        binding.root.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnPayCard)?.text = "Pay $totalFormatted"
        binding.root.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnPayNetBanking)?.text = "Pay $totalFormatted"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
