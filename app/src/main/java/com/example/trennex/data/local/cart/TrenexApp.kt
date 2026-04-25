package com.example.trennex.data.local.cart

import android.app.Application
import com.example.trennex.utils.cart.CartStore
import com.example.trennex.utils.wishlist.WishListStore

class TrenexApp: Application(){
    override fun onCreate() {
        super.onCreate()
        CartStore.initialize(this)
        WishListStore.initialize(this)
    }
}