package com.example.trennex.utils.wishlist

import android.content.Context
import com.example.trennex.analytics.AppAnalytics
import com.example.trennex.analytics.AnalyticsEvents
import com.example.trennex.analytics.AnalyticsItem
import com.example.trennex.analytics.AnalyticsNames
import com.example.trennex.data.local.AccountScope
import com.example.trennex.data.local.cart.AppDatabase
import com.example.trennex.repository.wishlist.WishlistRepository
import com.example.trennex.ui.wishlist.model.WishlistItemsModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

object WishListStore {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)


    @Volatile
    private var wishlistRepository: WishlistRepository? = null
    private fun repo(): WishlistRepository{
        return requireNotNull(wishlistRepository){
            "WishListStore not initialized. Call WishListStore.initialize(context) from Application.onCreate()."
        }
    }

    fun initialize(context: Context){
        if(wishlistRepository != null) return
        synchronized(this){
            if(wishlistRepository == null){
                wishlistRepository = WishlistRepository(AppDatabase.getInstance(context).wishlistDao())
                scope.launch {
                    AccountScope.owner.collectLatest { ownerId ->
                        wishlistRepository?.fetchAndSyncRemoteWishlist(ownerId)
                    }
                }
            }
        }
    }

    val items: Flow<List<WishlistItemsModel>> get() = repo().observeItems()
    fun addOrUpdate(item: WishlistItemsModel) {
        val ownerId = AccountScope.currentOwner()
        scope.launch {
            repo().addOrUpdate(ownerId, item)
            AppAnalytics.get().track(AnalyticsEvents.commerce(AnalyticsNames.ADD_TO_WISHLIST,
                listOf(AnalyticsItem("legacy:${item.id}", price = item.price))))
        }
    }
    fun removeItem(itemId: Int){
        val ownerId = AccountScope.currentOwner()
        scope.launch {
            repo().removeItem(ownerId, itemId)
        }
    }
}
