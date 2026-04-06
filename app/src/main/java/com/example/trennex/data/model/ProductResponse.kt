package com.example.trennex.data.model

data class ProductResponse(
    val id: Int,
    val title: String,
    val description: String,
    val price: Double,
    val images: List<String>,
    val discountPercentage: Double,
    val thumbnail: String,
    val reviews: List<ReviewResponse>,
    val shippingInformation: String,
    val warrantyInformation: String,
    val returnPolicy: String
)