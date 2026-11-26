package com.example.trennex.utils.cart

import android.content.Context
import com.example.trennex.analytics.AppAnalytics
import com.example.trennex.analytics.AnalyticsEvents
import com.example.trennex.analytics.AnalyticsNames
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object CartStore {
    enum class RemoteStatus { LOADING, READY, ERROR }
    private val mutableRemoteStatus = MutableStateFlow(RemoteStatus.LOADING)
    val remoteStatus: StateFlow<RemoteStatus> = mutableRemoteStatus
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var cartRepository: CartRepository? = null
    private var appContext: Context? = null

    private fun repo(): CartRepository {
        return  requireNotNull(cartRepository){
            "CartStore not initialized. Call CartStore.initialize(context) from Application.onCreate()."
        }
    }
    fun initialize(context: Context){
        if(cartRepository != null) return
        synchronized(this){
            if(cartRepository == null) {
                appContext = context.applicationContext
                cartRepository =
                    CartRepository(AppDatabase.getInstance(context))
                scope.launch {
                    AccountScope.owner.collectLatest { ownerId ->
                        mutableRemoteStatus.value = RemoteStatus.LOADING
                        CartSyncWorker.schedule(context.applicationContext)
                        val loaded = cartRepository?.fetchAndSyncRemoteCart(ownerId) == true
                        if (AccountScope.currentOwner() == ownerId) {
                            mutableRemoteStatus.value = if (loaded) RemoteStatus.READY else RemoteStatus.ERROR
                        }
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
            AppAnalytics.get().track(AnalyticsEvents.commerce(AnalyticsNames.ADD_TO_CART,
                listOf(AnalyticsEvents.item(item)), value = item.price * item.quantity))
            scheduleSync()
        }
    }

    suspend fun addItemAndAwait(item: CartItemModel) {
        val ownerId = AccountScope.currentOwner()
        withContext(Dispatchers.IO) {
            repo().addItemLocally(ownerId, item)
            AppAnalytics.get().track(AnalyticsEvents.commerce(AnalyticsNames.ADD_TO_CART,
                listOf(AnalyticsEvents.item(item)), value = item.price * item.quantity))
            scheduleSync()
        }
    }


    fun toggleSelection(cartKey: String , selected: Boolean){
        val ownerId = AccountScope.currentOwner()
        scope.launch {
            repo().toggleSelection(ownerId, cartKey,selected)
            scheduleSync()
        }
    }
    fun toggleSelectAll(selected: Boolean){
       val ownerId = AccountScope.currentOwner()
       scope.launch {
           repo().toggleAllSelection(ownerId, selected)
           scheduleSync()
       }
    }
    fun updateQuantity(cartKey: String, quantity: Int) {
       val ownerId = AccountScope.currentOwner()
       scope.launch {
           repo().updateQuantity(ownerId, cartKey,quantity)
           scheduleSync()
       }
    }
    fun removeItem(cartKey: String){
        val ownerId = AccountScope.currentOwner()
        scope.launch {
            val removed = repo().getLocalItem(ownerId, cartKey)
            repo().removeItem(ownerId, cartKey)
            removed?.let { AppAnalytics.get().track(AnalyticsEvents.commerce(AnalyticsNames.REMOVE_FROM_CART,
                listOf(AnalyticsEvents.item(it)), value = it.price * it.quantity)) }
            scheduleSync()
        }
    }

    fun deleteSelectedItems(){
        deleteSelectedItems(AccountScope.currentOwner())
    }

    fun deleteSelectedItems(ownerId: String){
        scope.launch {
            repo().deleteSelectedItems(ownerId)
            scheduleSync()
        }
    }

    fun refreshRemote() {
        val ownerId = AccountScope.currentOwner()
        mutableRemoteStatus.value = RemoteStatus.LOADING
        scope.launch {
            val loaded = repo().fetchAndSyncRemoteCart(ownerId)
            if (AccountScope.currentOwner() == ownerId) {
                mutableRemoteStatus.value = if (loaded) RemoteStatus.READY else RemoteStatus.ERROR
            }
            scheduleSync()
        }
    }

    private fun scheduleSync() {
        appContext?.let(CartSyncWorker::schedule)
    }
}
