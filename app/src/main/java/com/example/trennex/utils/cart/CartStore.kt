package com.example.trennex.utils.cart

import android.content.Context
import com.example.trennex.data.local.cart.AppDatabase
import com.example.trennex.repository.cart.CartRepository
import com.example.trennex.ui.cart.model.CartItemModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

object CartStore {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var cartRepository: CartRepository? = null

    private fun repo(): CartRepository {
        return  requireNotNull(cartRepository){
            "CartStore not initialized. Call CartStore.initialize(context) from Application.onCreate()."
        }
    }
    fun initialize(context: Context){
        if(cartRepository != null) return
        synchronized(this){
            if(cartRepository == null) {
                cartRepository =
                    CartRepository(AppDatabase.Companion.getInstance(context).cartDao())
            }
        }
    }

    val items: Flow<List<CartItemModel>>
        get() = repo().observeItems()

    val totalQuantity: Flow<Int>
        get() = items
            .map { list -> list.sumOf { it.quantity }}
            .distinctUntilChanged()

    fun addItem(item: CartItemModel){
        scope.launch {
            repo().addItem(item)
        }
    }


    fun toggleSelection(itemId: Int , selected: Boolean){
        scope.launch {
            repo().toggleSelection(itemId,selected)
        }
    }
    fun toggleSelectAll(selected: Boolean){
       scope.launch {
           repo().toggleAllSelection(selected)
       }
    }
    fun updateQuantity(itemId: Int, quantity: Int) {
       scope.launch {
           repo().updateQuantity(itemId,quantity)
       }
    }
    fun removeItem(itemId: Int){
        scope.launch {
            repo().removeItem(itemId)
        }
    }

    fun deleteSelectedItems(){
        scope.launch {
            repo().deleteSelectedItems()
        }
    }
}