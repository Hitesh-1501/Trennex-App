package com.example.trennex.utils.wishlist

import android.content.Context
import com.example.trennex.data.local.AccountScope
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
        val ownerId = AccountScope.currentOwner()
        val trimmedName = name.trim().ifBlank { "New Collection" }
        val model = CollectionModel(
            id = System.currentTimeMillis(),
            name = trimmedName,
            items = items
        )
        scope.launch {
            repo().addOrUpdate(ownerId, model)
        }
    }

    fun renameCollection(id: Long,newName: String){
        val ownerId = AccountScope.currentOwner()
        val updateName = newName.trim().ifBlank { "New Collection" }
        scope.launch {
            val latestCollections = repo().observeCollections(ownerId).first()
            val target = latestCollections.firstOrNull(){it.id == id}?: return@launch
            repo().addOrUpdate(ownerId, target.copy(name = updateName))
        }
    }
    fun removeCollection(id: Long){
        val ownerId = AccountScope.currentOwner()
        scope.launch {
            repo().removeCollection(ownerId, id)
        }
    }
    fun removeItemsFromCollection(collectionId: Long,itemIds: List<Int>){
        if(itemIds.isEmpty()) return
        val ownerId = AccountScope.currentOwner()
        scope.launch {
            val latestCollections = repo().observeCollections(ownerId).first()
            val target = latestCollections.firstOrNull(){it.id == collectionId}?: return@launch
            val updateItems = target.items.filterNot { it.id in itemIds.toSet() }
            if(updateItems.isEmpty()){
                repo().removeCollection(ownerId, collectionId)
            }else{
                repo().addOrUpdate(ownerId, target.copy(items = updateItems))
            }
        }
    }
}
