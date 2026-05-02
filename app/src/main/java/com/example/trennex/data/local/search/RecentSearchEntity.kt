package com.example.trennex.data.local.search

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_searches")
data class RecentSearchEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val query: String,
    val timeStamp: Long = System.currentTimeMillis()
)