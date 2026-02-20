package com.example.trennex.ui.product.model

data class VariantModel(
    val id : Int,
    val variant: String,
    val Price : String,
    val mrpPrice: String,
    var isSelected : Boolean = false
)