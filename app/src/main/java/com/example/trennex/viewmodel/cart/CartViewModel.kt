package com.example.trennex.viewmodel.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trennex.repository.user.AddressEntity
import com.example.trennex.repository.user.UserRepository
import com.example.trennex.ui.cart.CartUiState
import com.example.trennex.ui.profile.model.OrderModel
import com.example.trennex.utils.cart.CartStore
import com.google.firebase.Timestamp
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
        if (selectedItems.isEmpty()) return

        viewModelScope.launch {
            try {
                selectedItems.forEach { item ->
                    val order = OrderModel(
                        title = item.title,
                        description = item.description,
                        imageUrl = item.imageUrl,
                        imageRes = item.imageRes,
                        price = item.price,
                        quantity = item.quantity,
                        status = "PENDING",
                        orderDate = Timestamp.now(),
                        expectedDeliveryDate = Timestamp(
                            (System.currentTimeMillis() / 1000) + (7 * 24 * 60 * 60), 
                            0
                        )
                    )
                    userRepository.saveOrder(order)
                }
                CartStore.deleteSelectedItems()
            } catch (e: Exception) {
                // Handle error if needed
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
