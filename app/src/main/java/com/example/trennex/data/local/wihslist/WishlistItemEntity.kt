package com.example.trennex.data.local.wihslist

import androidx.room.Entity

@Entity(tableName = "wishlist_items", primaryKeys = ["ownerId", "id"])
data class WishlistItemEntity(
    val ownerId: String,
    val id: Int,
    val imageUrl: String,
    val title: String,
    val description: String,
    val mrp: Double,
    val price: Double,
    val rating: Double,
    val ratingCount: Int,
    val returnPolicy: String,
    val deliveryDetails: String
)
