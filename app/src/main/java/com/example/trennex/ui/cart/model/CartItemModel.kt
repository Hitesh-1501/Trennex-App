package com.example.trennex.ui.cart.model

data class CartItemModel(
    val id : Int,
    val image : Int,
    val title: String,
    val description : String,
    val mrp : Int,
    val Price : Int,
    var quantity : Int = 1,
)
