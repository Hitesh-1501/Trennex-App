package com.example.trennex.data.local.cart

import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "cart_items")
data class CartItemEntity(
    @PrimaryKey
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
    val isSelected: Boolean = false
)