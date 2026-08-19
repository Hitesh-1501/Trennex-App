package com.example.trennex.viewmodel.wishlist

import com.example.trennex.ui.wishlist.model.WishlistItemsModel
import com.example.trennex.ui.wishlist.model.CollectionModel

data class WishlistUiState(
    val items: List<WishlistItemsModel> = emptyList(),
    val collections: List<CollectionModel> = emptyList(),
    val isLoading: Boolean = false
)