package com.example.trennex.ui.cart.model

import com.google.firebase.firestore.Exclude

data class CartItemModel(
    val id: Int,
    val title: String,
    val description : String,
    val mrp: Double,
    val price: Double,
    val rating: Double = 0.0,
    val ratingCount: Int = 0,
    val returnPolicy: String = "",
    val deliveryDetails: String = "",
    val imageUrl: String? = null,
    val imageRes: Int? = null,
    val quantity : Int = 1,
    val isSelected: Boolean = false,
    val productId: String? = null,
    val variantId: String? = null,
    val variantName: String? = null,
) {
    @get:Exclude
    val cartKey: String get() = variantId ?: "legacy:$id"
}
