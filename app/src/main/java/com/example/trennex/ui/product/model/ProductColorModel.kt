package com.example.trennex.ui.product.model

data class ProductColorModel(
    val id : Int,
    val imageUrl : String,
    val modelName : String,
    var isSelected : Boolean =  false
)