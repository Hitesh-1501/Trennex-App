package com.example.trennex.repository.cart

import android.util.Log
import com.example.trennex.data.local.cart.CartDao
import com.example.trennex.data.local.cart.CartItemEntity
import com.example.trennex.repository.product.CatalogItemResolver
import com.example.trennex.ui.cart.model.CartItemModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class CartRepository(
    private val dao: CartDao,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {
    private val catalogResolver = CatalogItemResolver(firestore)

    private fun getUserId(): String? = auth.currentUser?.uid

    fun observeItems(): Flow<List<CartItemModel>> = dao.observeCartItems().map { entities ->
        entities.map { it.toModel() }
    }

    suspend fun fetchAndSyncRemoteCart() {
        val uid = getUserId() ?: return
        try {
            val newItems = runCatching {
                firestore.userCollection(uid, NEW_CART_COLLECTION)
                    .get()
                    .await()
                    .documents
                    .mapNotNull { document -> remoteCartItem(document) }
            }.getOrDefault(emptyList())

            if (newItems.isNotEmpty()) {
                newItems.forEach { dao.upsert(it.toEntity()) }
                return
            }

            val legacyItems = runCatching {
                firestore.userCollection(uid, LEGACY_CART_COLLECTION)
                    .get()
                    .await()
                    .documents
                    .mapNotNull { document ->
                        runCatching { document.toObject(CartItemModel::class.java) }.getOrNull()
                    }
            }.getOrDefault(emptyList())

            for (item in legacyItems) {
                val migrated = item.copy(quantity = item.quantity.coerceIn(1, MAX_QUANTITY))
                dao.upsert(migrated.toEntity())
                syncItemToFirestore(migrated)
            }
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to sync remote cart", exception)
        }
    }

    suspend fun addItem(item: CartItemModel) {
        val safeQuantity = item.quantity.coerceIn(1, MAX_QUANTITY)
        val existing = dao.getItemById(item.id)
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
                quantity = (existing.quantity + safeQuantity).coerceAtMost(MAX_QUANTITY),
                isSelected = true,
            )
        }

        dao.upsert(updatedItem.toEntity())
        syncItemToFirestore(updatedItem)
    }

    suspend fun toggleSelection(itemId: Int, selected: Boolean) {
        dao.updateSelection(itemId, selected)
        dao.getItemById(itemId)?.let { syncItemToFirestore(it.toModel()) }
    }

    suspend fun toggleAllSelection(selected: Boolean) {
        dao.updateAllSelection(selected)
        dao.getAllCartItems().forEach { syncItemToFirestore(it.toModel()) }
    }

    suspend fun updateQuantity(itemId: Int, quantity: Int) {
        dao.updateQuantity(itemId, quantity.coerceIn(1, MAX_QUANTITY))
        dao.getItemById(itemId)?.let { syncItemToFirestore(it.toModel()) }
    }

    suspend fun removeItem(itemId: Int) {
        dao.removeItem(itemId)
        removeRemoteItem(itemId)
    }

    suspend fun deleteSelectedItems() {
        dao.deleteSelectedItems()
        val uid = getUserId() ?: return
        try {
            runCatching {
                firestore.userCollection(uid, NEW_CART_COLLECTION)
                    .whereEqualTo("selected", true)
                    .get()
                    .await()
                    .documents
                    .forEach { it.reference.delete().await() }
            }
            runCatching {
                firestore.userCollection(uid, LEGACY_CART_COLLECTION)
                    .whereEqualTo("isSelected", true)
                    .get()
                    .await()
                    .documents
                    .forEach { it.reference.delete().await() }
            }
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to delete selected remote cart items", exception)
        }
    }

    private suspend fun syncItemToFirestore(item: CartItemModel) {
        val uid = getUserId() ?: return
        try {
            val reference = catalogResolver.resolveLegacyId(item.id)
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
            Log.w(TAG, "New cart path unavailable; keeping item ${item.id} on legacy path", exception)
            runCatching { syncLegacyItem(uid, item) }
                .onFailure { fallbackError -> Log.e(TAG, "Legacy cart fallback failed", fallbackError) }
        }
    }

    private suspend fun syncLegacyItem(uid: String, item: CartItemModel) {
        firestore.userCollection(uid, LEGACY_CART_COLLECTION)
            .document(item.id.toString())
            .set(item)
            .await()
    }

    private suspend fun removeRemoteItem(itemId: Int) {
        val uid = getUserId() ?: return
        runCatching {
            catalogResolver.resolveLegacyId(itemId)?.let { reference ->
                firestore.userCollection(uid, NEW_CART_COLLECTION)
                    .document(reference.variant.documentId)
                    .delete()
                    .await()
            }
        }.onFailure { exception ->
            Log.w(TAG, "New cart removal unavailable for item $itemId", exception)
        }
        try {
            firestore.userCollection(uid, LEGACY_CART_COLLECTION)
                .document(itemId.toString())
                .delete()
                .await()
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to remove cart item $itemId", exception)
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
    )

    private fun CartItemModel.toEntity() = CartItemEntity(
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
    )

    private fun FirebaseFirestore.userCollection(uid: String, collection: String) =
        collection("users").document(uid).collection(collection)

    private companion object {
        const val TAG = "CartRepository"
        const val NEW_CART_COLLECTION = "cartItems"
        const val LEGACY_CART_COLLECTION = "cart"
        const val MAX_QUANTITY = 10
    }
}
