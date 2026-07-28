package com.example.trennex

import android.app.Application
import com.example.trennex.utils.cart.CartStore
import com.example.trennex.utils.wishlist.CollectionStore
import com.example.trennex.utils.wishlist.WishListStore

import com.example.trennex.R
import com.google.android.libraries.places.api.Places

class TrenNexApp: Application(){
    override fun onCreate() {
        super.onCreate()
        CartStore.initialize(this)
        WishListStore.initialize(this)
        CollectionStore.initialize(this)
        
        val apiKey = getString(R.string.MAP_API_KEY)
        if (apiKey.isNotBlank()) {
            Places.initialize(this, apiKey)
        }
    }
}