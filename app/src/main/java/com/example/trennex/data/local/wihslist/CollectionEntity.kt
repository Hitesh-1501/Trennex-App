package com.example.trennex.data.local.wihslist

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "collections")
data class CollectionEntity(
    @PrimaryKey
    val id: Long,
    val name: String,
    val itemJson: String
)