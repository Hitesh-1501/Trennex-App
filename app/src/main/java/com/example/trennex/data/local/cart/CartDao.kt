package com.example.trennex.data.local.cart

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CartDao {
    @Query("SELECT * FROM cart_items WHERE ownerId = :ownerId ORDER BY id DESC")
    fun observeCartItems(ownerId: String): Flow<List<CartItemEntity>>

    @Query("SELECT * FROM cart_items WHERE ownerId = :ownerId AND id = :id LIMIT 1")
    suspend fun getItemById(ownerId: String, id: Int): CartItemEntity?

    @Query("SELECT * FROM cart_items WHERE ownerId = :ownerId ORDER BY id DESC")
    suspend fun getAllCartItems(ownerId: String): List<CartItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: CartItemEntity)

    @Update
    suspend fun update(item: CartItemEntity)

    @Query("UPDATE cart_items SET isSelected = :isSelected WHERE ownerId = :ownerId AND id = :itemId")
    suspend fun updateSelection(ownerId: String, itemId: Int, isSelected: Boolean)

    @Query("UPDATE cart_items SET isSelected = :selected WHERE ownerId = :ownerId")
    suspend fun updateAllSelection(ownerId: String, selected: Boolean)

    @Query("UPDATE cart_items SET quantity = :quantity WHERE ownerId = :ownerId AND id = :itemId")
    suspend fun updateQuantity(ownerId: String, itemId: Int, quantity: Int)


    @Query("DELETE FROM cart_items WHERE ownerId = :ownerId AND id = :itemId")
    suspend fun removeItem(ownerId: String, itemId: Int)

    @Query("DELETE FROM cart_items WHERE ownerId = :ownerId AND isSelected = 1")
    suspend fun deleteSelectedItems(ownerId: String)
}
