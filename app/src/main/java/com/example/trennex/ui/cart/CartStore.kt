package com.example.trennex.ui.cart

import com.example.trennex.ui.cart.model.CartItemModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object CartStore {
    private val _items = MutableStateFlow<List<CartItemModel>>(emptyList())
    val items: StateFlow<List<CartItemModel>> = _items.asStateFlow()

    fun addItem(item: CartItemModel){
        _items.update {current ->
            val index = current.indexOfFirst {
                it.id == item.id
            }
            if(index == -1){
                current + item.copy(quantity = item.quantity.coerceAtLeast(1), isSelected = true)
            }else{
                current.toMutableList().apply {
                    val existing = this[index]
                    this[index] = existing.copy(
                        title = item.title,
                        description = item.description,
                        mrp = item.mrp,
                        price = item.price,
                        rating = item.rating,
                        ratingCount = item.ratingCount,
                        returnPolicy = item.returnPolicy,
                        deliveryDetails = item.deliveryDetails,
                        imageUrl = item.imageUrl,
                        imageRes = item.imageRes,
                        quantity = existing.quantity + item.quantity.coerceAtLeast(1),
                        isSelected = true
                    )
                }
            }
        }
    }

    fun toggleSelection(itemId: Int , selected: Boolean){
        _items.update {list->
            list.map { if(it.id == itemId) it.copy(isSelected = selected) else it }
        }
    }


    fun toggleSelectAll(selected: Boolean){
        _items.update {list->
            list.map { it.copy(isSelected = selected) }
        }
    }
    fun updateQuantity(itemId: Int, quantity: Int) {
        val safeQuantity = quantity.coerceAtLeast(1)
        _items.update { list ->
            list.map { if (it.id == itemId) it.copy(quantity = safeQuantity) else it }
        }
    }
}