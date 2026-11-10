package com.example.trennex.viewmodel.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trennex.repository.user.AddressEntity
import com.example.trennex.repository.user.UserRepository
import com.example.trennex.ui.cart.CartUiState
import com.example.trennex.utils.cart.CartStore
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
        flow { emit(userRepository.getUserDetails()) }
    ) { items, addresses, selectedId, userDetails ->
        val selected = addresses.find { it.id == selectedId }
        CartUiState.from(
            items = items,
            selectedAddress = selected,
            savedAddresses = addresses,
            userName = userDetails?.get("name").orEmpty().ifBlank { "Guest User" },
            userPhone = userDetails?.get("phone").orEmpty()
        )
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

    fun placeOrder() {
        val selectedItems = uiState.value.items.filter { it.isSelected }
        android.util.Log.d("CartViewModel", "placeOrder: items count = ${selectedItems.size}")
        if (selectedItems.isEmpty()) return

        viewModelScope.launch {
            try {
                val address = uiState.value.selectedAddress
                    ?: throw IllegalStateException("Please select a delivery address")
                val requestId = userRepository.submitOrderRequest(
                    items = selectedItems.map { item ->
                        mapOf(
                            "legacyId" to item.id,
                            "quantity" to item.quantity.coerceIn(1, 10),
                        )
                    },
                    shippingAddress = mapOf(
                        "address" to address.address,
                        "flatNo" to address.flatNo,
                        "placeName" to address.placeName,
                        "addressType" to address.addressType,
                        "latitude" to address.latitude,
                        "longitude" to address.longitude,
                    ),
                    recipient = mapOf(
                        "name" to uiState.value.userName,
                        "phone" to uiState.value.userPhone,
                    ),
                    paymentMethod = "COD",
                )
                android.util.Log.d("CartViewModel", "order request accepted: $requestId")
                CartStore.deleteSelectedItems()
            } catch (e: Exception) {
                android.util.Log.e("CartViewModel", "order request failed; cart was preserved", e)
            }
        }
    }

    fun updateUserDetails(name: String, phone: String) {
        viewModelScope.launch {
            try {
                userRepository.updateUserDetails(name, phone)
                // The `uiState` will automatically update since it observes `getUserDetails()`
                // However, `getUserDetails` is fetched once per combine block creation in current logic.
                // We should ensure it triggers a refresh if needed, or rely on caller to manage local UI update.
            } catch (e: Exception) {
                android.util.Log.e("CartViewModel", "Failed to update user details", e)
            }
        }
    }

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
                "mobile" to uiState.value.userPhone,
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
