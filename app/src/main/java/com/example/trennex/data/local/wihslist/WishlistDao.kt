package com.example.trennex.data.local.wihslist

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WishlistDao {

    @Query("SELECT * FROM wishlist_items WHERE ownerId = :ownerId ORDER BY id DESC")
    fun observeWishlistItems(ownerId: String): Flow<List<WishlistItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: WishlistItemEntity)

    @Query("DELETE FROM wishlist_items WHERE ownerId = :ownerId AND id = :itemId")
    suspend fun removeItem(ownerId: String, itemId: Int)


}
