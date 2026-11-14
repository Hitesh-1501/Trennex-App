package com.example.trennex.ui.checkout

import android.os.Bundle
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
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

    private val viewModel: CartViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPaymentBinding.bind(view)

        (activity as? MainActivity)?.showToolBar(ToolBarType.CHECKOUT, "Payment")
        (activity as? MainActivity)?.toggleCartProgress(true)
        (activity as? MainActivity)?.updateCartStep(3)

        viewModel.resetCheckoutState()
        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.cardTotalAmount.setOnClickListener {
            toggleAmountDetailsCard()
        }

        val paymentContainer = binding.optionCod.parent as ViewGroup
        listOf(binding.optionSaved, binding.optionUpi, binding.optionCard,
            binding.optionEmi, binding.optionNetBanking).forEach { option ->
            val index = paymentContainer.indexOfChild(option)
            for (offset in 0..2) paymentContainer.getChildAt(index + offset)?.visibility = View.GONE
        }
        binding.optionCod.setOnClickListener { toggleAccordion(5) }
        binding.root.findViewById<View>(R.id.btnPlaceOrderCod)?.setOnClickListener {
            viewModel.placeOrder()
        }
        toggleAccordion(5)
    }

    private fun setupRadioGroupListeners() {
        // Common Pay Click Listener
        val payClickListener = View.OnClickListener {
            findNavController().navigate(R.id.action_paymentFragment_to_paymentSuccessFragment)
        }

        // Saved Card Pay Button
        binding.root.findViewById<View>(R.id.btnPaySaved)?.setOnClickListener(payClickListener)

        // UPI Radio Logic
        val rbPhonePe = binding.root.findViewById<android.widget.RadioButton>(R.id.rbPhonePe)
        val rbNewUpi = binding.root.findViewById<android.widget.RadioButton>(R.id.rbNewUpi)
        val btnPayUpi = binding.root.findViewById<View>(R.id.btnPayUpi)
        val layoutNewUpiInput = binding.root.findViewById<View>(R.id.layoutNewUpiInput)

        btnPayUpi?.setOnClickListener(payClickListener)

        rbPhonePe?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                rbNewUpi?.isChecked = false
                btnPayUpi?.visibility = View.VISIBLE
                layoutNewUpiInput?.visibility = View.GONE
            }
        }

        rbNewUpi?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                rbPhonePe?.isChecked = false
                btnPayUpi?.visibility = View.GONE
                layoutNewUpiInput?.visibility = View.VISIBLE
            }
        }

        // Card Pay Button
        binding.root.findViewById<View>(R.id.btnPayCard)?.setOnClickListener(payClickListener)

        // EMI Radio Logic
        val rbCreditCardEmi = binding.root.findViewById<android.widget.RadioButton>(R.id.rbCreditCardEmi)
        val rbBajajEmi = binding.root.findViewById<android.widget.RadioButton>(R.id.rbBajajEmi)
        val tvNoCostEmi = binding.root.findViewById<View>(R.id.tvNoCostEmi)
        val tvEmiAmount = binding.root.findViewById<View>(R.id.tvEmiAmount)
        val layoutEmiLogos = binding.root.findViewById<View>(R.id.layoutEmiLogos)
        val ivBajajLogo = binding.root.findViewById<View>(R.id.ivBajajLogo)
        val btnViewPlansBajaj = binding.root.findViewById<View>(R.id.btnViewPlansBajaj)

        rbCreditCardEmi?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                rbBajajEmi?.isChecked = false
                tvNoCostEmi?.visibility = View.VISIBLE
                tvEmiAmount?.visibility = View.VISIBLE
                layoutEmiLogos?.visibility = View.VISIBLE
                ivBajajLogo?.visibility = View.GONE
                btnViewPlansBajaj?.visibility = View.GONE
            }
        }

        rbBajajEmi?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                rbCreditCardEmi?.isChecked = false
                tvNoCostEmi?.visibility = View.GONE
                tvEmiAmount?.visibility = View.GONE
                layoutEmiLogos?.visibility = View.GONE
                ivBajajLogo?.visibility = View.VISIBLE
                btnViewPlansBajaj?.visibility = View.VISIBLE
            }
        }
        
        // Net Banking Radio Group Logic
        val rbSbi = binding.root.findViewById<android.widget.RadioButton>(R.id.rbSbi)
        val rbHdfc = binding.root.findViewById<android.widget.RadioButton>(R.id.rbHdfc)
        val rbIcici = binding.root.findViewById<android.widget.RadioButton>(R.id.rbIcici)
        val rbKotak = binding.root.findViewById<android.widget.RadioButton>(R.id.rbKotak)
        val rbAxis = binding.root.findViewById<android.widget.RadioButton>(R.id.rbAxis)
        val rbFederal = binding.root.findViewById<android.widget.RadioButton>(R.id.rbFederal)
        val rbIndianBank = binding.root.findViewById<android.widget.RadioButton>(R.id.rbIndianBank)
        
        val netBankingRbs = listOf(rbSbi, rbHdfc, rbIcici, rbKotak, rbAxis, rbFederal, rbIndianBank)
        val btnPayNetBanking = binding.root.findViewById<View>(R.id.btnPayNetBanking)
        btnPayNetBanking?.setOnClickListener(payClickListener)
        
        netBankingRbs.forEach { rb ->
            rb?.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked) {
                    netBankingRbs.forEach { otherRb ->
                        if (otherRb != buttonView) otherRb?.isChecked = false
                    }
                    btnPayNetBanking?.visibility = View.VISIBLE
                }
            }
        }

        // COD
        binding.root.findViewById<View>(R.id.btnPlaceOrderCod)?.setOnClickListener(payClickListener)
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
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.checkoutState.collect { state ->
                    binding.root.findViewById<View>(R.id.btnPlaceOrderCod)?.isEnabled = !state.isSubmitting
                    if (state.isSuccess && findNavController().currentDestination?.id == R.id.paymentFragment) {
                        findNavController().navigate(R.id.action_paymentFragment_to_paymentSuccessFragment)
                    } else if (state.errorMessage != null) {
                        Toast.makeText(requireContext(), state.errorMessage, Toast.LENGTH_LONG).show()
                        viewModel.resetCheckoutState()
                    }
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
