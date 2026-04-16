package com.example.trennex.ui.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class CartViewModel: ViewModel() {
    val uiState: StateFlow<CartUiState> = CartStore.items
        .map(CartUiState.Companion::from)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CartUiState()
        )
    fun toggleAll(selected: Boolean) = CartStore.toggleSelectAll(selected)
    fun toggleItem(itemId: Int, selected: Boolean) = CartStore.toggleSelection(itemId, selected)
    fun updateQuantity(itemId: Int, quantity: Int) = CartStore.updateQuantity(itemId, quantity)
}