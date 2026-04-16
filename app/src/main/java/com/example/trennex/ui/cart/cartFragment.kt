package com.example.trennex.ui.cart

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.trennex.R
import com.example.trennex.databinding.FragmentCartBinding
import com.example.trennex.ui.cart.adapter.CartAdapter
import com.example.trennex.ui.cart.model.CartItemModel
import com.example.trennex.ui.main.MainActivity
import com.example.trennex.ui.main.ToolBarType
import com.example.trennex.utils.CurrencyFormator
import kotlinx.coroutines.launch

class cartFragment : Fragment(R.layout.fragment_cart) {
    private var _binding: FragmentCartBinding? = null
    private val binding get() = _binding!!

    private val viewModel : CartViewModel by viewModels()

    private val cartAdapter by lazy {
        CartAdapter(
            onItemSelectionChanged = {itemId, selected -> viewModel.toggleItem(itemId, selected)},
            onQuantitySelected = {itemId, quantity -> viewModel.updateQuantity(itemId, quantity)}
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
       _binding = FragmentCartBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? MainActivity)?.showToolBar(ToolBarType.CART)

        binding.CartRv.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = cartAdapter
        }

        binding.checkAll.setOnCheckedChangeListener {_,isChecked ->
            viewModel.toggleAll(isChecked)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.uiState.collect(::render)
            }
        }

    }


    private fun render(state: CartUiState){
        val hasItems = state.totalItems > 0
        binding.layoutEmptyCart.root.visibility = if (hasItems) View.GONE else View.VISIBLE
        binding.cartScrollView.visibility = if (hasItems) View.VISIBLE else View.GONE

        (activity as? MainActivity)?.toggleCartProgress(hasItems)
        if(hasItems){
            (activity as? MainActivity)?.updateCartStep(1)
        }
        cartAdapter.submitList(state.items)
        binding.checkAll.setOnCheckedChangeListener(null)
        binding.checkAll.isChecked = state.allSelected
        binding.checkAll.setOnCheckedChangeListener { _, isChecked ->
            viewModel.toggleAll(isChecked)
        }
        binding.tvSelectedItems.text = "${state.selectedItems}/${state.totalItems} Items Selected"
        binding.tvPriceSelected.text = "Price Detail (${state.selectedItems} Items)"
        binding.tvTotalMrp.text = CurrencyFormator.formatInr(state.totalMrp)
        binding.tvBottomSelectedItem.text = if (state.selectedItems > 0) {
            "${state.selectedItems} items selected"
        } else {
            "No item selected"
        }
        binding.tvDiscount.text = "-${CurrencyFormator.formatInr(state.totalDiscount)}"
        binding.tvTotalAmount.text = CurrencyFormator.formatInr(state.totalPrice)



    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}