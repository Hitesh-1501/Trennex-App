package com.example.trennex

import android.app.Application
import android.util.Log
import com.example.trennex.utils.cart.CartStore
import com.example.trennex.utils.wishlist.CollectionStore
import com.example.trennex.utils.wishlist.WishListStore

import com.example.trennex.R
import com.google.android.libraries.places.api.Places
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import com.google.firebase.storage.FirebaseStorage

class TrenNexApp: Application(){
    override fun onCreate() {
        super.onCreate()
        configureFirebaseEmulators()
        CartStore.initialize(this)
        WishListStore.initialize(this)
        CollectionStore.initialize(this)
        
        val apiKey = getString(R.string.MAP_API_KEY)
        if (apiKey.isNotBlank()) {
            Places.initialize(this, apiKey)
        }
    }

    private fun configureFirebaseEmulators() {
        if (!BuildConfig.USE_FIREBASE_EMULATORS) return

        val host = BuildConfig.FIREBASE_EMULATOR_HOST
        val firestore = FirebaseFirestore.getInstance()
        firestore.firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
            .build()
        firestore.useEmulator(host, FIRESTORE_EMULATOR_PORT)
        FirebaseStorage.getInstance().useEmulator(host, STORAGE_EMULATOR_PORT)

        if (BuildConfig.USE_FIREBASE_AUTH_EMULATOR) {
            FirebaseAuth.getInstance().useEmulator(host, AUTH_EMULATOR_PORT)
        }

        Log.i(
            TAG,
            "Firebase debug emulators enabled at $host " +
                "(Auth emulator: ${BuildConfig.USE_FIREBASE_AUTH_EMULATOR})"
        )
    }

    private companion object {
        const val TAG = "TrenNexApp"
        const val AUTH_EMULATOR_PORT = 9099
        const val FIRESTORE_EMULATOR_PORT = 8080
        const val STORAGE_EMULATOR_PORT = 9199
    }
}
