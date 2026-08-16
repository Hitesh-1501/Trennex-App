package com.example.trennex.viewmodel.wishlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trennex.ui.cart.model.CartItemModel
import com.example.trennex.ui.wishlist.model.CollectionModel
import com.example.trennex.ui.wishlist.model.WishlistItemsModel
import com.example.trennex.utils.cart.CartStore
import com.example.trennex.utils.wishlist.CollectionStore
import com.example.trennex.utils.wishlist.WishListStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class WishlistViewModel : ViewModel() {

    private val _isSelectionMode = MutableStateFlow(false)
    private val _selectedItemIds = MutableStateFlow(emptySet<Int>())

    val uiState: StateFlow<WishlistUiState> = combine(
        WishListStore.items,
        CollectionStore.collections,
        _isSelectionMode,
        _selectedItemIds
    ) { items, collections, isSelectionMode, selectedItemIds ->
        WishlistUiState(
            items = items,
            collections = collections,
            isSelectionMode = isSelectionMode,
            selectedItemIds = selectedItemIds
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WishlistUiState()
    )

    fun toggleSelectionMode(enabled: Boolean) {
        _isSelectionMode.update { enabled }
        if (!enabled) {
            _selectedItemIds.update { emptySet() }
        }
    }

    fun toggleItemSelection(itemId: Int) {
        _selectedItemIds.update { current ->
            if (current.contains(itemId)) current - itemId
            else current + itemId
        }
    }

    fun clearSelection() {
        _selectedItemIds.update { emptySet() }
    }

    fun addToCart(item: WishlistItemsModel) {
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

    fun removeItem(itemId: Int) {
        WishListStore.removeItem(itemId)
    }

    fun removeSelectedItems() {
        val selected = _selectedItemIds.value
        selected.forEach { WishListStore.removeItem(it) }
        toggleSelectionMode(false)
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