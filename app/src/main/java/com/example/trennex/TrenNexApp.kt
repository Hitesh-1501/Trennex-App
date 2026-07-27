package com.example.trennex

import android.app.Application
import com.example.trennex.utils.cart.CartStore
import com.example.trennex.utils.wishlist.CollectionStore
import com.example.trennex.utils.wishlist.WishListStore

class TrenNexApp: Application(){
    override fun onCreate() {
        super.onCreate()
        CartStore.initialize(this)
        WishListStore.initialize(this)
        CollectionStore.initialize(this)
    }
}