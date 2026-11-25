package com.example.trennex.data.local.cart

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CartDao {
    @Query("SELECT * FROM pending_cart_sync ORDER BY ownerId, cartKey")
    suspend fun pendingSync(): List<PendingCartSyncEntity>

    @Query("SELECT * FROM pending_cart_sync WHERE ownerId = :ownerId AND cartKey = :cartKey LIMIT 1")
    suspend fun pendingForKey(ownerId: String, cartKey: String): PendingCartSyncEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPending(item: PendingCartSyncEntity)

    @Query("DELETE FROM pending_cart_sync WHERE ownerId = :ownerId AND cartKey = :cartKey AND generation = :generation")
    suspend fun clearPending(ownerId: String, cartKey: String, generation: String)

    @Query("SELECT * FROM cart_items WHERE ownerId = :ownerId ORDER BY id DESC")
    fun observeCartItems(ownerId: String): Flow<List<CartItemEntity>>

    @Query("SELECT * FROM cart_items WHERE ownerId = :ownerId AND cartKey = :cartKey LIMIT 1")
    suspend fun getItemByKey(ownerId: String, cartKey: String): CartItemEntity?

    @Query("SELECT * FROM cart_items WHERE ownerId = :ownerId ORDER BY id DESC")
    suspend fun getAllCartItems(ownerId: String): List<CartItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: CartItemEntity)

    @Update
    suspend fun update(item: CartItemEntity)

    @Query("UPDATE cart_items SET isSelected = :isSelected WHERE ownerId = :ownerId AND cartKey = :cartKey")
    suspend fun updateSelection(ownerId: String, cartKey: String, isSelected: Boolean)

    @Query("UPDATE cart_items SET isSelected = :selected WHERE ownerId = :ownerId")
    suspend fun updateAllSelection(ownerId: String, selected: Boolean)

    @Query("UPDATE cart_items SET quantity = :quantity WHERE ownerId = :ownerId AND cartKey = :cartKey")
    suspend fun updateQuantity(ownerId: String, cartKey: String, quantity: Int)


    @Query("DELETE FROM cart_items WHERE ownerId = :ownerId AND cartKey = :cartKey")
    suspend fun removeItem(ownerId: String, cartKey: String)

    @Query("DELETE FROM cart_items WHERE ownerId = :ownerId AND isSelected = 1")
    suspend fun deleteSelectedItems(ownerId: String)
}
