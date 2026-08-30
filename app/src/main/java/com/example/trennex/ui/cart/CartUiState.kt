package com.example.trennex.ui.cart

import com.example.trennex.repository.user.AddressEntity
import com.example.trennex.ui.cart.model.CartItemModel

data class CartUiState(
    val items: List<CartItemModel> = emptyList(),
    val totalItems: Int = 0,
    val selectedItems: Int = 0,
    val totalMrp: Double = 0.0,
    val totalPrice: Double = 0.0,
    val totalDiscount: Double = 0.0,
    val allSelected: Boolean = false,
    val selectedAddress: AddressEntity? = null,
    val savedAddresses: List<AddressEntity> = emptyList(),
    val userName: String = "",
    val userPhone: String = ""
){
    companion object{
        fun from(
            items: List<CartItemModel>,
            selectedAddress: AddressEntity? = null,
            savedAddresses: List<AddressEntity> = emptyList(),
            userName: String = "",
            userPhone: String = ""
        ): CartUiState{
            val selected = items.filter { it.isSelected }
            val totalMrp = selected.sumOf { it.mrp * it.quantity}
            val totalPrice = selected.sumOf { it.price * it.quantity}
            return CartUiState(
                items = items,
                totalItems = items.size,
                selectedItems = selected.size,
                totalMrp = totalMrp,
                totalPrice = totalPrice,
                totalDiscount = (totalMrp - totalPrice).coerceAtLeast(0.0),
                allSelected = items.isNotEmpty() && selected.size == items.size,
                selectedAddress = selectedAddress,
                savedAddresses = savedAddresses,
                userName = userName,
                userPhone = userPhone
            )
        }
    }
}
