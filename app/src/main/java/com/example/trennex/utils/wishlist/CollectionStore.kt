package com.example.trennex.utils.wishlist

import com.example.trennex.ui.wishlist.model.CollectionModel
import com.example.trennex.ui.wishlist.model.WishlistItemsModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object CollectionStore {
    private val _collections = MutableStateFlow<List<CollectionModel>>(emptyList())
    val collections: StateFlow<List<CollectionModel>> = _collections.asStateFlow()

    fun createCollection(name: String,items: List<WishlistItemsModel>){
        val trimmedName = name.trim().ifBlank { "New Collection" }
        val model = CollectionModel(
            id = System.currentTimeMillis(),
            name = trimmedName,
            items = items
        )
        _collections.value = _collections.value + model
    }

    fun renameCollection(id: Long,newName: String){
        val updateName = newName.trim().ifBlank { "New Collection" }
        _collections.value = _collections.value.map {collection->
            if(collection.id == id){
                collection.copy(name = updateName)
            }else{
                collection
            }
        }
    }
    fun removeCollection(id: Long){
        _collections.value = _collections.value.filterNot { it.id == id }
    }
}
