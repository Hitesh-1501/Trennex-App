package com.example.trennex.repository.wishlist

import com.example.trennex.data.local.wihslist.WishlistDao
import com.example.trennex.data.local.wihslist.WishlistItemEntity
import com.example.trennex.ui.wishlist.model.WishlistItemsModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WishlistRepository(
    private val dao: WishlistDao
){
    fun observeItems(): Flow<List<WishlistItemsModel>>{
        return dao.observeWishlistItems().map {entities ->
            entities.map{
                it.toModel()
            }
        }
    }
    suspend fun addOrUpdate(item: WishlistItemsModel){
        dao.upsert(item.toEntity())
    }
    suspend fun removeItem(itemId: Int){
        dao.removeItem(itemId)
    }

    private fun WishlistItemEntity.toModel(): WishlistItemsModel{
        return WishlistItemsModel(
            id = id,
            imageUrl = imageUrl,
            title = title,
            description = description,
            mrp = mrp,
            price = price,
            rating = rating,
            ratingCount = ratingCount,
            returnPolicy = returnPolicy,
            deliveryDetails = deliveryDetails
        )
    }

    private fun WishlistItemsModel.toEntity(): WishlistItemEntity{
        return WishlistItemEntity(
            id = id,
            imageUrl = imageUrl,
            title = title,
            description = description,
            mrp = mrp,
            price = price,
            rating = rating,
            ratingCount = ratingCount,
            returnPolicy = returnPolicy,
            deliveryDetails = deliveryDetails
        )
    }
}