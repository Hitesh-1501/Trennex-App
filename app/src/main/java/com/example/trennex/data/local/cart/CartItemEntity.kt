package com.example.trennex.data.local.cart

import androidx.room.Entity
@Entity(tableName = "cart_items", primaryKeys = ["ownerId", "cartKey"])
data class CartItemEntity(
    val ownerId: String,
    val id: Int,
    val title: String,
    val description : String,
    val mrp: Double,
    val price: Double,
    val rating: Double,
    val ratingCount: Int,
    val returnPolicy: String,
    val deliveryDetails: String,
    val imageUrl: String?,
    val imageRes: Int?,
    val quantity : Int,
    val isSelected: Boolean = false,
    val cartKey: String = "legacy:$id",
    val productId: String? = null,
    val variantId: String? = null,
    val variantName: String? = null,
)
