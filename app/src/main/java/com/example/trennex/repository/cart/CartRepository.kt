package com.example.trennex.repository.cart

import android.util.Log
import com.example.trennex.data.local.AccountScope
import com.example.trennex.data.local.cart.CartDao
import com.example.trennex.data.local.cart.CartItemEntity
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

class CartRepository(
    private val dao: CartDao,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {
    private val catalogResolver = CatalogItemResolver(firestore)

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeItems(): Flow<List<CartItemModel>> = AccountScope.owner.flatMapLatest { ownerId ->
        dao.observeCartItems(ownerId).map { entities -> entities.map { it.toModel() } }
            .onStart { emit(emptyList()) }
    }

    suspend fun fetchAndSyncRemoteCart(ownerId: String) {
        val uid = AccountScope.uid(ownerId) ?: return
        if (auth.currentUser?.uid != uid) return
        try {
            val newItems = runCatching {
                firestore.userCollection(uid, NEW_CART_COLLECTION)
                    .get()
                    .await()
                    .documents
                    .mapNotNull { document -> remoteCartItem(document) }
            }.getOrDefault(emptyList())

            if (newItems.isNotEmpty()) {
                newItems.forEach { item ->
                    dao.getItemByKey(ownerId, "legacy:${item.id}")?.let {
                        dao.removeItem(ownerId, it.cartKey)
                    }
                    dao.upsert(item.toEntity(ownerId))
                }
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
                dao.upsert(migrated.toEntity(ownerId))
                syncItemToFirestore(ownerId, migrated)
            }
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to sync remote cart", exception)
        }
    }

    suspend fun addItem(ownerId: String, item: CartItemModel) {
        syncItemToFirestore(ownerId, addItemLocally(ownerId, item))
    }

    suspend fun addItemLocally(ownerId: String, item: CartItemModel): CartItemModel {
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

        dao.upsert(updatedItem.toEntity(ownerId))
        return updatedItem
    }

    suspend fun syncAddedItem(ownerId: String, item: CartItemModel) {
        syncItemToFirestore(ownerId, item)
    }

    suspend fun toggleSelection(ownerId: String, cartKey: String, selected: Boolean) {
        dao.updateSelection(ownerId, cartKey, selected)
        dao.getItemByKey(ownerId, cartKey)?.let { syncItemToFirestore(ownerId, it.toModel()) }
    }

    suspend fun toggleAllSelection(ownerId: String, selected: Boolean) {
        dao.updateAllSelection(ownerId, selected)
        dao.getAllCartItems(ownerId).forEach { syncItemToFirestore(ownerId, it.toModel()) }
    }

    suspend fun updateQuantity(ownerId: String, cartKey: String, quantity: Int) {
        dao.updateQuantity(ownerId, cartKey, quantity.coerceIn(1, MAX_QUANTITY))
        dao.getItemByKey(ownerId, cartKey)?.let { syncItemToFirestore(ownerId, it.toModel()) }
    }

    suspend fun removeItem(ownerId: String, cartKey: String) {
        val item = dao.getItemByKey(ownerId, cartKey)?.toModel() ?: return
        dao.removeItem(ownerId, cartKey)
        removeRemoteItem(ownerId, item)
    }

    suspend fun deleteSelectedItems(ownerId: String) {
        dao.deleteSelectedItems(ownerId)
        val uid = AccountScope.uid(ownerId) ?: return
        if (auth.currentUser?.uid != uid) return
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

    private suspend fun syncItemToFirestore(ownerId: String, item: CartItemModel) {
        val uid = AccountScope.uid(ownerId) ?: return
        if (auth.currentUser?.uid != uid) return
        try {
            val reference = if (item.productId != null && item.variantId != null) {
                catalogResolver.resolve(item.productId, item.variantId)
            } else catalogResolver.resolveLegacyId(item.id)
            if (item.variantId != null && reference == null) {
                Log.w(TAG, "Selected variant ${item.variantId} is unavailable; keeping local cart row")
                return
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
            if (item.variantId == null) {
                runCatching { syncLegacyItem(uid, item) }
                    .onFailure { fallbackError -> Log.e(TAG, "Legacy cart fallback failed", fallbackError) }
            }
        }
    }

    private suspend fun syncLegacyItem(uid: String, item: CartItemModel) {
        firestore.userCollection(uid, LEGACY_CART_COLLECTION)
            .document(item.id.toString())
            .set(item)
            .await()
    }

    private suspend fun removeRemoteItem(ownerId: String, item: CartItemModel) {
        val uid = AccountScope.uid(ownerId) ?: return
        if (auth.currentUser?.uid != uid) return
        runCatching {
            val reference = if (item.productId != null && item.variantId != null) {
                catalogResolver.resolve(item.productId, item.variantId)
            } else catalogResolver.resolveLegacyId(item.id)
            reference?.let {
                firestore.userCollection(uid, NEW_CART_COLLECTION)
                    .document(it.variant.documentId)
                    .delete()
                    .await()
            }
        }.onFailure { exception ->
            Log.w(TAG, "New cart removal unavailable for item ${item.cartKey}", exception)
        }
        try {
            firestore.userCollection(uid, LEGACY_CART_COLLECTION)
                .document(item.id.toString())
                .delete()
                .await()
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to remove cart item ${item.cartKey}", exception)
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
    }
}
