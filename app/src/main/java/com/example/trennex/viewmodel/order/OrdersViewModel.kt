package com.example.trennex.viewmodel.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trennex.repository.user.UserRepository
import com.example.trennex.ui.profile.model.OrderModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class OrdersViewModel : ViewModel() {
    private val userRepository = UserRepository()

    private val _orders = MutableStateFlow<List<OrderModel>>(emptyList())
    val orders: StateFlow<List<OrderModel>> = _orders.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        fetchOrders()
    }

    private fun fetchOrders() {
        viewModelScope.launch {
            userRepository.observeOrders()
                .catch { e ->
                    _error.value = e.message
                    _isLoading.value = false
                }
                .collect { orderList ->
                    _orders.value = orderList
                    _isLoading.value = false
                }
        }
    }
    
    fun filterOrders(query: String) {
        // Implement filtering if needed
    }
}
