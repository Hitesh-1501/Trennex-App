package com.example.trennex.data.local.cart

import android.app.Application
import com.example.trennex.utils.cart.CartStore

class TrenexApp: Application(){
    override fun onCreate() {
        super.onCreate()
        CartStore.initialize(this)
    }
}