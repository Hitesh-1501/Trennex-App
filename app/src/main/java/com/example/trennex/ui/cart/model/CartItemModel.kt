package com.example.trennex.ui.cart.model

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
    val isSelected: Boolean = false
)
