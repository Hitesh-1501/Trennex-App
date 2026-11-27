package com.example.trennex.viewmodel.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trennex.repository.user.AddressEntity
import com.example.trennex.repository.user.UserRepository
import com.example.trennex.data.local.AccountScope
import com.example.trennex.ui.cart.CartUiState
import com.example.trennex.analytics.AppAnalytics
import com.example.trennex.crash.AppCrash
import com.example.trennex.crash.CrashArea
import com.example.trennex.analytics.AnalyticsEvents
import com.example.trennex.utils.cart.CartStore
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CheckoutState(
    val isSubmitting: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
)

class CartViewModel: ViewModel() {
    private val userRepository = UserRepository()
    private val _checkoutState = MutableStateFlow(CheckoutState())
    val checkoutState: StateFlow<CheckoutState> = _checkoutState

    val uiState: StateFlow<CartUiState> = combine(
        CartStore.items,
        userRepository.observeSavedAddresses().catch {
            android.util.Log.w("CartViewModel", "Address listener stopped", it)
            emit(emptyList())
        },
        userRepository.observeSelectedAddressId().catch {
            android.util.Log.w("CartViewModel", "Selected-address listener stopped", it)
            emit(null)
        },
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
    fun toggleItem(cartKey: String, selected: Boolean) = CartStore.toggleSelection(cartKey, selected)
    fun updateQuantity(cartKey: String, quantity: Int) = CartStore.updateQuantity(cartKey, quantity)
    fun removeItem(cartKey: String) = CartStore.removeItem(cartKey)

    fun deleteSelectedItems() = CartStore.deleteSelectedItems()

    fun resetCheckoutState() {
        if (!_checkoutState.value.isSubmitting) _checkoutState.value = CheckoutState()
    }

    fun placeOrder(isOnline: Boolean) {
        if (_checkoutState.value.isSubmitting || _checkoutState.value.isSuccess) return
        if (!isOnline) {
            _checkoutState.value = CheckoutState(errorMessage = "Connect to the internet before placing your order.")
            return
        }
        val selectedItems = uiState.value.items.filter { it.isSelected }
        android.util.Log.d("CartViewModel", "placeOrder: items count = ${selectedItems.size}")
        if (selectedItems.isEmpty()) {
            _checkoutState.value = CheckoutState(errorMessage = "Your cart has no selected items.")
            return
        }

        val checkoutOwner = AccountScope.currentOwner()
        _checkoutState.value = CheckoutState(isSubmitting = true)
        AppCrash.get().log(CrashArea.CHECKOUT, "start")
        viewModelScope.launch {
            try {
                val address = uiState.value.selectedAddress
                    ?: throw IllegalStateException("Please select a delivery address")
                val orderId = userRepository.submitOrderRequest(
                    items = selectedItems.map { item ->
                        buildMap<String, Any> {
                            if (item.productId != null && item.variantId != null) {
                                put("productId", item.productId)
                                put("variantId", item.variantId)
                            } else put("legacyId", item.id)
                            put("quantity", item.quantity.coerceIn(1, 10))
                        }
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
                android.util.Log.d("CartViewModel", "order confirmed")
                CartStore.deleteSelectedItems(checkoutOwner)
                _checkoutState.value = CheckoutState(isSuccess = true)
                AppCrash.get().log(CrashArea.CHECKOUT, "complete")
                // Analytics is intentionally outside the checkout success path.
                if (AppAnalytics.get().isCollectionEnabled) {
                    try {
                        val (totalMinor, items) = userRepository.getConfirmedCodOrderAnalytics(orderId)
                        AppAnalytics.get().trackPurchaseOnce(orderId,
                            AnalyticsEvents.confirmedCodOrder(orderId, totalMinor, items))
                    } catch (analyticsError: Exception) {
                        android.util.Log.w("CartViewModel", "Confirmed order analytics unavailable", analyticsError)
                    }
                }
            } catch (e: Exception) {
                AppCrash.get().log(CrashArea.CHECKOUT, "failed")
                if (e !is IllegalStateException) AppCrash.get().recordUnexpected(CrashArea.CHECKOUT, e)
                android.util.Log.e("CartViewModel", "order request failed; cart was preserved", e)
                _checkoutState.value = CheckoutState(
                    errorMessage = (if (e is FirebaseFunctionsException &&
                        e.code == FirebaseFunctionsException.Code.NOT_FOUND
                    ) {
                        "Order service is unavailable. Please try again later."
                    } else e.message)?.takeIf { it.isNotBlank() }
                        ?: "We could not place your order. Please try again.",
                )
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
