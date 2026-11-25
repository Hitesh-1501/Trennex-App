package com.example.trennex.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.trennex.R
import com.example.trennex.databinding.FragmentOrdersBinding
import com.example.trennex.ui.main.MainActivity
import com.example.trennex.ui.main.ToolBarType
import com.example.trennex.ui.profile.adapter.OrderAdapter
import com.example.trennex.viewmodel.order.OrdersViewModel
import com.example.trennex.ui.profile.model.OrderModel
import com.example.trennex.ui.common.NetworkStatus
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine

class OrdersFragment : Fragment(R.layout.fragment_orders) {

    private var _binding: FragmentOrdersBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OrdersViewModel by viewModels()
    private val orderAdapter by lazy { 
        OrderAdapter { order ->
            val action = OrdersFragmentDirections.actionOrdersFragmentToOrderDetailFragment(
                orderId = order.id,
                title = order.title,
                description = order.description,
                imageUrl = order.imageUrl,
                price = order.price.toFloat(),
                quantity = order.quantity,
                status = order.status,
                listingPrice = order.listingPrice.toFloat(),
                paymentMethod = order.paymentMethod,
                deliveryAddress = order.deliveryAddress,
                recipientName = order.recipientName,
                recipientPhone = order.recipientPhone
            )
            findNavController().navigate(action)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrdersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? MainActivity)?.showToolBar(ToolBarType.TITLE, "My Orders")

        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        binding.rvOrders.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = orderAdapter
        }
    }

    private fun setupListeners() {
        binding.etSearchOrders.addTextChangedListener { text ->
            viewModel.filterOrders(text?.toString().orEmpty())
        }
        
        binding.btnFilter.setOnClickListener {
            // Show filter dialog/bottom sheet
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(viewModel.orders, viewModel.isLoading, viewModel.error,
                    NetworkStatus.observe(requireContext())) { orders, loading, error, online ->
                    OrdersContentState(orders, loading, error, online)
                }.collect { (orders, loading, error, online) ->
                    orderAdapter.submitList(orders)
                    binding.rvOrders.visibility = if (orders.isEmpty()) View.GONE else View.VISIBLE
                    when {
                        orders.isNotEmpty() -> binding.contentStatus.hide()
                        !online -> binding.contentStatus.showOffline { viewModel.retry() }
                        loading -> binding.contentStatus.showLoading()
                        error != null -> binding.contentStatus.showError(
                            "Could not load orders. Please retry."
                        ) { viewModel.retry() }
                        else -> binding.contentStatus.showEmpty("No orders found")
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private data class OrdersContentState(
        val orders: List<OrderModel>,
        val loading: Boolean,
        val error: String?,
        val online: Boolean,
    )
}
