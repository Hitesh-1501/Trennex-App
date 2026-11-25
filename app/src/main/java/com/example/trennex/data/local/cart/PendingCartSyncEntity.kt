package com.example.trennex.data.local.cart

import androidx.room.Entity

/** One idempotent remote operation per owner and cart key. A deletion remains as a tombstone. */
@Entity(tableName = "pending_cart_sync", primaryKeys = ["ownerId", "cartKey"])
data class PendingCartSyncEntity(
    val ownerId: String,
    val cartKey: String,
    val legacyId: Int,
    val variantId: String?,
    val operation: String,
    val generation: String,
)
