package com.example.trennex.ui.wishlist

import com.example.trennex.ui.wishlist.model.WishlistItemsModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object WishListStore {
    private val _items = MutableStateFlow<List<WishlistItemsModel>>(emptyList())
    val items: StateFlow<List<WishlistItemsModel>> = _items.asStateFlow()

    fun addOrUpdate(item: WishlistItemsModel) {
        _items.update { current ->
            val index = current.indexOfFirst { it.id == item.id }
            if (index >= 0) {
                current.toMutableList().apply { this[index] = item }
            } else {
                current + item
            }
        }
    }

    fun removeById(productId: Int) {
        _items.update { current -> current.filterNot { it.id == productId } }
    }

    fun contains(productId: Int): Boolean = _items.value.any { it.id == productId }
}