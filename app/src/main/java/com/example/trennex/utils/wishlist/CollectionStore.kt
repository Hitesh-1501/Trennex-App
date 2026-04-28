package com.example.trennex.utils.wishlist

import android.content.Context
import com.example.trennex.data.local.cart.AppDatabase
import com.example.trennex.repository.wishlist.CollectionRepository
import com.example.trennex.ui.wishlist.model.CollectionModel
import com.example.trennex.ui.wishlist.model.WishlistItemsModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object CollectionStore {
    val scope = CoroutineScope(SupervisorJob()+ Dispatchers.IO)

    @Volatile
    private var collectionRepository: CollectionRepository? = null
    private fun repo(): CollectionRepository{
        return requireNotNull(collectionRepository){
            "CollectionStore not initialized. Call CollectionStore.initialize(context) from Application.onCreate()."
        }
    }

    fun initialize(context: Context){
        if(collectionRepository != null) return
        synchronized(this){
            collectionRepository = CollectionRepository(AppDatabase.getInstance(context).collectionDao())
        }
    }

    val collections: Flow<List<CollectionModel>> get() = repo().observeCollections()


    fun createCollection(name: String,items: List<WishlistItemsModel>){
        val trimmedName = name.trim().ifBlank { "New Collection" }
        val model = CollectionModel(
            id = System.currentTimeMillis(),
            name = trimmedName,
            items = items
        )
        scope.launch {
            repo().addOrUpdate(model)
        }
    }

    fun renameCollection(id: Long,newName: String){
        val updateName = newName.trim().ifBlank { "New Collection" }
        scope.launch {
            val latestCollections = repo().observeCollections().first()
            val target = latestCollections.firstOrNull(){it.id == id}?: return@launch
            repo().addOrUpdate(target.copy(name = updateName))
        }
    }
    fun removeCollection(id: Long){
        scope.launch {
            repo().removeCollection(id)
        }
    }
}
