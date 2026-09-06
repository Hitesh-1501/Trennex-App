package com.example.trennex.ui.profile.model

import com.google.firebase.Timestamp

data class OrderModel(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val imageUrl: String? = null,
    val imageRes: Int? = null,
    val price: Double = 0.0,
    val quantity: Int = 1,
    val status: String = "PENDING", // PENDING, DELIVERED
    val orderDate: Timestamp = Timestamp.now(),
    val expectedDeliveryDate: Timestamp = Timestamp.now()
)
