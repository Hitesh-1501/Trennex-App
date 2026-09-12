package com.example.trennex.ui.profile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.trennex.R
import com.example.trennex.databinding.FragmentOrderDetailsBinding
import com.example.trennex.ui.main.MainActivity
import com.example.trennex.ui.main.ToolBarType
import com.example.trennex.utils.CurrencyFormator

class OrderDetailsFragment : Fragment(R.layout.fragment_order_details) {

    private var _binding: FragmentOrderDetailsBinding? = null
    private val binding get() = _binding!!

    private val args: OrderDetailsFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentOrderDetailsBinding.bind(view)

        (activity as? MainActivity)?.showToolBar(ToolBarType.TITLE, "Order Details")

        val order = args.order
        bindOrderData(order)
        setupListeners(order)
    }

    private fun bindOrderData(order: com.example.trennex.ui.profile.model.OrderModel) {
        if (order.imageUrl != null) {
            Glide.with(binding.ivProduct).load(order.imageUrl).placeholder(R.drawable.placeholder).into(binding.ivProduct)
        } else if (order.imageRes != null) {
            binding.ivProduct.setImageResource(order.imageRes)
        }

        val fullTitle = buildString {
            append(order.title)
            if (order.description.isNotBlank()) {
                append(" (${order.description})")
            }
        }
        binding.tvProductTitle.text = fullTitle

        val formattedId = "Order #${order.id.take(12).uppercase()}"
        binding.tvOrderIdCard.text = formattedId
        binding.tvOrderIdBottom.text = formattedId

        val totalPrice = order.price * order.quantity
        val estimatedMrp = totalPrice * 1.4

        binding.tvListingPrice.text = CurrencyFormator.formatInr(estimatedMrp)
        binding.tvSpecialPrice.text = CurrencyFormator.formatInr(totalPrice)
        binding.tvTotalAmount.text = CurrencyFormator.formatInr(totalPrice)
    }

    private fun setupListeners(order: com.example.trennex.ui.profile.model.OrderModel) {
        val copyAction = View.OnClickListener {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Order ID", order.id)
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
