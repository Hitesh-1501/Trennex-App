package com.example.trennex.data.local.wihslist

import androidx.room.Entity

@Entity(tableName = "collections", primaryKeys = ["ownerId", "id"])
data class CollectionEntity(
    val ownerId: String,
    val id: Long,
    val name: String,
    val itemJson: String
)
