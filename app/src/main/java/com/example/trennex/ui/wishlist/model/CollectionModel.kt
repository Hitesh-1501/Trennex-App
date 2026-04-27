package com.example.trennex.ui.wishlist.model

data class CollectionModel(
    val id: Long,
    val name: String,
    val items: List<WishlistItemsModel>
)