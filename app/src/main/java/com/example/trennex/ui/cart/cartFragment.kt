package com.example.trennex.ui.cart

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.trennex.R
import com.example.trennex.databinding.FragmentCartBinding
import com.example.trennex.ui.cart.adapter.CartAdapter
import com.example.trennex.ui.cart.model.CartItemModel

class cartFragment : Fragment(R.layout.fragment_cart) {
    private var _binding : FragmentCartBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
       _binding = FragmentCartBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val cartItems = listOf(
            CartItemModel(1,R.drawable.samsung_mobile,"Samsung","Galaxy S24 5G Snapdragon (Onyx Black, 128 GB) (6 GB RAM)",75000,40999,1),
            CartItemModel(2,R.drawable.samsung_mobile,"Samsung","Galaxy S24 5G Snapdragon (Onyx Black, 128 GB) (6 GB RAM)",75000,40999,1),
            CartItemModel(2,R.drawable.samsung_mobile,"Samsung","Galaxy S24 5G Snapdragon (Onyx Black, 128 GB) (6 GB RAM)",75000,40999,1)
        )
        binding.layoutEmptyCart.visibility = View.GONE
        binding.cartScrollView.visibility = View.VISIBLE
        binding.bottomCheckoutLayout.visibility = View.VISIBLE

        binding.CartRv.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = CartAdapter(cartItems)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}