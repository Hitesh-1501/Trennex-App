package com.example.trennex.ui.wishlist.model

data class WishlistItemsModel(
    val id : Int,
    val imageUrl : String,
    val title : String,
    val description: String,
    val mrp: Double,
    val price : Double,
    val rating: Double,
    val ratingCount: Int,
    val returnPolicy: String,
    val deliveryDetails: String
)
