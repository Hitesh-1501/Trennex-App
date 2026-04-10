package com.example.trennex.data.model

import com.google.gson.annotations.SerializedName

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
    val returnPolicy: String,
    val category: String? = null,
    val brand: String? = null,
    val sku: String? = null,
    val availabilityStatus: String? = null,
    val weight: Int? = null,
    val tags: List<String>? = null,
    val dimensions: ProductDimensions? = null,
    @SerializedName(value = "colors", alternate =  ["colorOptions"])
    val colors: List<String>? = null,
    @SerializedName(value = "variants", alternate = ["storageOptions", "sizeOptions"])
    val variants: List<String>? = null
)