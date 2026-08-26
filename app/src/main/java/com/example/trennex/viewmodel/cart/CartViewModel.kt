package com.example.trennex.viewmodel.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trennex.repository.user.AddressEntity
import com.example.trennex.repository.user.UserRepository
import com.example.trennex.utils.cart.CartStore
import com.example.trennex.ui.cart.CartUiState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CartViewModel: ViewModel() {
    private val userRepository = UserRepository()

    private val _events = MutableSharedFlow<CartEvent>()
    val events = _events.asSharedFlow()

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

    fun onPlaceOrderClicked() {
        if (uiState.value.selectedItems > 0) {
            viewModelScope.launch {
                _events.emit(CartEvent.NavigateToCheckout)
            }
        } else {
            viewModelScope.launch {
                _events.emit(CartEvent.ShowMessage("Please select at least one item to proceed"))
            }
        }
    }

    fun onDeleteClicked() {
        val selectedCount = uiState.value.selectedItems
        viewModelScope.launch {
            if (selectedCount > 0) {
                _events.emit(CartEvent.ShowDeleteConfirmation(selectedCount))
            } else {
                _events.emit(CartEvent.ShowMessage("Please select at least one item"))
            }
        }
    }

    fun onChangeAddressClicked() {
        viewModelScope.launch {
            _events.emit(CartEvent.ShowAddressSelection)
        }
    }

    fun toggleAll(selected: Boolean) = CartStore.toggleSelectAll(selected)
    fun toggleItem(itemId: Int, selected: Boolean) = CartStore.toggleSelection(itemId, selected)
    fun updateQuantity(itemId: Int, quantity: Int) = CartStore.updateQuantity(itemId, quantity)
    fun removeItem(itemId: Int) = CartStore.removeItem(itemId)
    fun deleteSelectedItems() = CartStore.deleteSelectedItems()

    fun selectAddress(addressId: String) {
        viewModelScope.launch {
            userRepository.updateSelectedAddressId(addressId)
        }
    }

    fun deleteAddress(addressId: String) {
        viewModelScope.launch {
            userRepository.deleteAddress(addressId)
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

sealed class CartEvent {
    object NavigateToCheckout : CartEvent()
    object ShowAddressSelection : CartEvent()
    data class ShowDeleteConfirmation(val count: Int) : CartEvent()
    data class ShowMessage(val message: String) : CartEvent()
}
