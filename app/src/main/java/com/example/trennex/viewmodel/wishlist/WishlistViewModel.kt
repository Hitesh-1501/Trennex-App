package com.example.trennex.viewmodel.wishlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trennex.ui.cart.model.CartItemModel
import com.example.trennex.ui.wishlist.model.WishlistItemsModel
import com.example.trennex.utils.cart.CartStore
import com.example.trennex.utils.wishlist.CollectionStore
import com.example.trennex.utils.wishlist.WishListStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class WishlistViewModel : ViewModel() {

    val uiState: StateFlow<WishlistUiState> = combine(
        WishListStore.items,
        CollectionStore.collections
    ) { items, collections ->
        WishlistUiState(
            items = items,
            collections = collections,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WishlistUiState(isLoading = true)
    )

    fun removeItemFromWishlist(itemId: Int) {
        WishListStore.removeItem(itemId)
    }

    fun addItemToCart(item: WishlistItemsModel) {
        CartStore.addItem(
            CartItemModel(
                id = item.id,
                title = item.title,
                description = item.description,
                mrp = item.mrp,
                price = item.price,
                rating = item.rating,
                ratingCount = item.ratingCount,
                returnPolicy = item.returnPolicy,
                deliveryDetails = item.deliveryDetails,
                imageUrl = item.imageUrl,
                quantity = 1,
                isSelected = true
            )
        )
        WishListStore.removeItem(item.id)
    }

    fun createCollection(name: String, items: List<WishlistItemsModel>) {
        CollectionStore.createCollection(name, items)
    }

    fun deleteCollection(collectionId: Long) {
        CollectionStore.removeCollection(collectionId)
    }

    fun renameCollection(collectionId: Long, newName: String) {
        CollectionStore.renameCollection(collectionId, newName)
    }

    fun removeItemsFromCollection(collectionId: Long, itemIds: List<Int>) {
        CollectionStore.removeItemsFromCollection(collectionId, itemIds)
    }
}