package com.example.trennex.repository.wishlist

import com.example.trennex.data.local.wihslist.CollectionDao
import com.example.trennex.data.local.wihslist.CollectionEntity
import com.example.trennex.data.local.AccountScope
import com.example.trennex.ui.wishlist.model.CollectionModel
import com.example.trennex.ui.wishlist.model.WishlistItemsModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.ExperimentalCoroutinesApi

class CollectionRepository(
    private val dao: CollectionDao,
    private val gson: Gson = Gson()
){
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeCollections(): Flow<List<CollectionModel>>{
        return AccountScope.owner.flatMapLatest { ownerId ->
            observeCollections(ownerId).onStart { emit(emptyList()) }
        }
    }

    fun observeCollections(ownerId: String): Flow<List<CollectionModel>> =
        dao.observeCollections(ownerId).map { entities -> entities.map { it.toModel() } }

    suspend fun addOrUpdate(ownerId: String, collection: CollectionModel){
        dao.upsert(collection.toEntity(ownerId))
    }

    suspend fun removeCollection(ownerId: String, collectionId: Long){
        dao.removeCollection(ownerId, collectionId)
    }


    private fun CollectionEntity.toModel(): CollectionModel{
        val listType = object : TypeToken<List<WishlistItemsModel>>(){}.type
        return CollectionModel(
            id = id,
            name = name,
            items = gson.fromJson(itemJson,listType)?: emptyList()
        )
    }
    private fun CollectionModel.toEntity(ownerId: String): CollectionEntity{
        return CollectionEntity(
            ownerId = ownerId,
            id = id,
            name = name,
            itemJson = gson.toJson(items)
        )
    }
}
