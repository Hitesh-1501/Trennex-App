package com.example.trennex.data.local.wihslist

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {
    @Query("SELECT * FROM collections WHERE ownerId = :ownerId ORDER BY id DESC")
    fun observeCollections(ownerId: String): Flow<List<CollectionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(collection: CollectionEntity)

    @Query("DELETE FROM collections WHERE ownerId = :ownerId AND id = :collectionId")
    suspend fun removeCollection(ownerId: String, collectionId: Long)
        
}
