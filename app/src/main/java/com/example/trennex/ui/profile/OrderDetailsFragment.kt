package com.example.trennex.ui.profile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.trennex.R
import com.example.trennex.databinding.DialogOrderDeliveryAddressBinding
import com.example.trennex.databinding.DialogOrderUserDetailsBinding
import com.example.trennex.databinding.FragmentOrderDetailsBinding
import com.example.trennex.ui.main.MainActivity
import com.example.trennex.ui.main.ToolBarType
import com.example.trennex.utils.CurrencyFormator
import com.example.trennex.viewmodel.cart.CartViewModel
import kotlinx.coroutines.launch

class OrderDetailsFragment : Fragment(R.layout.fragment_order_details) {

    private var _binding: FragmentOrderDetailsBinding? = null
    private val binding get() = _binding!!

    private val args: OrderDetailsFragmentArgs by navArgs()
    private val viewModel: CartViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentOrderDetailsBinding.bind(view)

        (activity as? MainActivity)?.showToolBar(ToolBarType.TITLE, "Order Details")

        bindOrderData()
        observeUserData()
        setupListeners()
    }

    private fun bindOrderData() {
        if (!args.imageUrl.isNullOrBlank()) {
            Glide.with(binding.ivProduct).load(args.imageUrl).placeholder(R.drawable.placeholder).into(binding.ivProduct)
        } else {
            binding.ivProduct.setImageResource(R.drawable.placeholder)
        }

        val fullTitle = buildString {
            append(args.title)
            if (args.description.isNotBlank()) {
                append(" (${args.description})")
            }
        }
        binding.tvProductTitle.text = fullTitle

        val orderIdDisplay = if (args.orderId.isNotBlank()) args.orderId.take(12).uppercase() else "12344544453"
        val formattedId = "Order #$orderIdDisplay"
        binding.tvOrderIdCard.text = formattedId
        binding.tvOrderIdBottom.text = formattedId

        val unitPrice = args.price.toDouble()
        val totalPrice = unitPrice * args.quantity
        val estimatedMrp = totalPrice * 1.4

        binding.tvListingPrice.text = CurrencyFormator.formatInr(estimatedMrp)
        binding.tvSpecialPrice.text = CurrencyFormator.formatInr(totalPrice)
        binding.tvTotalAmount.text = CurrencyFormator.formatInr(totalPrice)
    }

    private fun observeUserData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    state.selectedAddress?.let {
                        binding.tvDeliveryAddress.text = it.displayAddress
                        val phone = state.userPhone.ifBlank { it.mobile }
                        binding.tvUserContact.text = "${state.userName}  $phone"
                    } ?: run {
                        binding.tvDeliveryAddress.text = "Select Delivery Address"
                        binding.tvUserContact.text = "${state.userName}  ${state.userPhone}"
                    }
                }
            }
        }
    }

    private fun setupListeners() {
        val copyAction = View.OnClickListener {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Order ID", args.orderId)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(requireContext(), "Order ID copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        binding.btnCopyOrderIdCard.setOnClickListener(copyAction)
        binding.btnCopyOrderIdBottom.setOnClickListener(copyAction)

        binding.btnTrackOrder.setOnClickListener {
            Toast.makeText(requireContext(), "Order is on the way!", Toast.LENGTH_SHORT).show()
        }

        binding.btnShopMore.setOnClickListener {
            findNavController().navigate(R.id.homeFragment)
        }

        binding.btnHelp.setOnClickListener {
            Toast.makeText(requireContext(), "Redirecting to Help Center...", Toast.LENGTH_SHORT).show()
        }
        
        binding.tvDeliveryAddress.setOnClickListener { showDeliveryAddressDialog() }
        binding.tvUserContact.setOnClickListener { showUserDetailsDialog() }
    }

    private fun showDeliveryAddressDialog() {
        val dialogBinding = DialogOrderDeliveryAddressBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()
            
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        val state = viewModel.uiState.value
        state.selectedAddress?.let {
            dialogBinding.tvUserName.text = it.userName
            dialogBinding.tvFullAddress.text = it.displayAddress
            dialogBinding.tvMobile.text = it.mobile.ifBlank { state.userPhone }
        }
        
        dialogBinding.ivClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showUserDetailsDialog() {
        val dialogBinding = DialogOrderUserDetailsBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()
            
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        val state = viewModel.uiState.value
        dialogBinding.etName.setText(state.userName)
        dialogBinding.etPhone.setText(state.userPhone)

        val initialName = state.userName
        val initialPhone = state.userPhone

        fun validateInput() {
            val nameText = dialogBinding.etName.text.toString().trim()
            val phoneText = dialogBinding.etPhone.text.toString().trim()
            val isValid = (nameText != initialName || phoneText != initialPhone) && nameText.isNotBlank() && phoneText.length == 10
            dialogBinding.btnUpdate.isEnabled = isValid
            
            dialogBinding.btnUpdate.backgroundTintList = android.content.res.ColorStateList.valueOf(
                androidx.core.content.ContextCompat.getColor(
                    requireContext(),
                    if (isValid) R.color.colorAccent else R.color.textPlaceholder
                )
            )
        }

        dialogBinding.etName.addTextChangedListener { validateInput() }
        dialogBinding.etPhone.addTextChangedListener { validateInput() }
        validateInput()

        dialogBinding.btnUpdate.setOnClickListener {
            val newName = dialogBinding.etName.text.toString().trim()
            val newPhone = dialogBinding.etPhone.text.toString().trim()
            
            viewModel.updateUserDetails(newName, newPhone)
            
            binding.tvUserContact.text = "$newName  $newPhone"
            Toast.makeText(requireContext(), "User details updated", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialogBinding.ivClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
