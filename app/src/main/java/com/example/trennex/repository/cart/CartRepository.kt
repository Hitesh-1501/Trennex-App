package com.example.trennex.repository.cart

import android.util.Log
import com.example.trennex.data.local.AccountScope
import com.example.trennex.data.local.cart.CartDao
import com.example.trennex.data.local.cart.CartItemEntity
import com.example.trennex.data.local.cart.AppDatabase
import com.example.trennex.data.local.cart.PendingCartSyncEntity
import androidx.room.withTransaction
import com.example.trennex.repository.product.CatalogItemResolver
import com.example.trennex.ui.cart.model.CartItemModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import java.util.UUID

class CartRepository(
    private val database: AppDatabase,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {
    private val dao: CartDao = database.cartDao()
    private val mutationLock = Mutex()
    private val catalogResolver = CatalogItemResolver(firestore)

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeItems(): Flow<List<CartItemModel>> = AccountScope.owner.flatMapLatest { ownerId ->
        dao.observeCartItems(ownerId).map { entities -> entities.map { it.toModel() } }
            .onStart { emit(emptyList()) }
    }

    suspend fun fetchAndSyncRemoteCart(ownerId: String): Boolean {
        val uid = AccountScope.uid(ownerId) ?: return true
        if (auth.currentUser?.uid != uid) return false
        try {
            val newItems = withTimeout(15_000) {
                firestore.userCollection(uid, NEW_CART_COLLECTION)
                    .get()
                    .await()
                    .documents
                    .mapNotNull { document -> remoteCartItem(document) }
            }

            if (newItems.isNotEmpty()) {
                newItems.forEach { item ->
                    if (dao.pendingForKey(ownerId, item.cartKey) != null) return@forEach
                    dao.getItemByKey(ownerId, "legacy:${item.id}")?.let {
                        dao.removeItem(ownerId, it.cartKey)
                    }
                    dao.upsert(item.toEntity(ownerId))
                }
                return true
            }

            val legacyItems = withTimeout(15_000) {
                firestore.userCollection(uid, LEGACY_CART_COLLECTION)
                    .get()
                    .await()
                    .documents
                    .mapNotNull { document ->
                        runCatching { document.toObject(CartItemModel::class.java) }.getOrNull()
                    }
            }

            for (item in legacyItems) {
                val migrated = item.copy(quantity = item.quantity.coerceIn(1, MAX_QUANTITY))
                if (dao.pendingForKey(ownerId, migrated.cartKey) != null) continue
                dao.upsert(migrated.toEntity(ownerId))
                runCatching { withTimeout(20_000) { syncItemToFirestore(ownerId, migrated) } }
                    .onFailure { queue(ownerId, migrated.cartKey, migrated.id, migrated.variantId, UPSERT) }
            }
            return true
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to sync remote cart", exception)
            return false
        }
    }

    suspend fun addItem(ownerId: String, item: CartItemModel) {
        addItemLocally(ownerId, item)
    }

    suspend fun getLocalItem(ownerId: String, cartKey: String): CartItemModel? =
        dao.getItemByKey(ownerId, cartKey)?.toModel()

    suspend fun addItemLocally(ownerId: String, item: CartItemModel): CartItemModel = mutationLock.withLock {
        database.withTransaction {
            val updated = mergeItem(ownerId, item)
            dao.upsert(updated.toEntity(ownerId))
            queue(ownerId, updated.cartKey, updated.id, updated.variantId, UPSERT)
            updated
        }
    }

    private suspend fun mergeItem(ownerId: String, item: CartItemModel): CartItemModel {
        val safeQuantity = item.quantity.coerceIn(1, MAX_QUANTITY)
        val existing = dao.getItemByKey(ownerId, item.cartKey)
        val updatedItem = if (existing == null) {
            item.copy(quantity = safeQuantity, isSelected = true)
        } else {
            existing.toModel().copy(
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
                productId = item.productId,
                variantId = item.variantId,
                variantName = item.variantName,
                quantity = (existing.quantity + safeQuantity).coerceAtMost(MAX_QUANTITY),
                isSelected = true,
            )
        }

        return updatedItem
    }

    suspend fun toggleSelection(ownerId: String, cartKey: String, selected: Boolean) {
        mutationLock.withLock { database.withTransaction {
            dao.updateSelection(ownerId, cartKey, selected)
            dao.getItemByKey(ownerId, cartKey)?.let { queue(ownerId, cartKey, it.id, it.variantId, UPSERT) }
        } }
    }

    suspend fun toggleAllSelection(ownerId: String, selected: Boolean) {
        mutationLock.withLock { database.withTransaction {
            dao.updateAllSelection(ownerId, selected)
            dao.getAllCartItems(ownerId).forEach { queue(ownerId, it.cartKey, it.id, it.variantId, UPSERT) }
        } }
    }

    suspend fun updateQuantity(ownerId: String, cartKey: String, quantity: Int) {
        mutationLock.withLock { database.withTransaction {
            dao.updateQuantity(ownerId, cartKey, quantity.coerceIn(1, MAX_QUANTITY))
            dao.getItemByKey(ownerId, cartKey)?.let { queue(ownerId, cartKey, it.id, it.variantId, UPSERT) }
        } }
    }

    suspend fun removeItem(ownerId: String, cartKey: String) {
        mutationLock.withLock { database.withTransaction {
            val item = dao.getItemByKey(ownerId, cartKey) ?: return@withTransaction
            dao.removeItem(ownerId, cartKey)
            queue(ownerId, cartKey, item.id, item.variantId, DELETE)
        } }
    }

    suspend fun deleteSelectedItems(ownerId: String) {
        mutationLock.withLock { database.withTransaction {
            dao.getAllCartItems(ownerId).filter { it.isSelected }.forEach {
                queue(ownerId, it.cartKey, it.id, it.variantId, DELETE)
            }
            dao.deleteSelectedItems(ownerId)
        } }
    }

    private suspend fun queue(ownerId: String, cartKey: String, legacyId: Int, variantId: String?, operation: String) {
        if (AccountScope.uid(ownerId) == null) return
        dao.upsertPending(PendingCartSyncEntity(ownerId, cartKey, legacyId, variantId, operation, UUID.randomUUID().toString()))
    }

    /** Absolute set/delete operations are safe to replay after process death or a network timeout. */
    suspend fun drainPending(): Boolean {
        var hadFailure = false
        for (pending in dao.pendingSync()) {
            val uid = AccountScope.uid(pending.ownerId) ?: continue
            if (auth.currentUser?.uid != uid) continue
            try {
                withTimeout(20_000) {
                    if (pending.operation == DELETE) {
                        pending.variantId?.let { firestore.userCollection(uid, NEW_CART_COLLECTION).document(it).delete().await() }
                        firestore.userCollection(uid, LEGACY_CART_COLLECTION).document(pending.legacyId.toString()).delete().await()
                    } else {
                        val latest = dao.getItemByKey(pending.ownerId, pending.cartKey)?.toModel()
                        if (latest != null) syncItemToFirestore(pending.ownerId, latest)
                    }
                }
                if (auth.currentUser?.uid != uid) continue
                dao.clearPending(pending.ownerId, pending.cartKey, pending.generation)
            } catch (error: Exception) {
                Log.w(TAG, "Pending cart sync will retry", error)
                hadFailure = true
            }
        }
        return !hadFailure
    }

    private suspend fun syncItemToFirestore(ownerId: String, item: CartItemModel) {
        val uid = AccountScope.uid(ownerId) ?: return
        if (auth.currentUser?.uid != uid) return
        try {
            val reference = if (item.productId != null && item.variantId != null) {
                catalogResolver.resolve(item.productId, item.variantId)
            } else catalogResolver.resolveLegacyId(item.id)
            if (item.variantId != null && reference == null) {
                Log.w(TAG, "Selected variant ${item.variantId} is unavailable; keeping local cart row")
                throw IllegalStateException("Selected variant is unavailable")
            }
            if (reference != null) {
                val newDocument = firestore.userCollection(uid, NEW_CART_COLLECTION)
                    .document(reference.variant.documentId)
                newDocument.set(
                    mapOf(
                        "schemaVersion" to 1,
                        "productId" to reference.product.documentId,
                        "variantId" to reference.variant.documentId,
                        "quantity" to item.quantity.coerceIn(1, MAX_QUANTITY),
                        "selected" to item.isSelected,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
                firestore.userCollection(uid, LEGACY_CART_COLLECTION)
                    .document(item.id.toString())
                    .delete()
                    .await()
                Log.d(TAG, "Synced cart item ${item.id} to new cartItems path")
            } else {
                firestore.userCollection(uid, LEGACY_CART_COLLECTION)
                    .document(item.id.toString())
                    .set(item)
                    .await()
                Log.d(TAG, "Kept cart item ${item.id} on legacy path until catalog migration")
            }
        } catch (exception: Exception) {
            Log.w(TAG, "Cart sync failed for ${item.cartKey}", exception)
            throw exception
        }
    }

    private suspend fun remoteCartItem(document: com.google.firebase.firestore.DocumentSnapshot): CartItemModel? {
        val productId = document.getString("productId") ?: return null
        val variantId = document.getString("variantId") ?: return null
        val reference = catalogResolver.resolve(productId, variantId) ?: return null
        val product = reference.product.toProductResponse(listOf(reference.variant))
        return CartItemModel(
            id = product.id,
            title = product.title,
            description = product.description,
            mrp = reference.variant.mrpMinor / 100.0,
            price = reference.variant.priceMinor / 100.0,
            rating = product.reviews.firstOrNull()?.rating?.toDouble() ?: 0.0,
            ratingCount = 0,
            returnPolicy = product.returnPolicy,
            deliveryDetails = product.shippingInformation,
            imageUrl = product.thumbnail,
            quantity = (document.getLong("quantity") ?: 1L).toInt().coerceIn(1, MAX_QUANTITY),
            isSelected = document.getBoolean("selected") ?: true,
            productId = productId,
            variantId = variantId,
            variantName = reference.variant.name,
        )
    }

    private fun CartItemEntity.toModel() = CartItemModel(
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
        isSelected = isSelected,
        productId = productId,
        variantId = variantId,
        variantName = variantName,
    )

    private fun CartItemModel.toEntity(ownerId: String) = CartItemEntity(
        ownerId = ownerId,
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
        isSelected = isSelected,
        cartKey = cartKey,
        productId = productId,
        variantId = variantId,
        variantName = variantName,
    )

    private fun FirebaseFirestore.userCollection(uid: String, collection: String) =
        collection("users").document(uid).collection(collection)

    private companion object {
        const val TAG = "CartRepository"
        const val NEW_CART_COLLECTION = "cartItems"
        const val LEGACY_CART_COLLECTION = "cart"
        const val MAX_QUANTITY = 10
        const val UPSERT = "UPSERT"
        const val DELETE = "DELETE"
    }
}
