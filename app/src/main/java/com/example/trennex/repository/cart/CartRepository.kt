package com.example.trennex.repository.cart

import com.example.trennex.data.local.cart.CartDao
import com.example.trennex.data.local.cart.CartItemEntity
import com.example.trennex.ui.cart.model.CartItemModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CartRepository(
    private val dao: CartDao
){
    fun observeItems(): Flow<List<CartItemModel>>{
        return dao.observeCartItems().map { entities ->
            entities.map{
                it.toModel()
            }
        }
    }
    suspend fun addItem(item: CartItemModel){
        val safeQuantity =  item.quantity.coerceAtLeast(1)
        val existing = dao.getItemById(item.id)
        if(existing == null){
            dao.upsert(item.copy(quantity = safeQuantity, isSelected = true).toEntity())
        }else{
            dao.update(
                existing.copy(
                    title = item.title,
                    description = item.description,
                    mrp = item.mrp,
                    price = item.price,
                    rating = item.rating,
                    ratingCount = item.ratingCount,
                    returnPolicy = item.returnPolicy,
                    deliveryDetails = item.deliveryDetails,
                    imageUrl = item.imageUrl,
                    imageRes = item.imageRes,
                    quantity = existing.quantity + safeQuantity,
                    isSelected = true
                )
            )
        }
    }

    suspend fun toggleSelection(itemId: Int, selected: Boolean){
        dao.updateSelection(itemId,selected)
    }

    suspend fun toggleAllSelection(selected: Boolean){
        dao.updateAllSelection(selected)
    }

    suspend fun updateQuantity(itemId: Int, quantity: Int){
        dao.updateQuantity(itemId,quantity.coerceAtLeast(1))
    }

    suspend fun removeItem(itemId: Int){
        dao.removeItem(itemId)
    }

    private fun CartItemEntity.toModel(): CartItemModel{
        return CartItemModel(
            id = id,
            title = title,
            description = description,
            mrp = mrp,
            price = price,
            rating = rating,
            ratingCount = ratingCount,
            returnPolicy = returnPolicy,
            deliveryDetails = deliveryDetails,
            imageUrl = imageUrl,
            imageRes = imageRes,
            quantity = quantity,
            isSelected = isSelected
        )
    }

    private fun CartItemModel.toEntity(): CartItemEntity{
        return CartItemEntity(
            id = id,
            title = title,
            description = description,
            mrp = mrp,
            price = price,
            rating = rating,
            ratingCount = ratingCount,
            returnPolicy = returnPolicy,
            deliveryDetails = deliveryDetails,
            imageUrl = imageUrl,
            imageRes = imageRes,
            quantity = quantity,
            isSelected = isSelected
        )
    }
}