package com.example.trennex.repository.wishlist

import com.example.trennex.data.local.wihslist.CollectionDao
import com.example.trennex.data.local.wihslist.CollectionEntity
import com.example.trennex.ui.wishlist.model.CollectionModel
import com.example.trennex.ui.wishlist.model.WishlistItemsModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CollectionRepository(
    private val dao: CollectionDao,
    private val gson: Gson = Gson()
){
    fun observeCollections(): Flow<List<CollectionModel>>{
        return dao.observeCollections().map {entities ->
            entities.map { it.toModel() }
        }
    }

    suspend fun addOrUpdate(collection: CollectionModel){
        dao.upsert(collection.toEntity())
    }

    suspend fun removeCollection(collectionId: Long){
        dao.removeCollection(collectionId)
    }


    private fun CollectionEntity.toModel(): CollectionModel{
        val listType = object : TypeToken<List<WishlistItemsModel>>(){}.type
        return CollectionModel(
            id = id,
            name = name,
            items = gson.fromJson(itemJson,listType)?: emptyList()
        )
    }
    private fun CollectionModel.toEntity(): CollectionEntity{
        return CollectionEntity(
            id = id,
            name = name,
            itemJson = gson.toJson(items)
        )
    }
}