package com.example.trennex.ui.product.model

data class ProductColorModel(
    val id : Int,
    val img : Int,
    val modelName : String,
    var isSelected : Boolean =  false
)