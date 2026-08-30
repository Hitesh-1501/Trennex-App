package com.example.trennex.ui.checkout

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.trennex.R
import com.example.trennex.databinding.FragmentPaymentBinding
import com.example.trennex.ui.main.MainActivity
import com.example.trennex.ui.main.ToolBarType

class PaymentFragment : Fragment(R.layout.fragment_payment) {

    private var _binding: FragmentPaymentBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPaymentBinding.bind(view)

        (activity as? MainActivity)?.showToolBar(ToolBarType.CART, "Payment")
        (activity as? MainActivity)?.toggleCartProgress(true)
        (activity as? MainActivity)?.updateCartStep(3)

        binding.btnPayNow.setOnClickListener {
            Toast.makeText(requireContext(), "Payment processing...", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
