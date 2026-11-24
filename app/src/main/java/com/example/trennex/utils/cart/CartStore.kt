package com.example.trennex.utils.cart

import android.content.Context
import com.example.trennex.data.local.AccountScope
import com.example.trennex.data.local.cart.AppDatabase
import com.example.trennex.repository.cart.CartRepository
import com.example.trennex.ui.cart.model.CartItemModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
                scope.launch {
                    AccountScope.owner.collectLatest { ownerId ->
                        cartRepository?.fetchAndSyncRemoteCart(ownerId)
                    }
                }
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
        val ownerId = AccountScope.currentOwner()
        scope.launch {
            repo().addItem(ownerId, item)
        }
    }

    suspend fun addItemAndAwait(item: CartItemModel) {
        val ownerId = AccountScope.currentOwner()
        withContext(Dispatchers.IO) {
            repo().addItem(ownerId, item)
        }
    }


    fun toggleSelection(cartKey: String , selected: Boolean){
        val ownerId = AccountScope.currentOwner()
        scope.launch {
            repo().toggleSelection(ownerId, cartKey,selected)
        }
    }
    fun toggleSelectAll(selected: Boolean){
       val ownerId = AccountScope.currentOwner()
       scope.launch {
           repo().toggleAllSelection(ownerId, selected)
       }
    }
    fun updateQuantity(cartKey: String, quantity: Int) {
       val ownerId = AccountScope.currentOwner()
       scope.launch {
           repo().updateQuantity(ownerId, cartKey,quantity)
       }
    }
    fun removeItem(cartKey: String){
        val ownerId = AccountScope.currentOwner()
        scope.launch {
            repo().removeItem(ownerId, cartKey)
        }
    }

    fun deleteSelectedItems(){
        deleteSelectedItems(AccountScope.currentOwner())
    }

    fun deleteSelectedItems(ownerId: String){
        scope.launch {
            repo().deleteSelectedItems(ownerId)
        }
    }
}
