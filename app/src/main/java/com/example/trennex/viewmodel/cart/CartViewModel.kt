package com.example.trennex.viewmodel.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trennex.repository.user.AddressEntity
import com.example.trennex.repository.user.UserRepository
import com.example.trennex.utils.cart.CartStore
import com.example.trennex.ui.cart.CartUiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CartViewModel: ViewModel() {
    private val userRepository = UserRepository()

    val uiState: StateFlow<CartUiState> = combine(
        CartStore.items,
        userRepository.observeSavedAddresses(),
        userRepository.observeSelectedAddressId(),
        flow { emit(userRepository.getUserName()) }
    ) { items, addresses, selectedId, name ->
        val selected = addresses.find { it.id == selectedId }
        CartUiState.from(items, selected, addresses, name)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CartUiState()
    )

    fun toggleAll(selected: Boolean) = CartStore.toggleSelectAll(selected)
    fun toggleItem(itemId: Int, selected: Boolean) = CartStore.toggleSelection(itemId, selected)
    fun updateQuantity(itemId: Int, quantity: Int) = CartStore.updateQuantity(itemId, quantity)
    fun removeItem(itemId: Int) = CartStore.removeItem(itemId)

    fun deleteSelectedItems() = CartStore.deleteSelectedItems()

    fun selectAddress(address: AddressEntity) {
        viewModelScope.launch {
            userRepository.updateSelectedAddressId(address.id)
        }
    }

    fun deleteAddress(address: AddressEntity) {
        viewModelScope.launch {
            userRepository.deleteAddress(address.id)
        }
    }

    fun saveCurrentAddress(address: String, latitude: Double, longitude: Double) {
        viewModelScope.launch {
            val data = mapOf(
                "address" to address,
                "userName" to uiState.value.userName,
                "addressType" to "Home",
                "latitude" to latitude,
                "longitude" to longitude,
                "placeName" to (address.take(20) + if(address.length > 20) "..." else "")
            )
            val id = userRepository.saveAddress(data)
            if (id != null) {
                userRepository.updateSelectedAddressId(id)
            }
        }
    }
}
