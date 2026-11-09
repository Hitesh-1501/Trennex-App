package com.example.trennex.repository.wishlist

import android.util.Log
import com.example.trennex.data.local.wihslist.WishlistDao
import com.example.trennex.data.local.wihslist.WishlistItemEntity
import com.example.trennex.repository.product.CatalogItemResolver
import com.example.trennex.ui.wishlist.model.WishlistItemsModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class WishlistRepository(
    private val dao: WishlistDao,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {
    private val catalogResolver = CatalogItemResolver(firestore)

    private fun getUserId(): String? = auth.currentUser?.uid

    fun observeItems(): Flow<List<WishlistItemsModel>> = dao.observeWishlistItems().map { entities ->
        entities.map { it.toModel() }
    }

    suspend fun fetchAndSyncRemoteWishlist() {
        val uid = getUserId() ?: return
        try {
            val newItems = runCatching {
                firestore.userCollection(uid, NEW_WISHLIST_COLLECTION)
                    .get()
                    .await()
                    .documents
                    .mapNotNull { document -> remoteWishlistItem(document) }
            }.getOrDefault(emptyList())

            if (newItems.isNotEmpty()) {
                newItems.forEach { dao.upsert(it.toEntity()) }
                return
            }

            val legacyItems = runCatching {
                firestore.userCollection(uid, LEGACY_WISHLIST_COLLECTION)
                    .get()
                    .await()
                    .documents
                    .mapNotNull { document ->
                        runCatching { document.toObject(WishlistItemsModel::class.java) }.getOrNull()
                    }
            }.getOrDefault(emptyList())

            for (item in legacyItems) {
                dao.upsert(item.toEntity())
                syncItemToFirestore(item)
            }
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to sync remote wishlist", exception)
        }
    }

    suspend fun addOrUpdate(item: WishlistItemsModel) {
        dao.upsert(item.toEntity())
        syncItemToFirestore(item)
    }

    suspend fun removeItem(itemId: Int) {
        dao.removeItem(itemId)
        val uid = getUserId() ?: return
        runCatching {
            catalogResolver.resolveLegacyId(itemId)?.let { reference ->
                firestore.userCollection(uid, NEW_WISHLIST_COLLECTION)
                    .document(reference.variant.documentId)
                    .delete()
                    .await()
            }
        }.onFailure { exception ->
            Log.w(TAG, "New wishlist removal unavailable for item $itemId", exception)
        }
        try {
            firestore.userCollection(uid, LEGACY_WISHLIST_COLLECTION)
                .document(itemId.toString())
                .delete()
                .await()
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to remove wishlist item $itemId", exception)
        }
    }

    private suspend fun syncItemToFirestore(item: WishlistItemsModel) {
        val uid = getUserId() ?: return
        try {
            val reference = catalogResolver.resolveLegacyId(item.id)
            if (reference != null) {
                firestore.userCollection(uid, NEW_WISHLIST_COLLECTION)
                    .document(reference.variant.documentId)
                    .set(
                        mapOf(
                            "schemaVersion" to 1,
                            "productId" to reference.product.documentId,
                            "variantId" to reference.variant.documentId,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp(),
                        )
                    )
                    .await()
                firestore.userCollection(uid, LEGACY_WISHLIST_COLLECTION)
                    .document(item.id.toString())
                    .delete()
                    .await()
                Log.d(TAG, "Synced wishlist item ${item.id} to new wishlistItems path")
            } else {
                firestore.userCollection(uid, LEGACY_WISHLIST_COLLECTION)
                    .document(item.id.toString())
                    .set(item)
                    .await()
                Log.d(TAG, "Kept wishlist item ${item.id} on legacy path until catalog migration")
            }
        } catch (exception: Exception) {
            Log.w(TAG, "New wishlist path unavailable; keeping item ${item.id} on legacy path", exception)
            runCatching { syncLegacyItem(uid, item) }
                .onFailure { fallbackError -> Log.e(TAG, "Legacy wishlist fallback failed", fallbackError) }
        }
    }

    private suspend fun syncLegacyItem(uid: String, item: WishlistItemsModel) {
        firestore.userCollection(uid, LEGACY_WISHLIST_COLLECTION)
            .document(item.id.toString())
            .set(item)
            .await()
    }

    private suspend fun remoteWishlistItem(document: com.google.firebase.firestore.DocumentSnapshot): WishlistItemsModel? {
        val productId = document.getString("productId") ?: return null
        val variantId = document.getString("variantId") ?: return null
        val reference = catalogResolver.resolve(productId, variantId) ?: return null
        val product = reference.product.toProductResponse(listOf(reference.variant))
        return WishlistItemsModel(
            id = product.id,
            imageUrl = product.thumbnail,
            title = product.title,
            description = product.description,
            mrp = reference.variant.mrpMinor / 100.0,
            price = reference.variant.priceMinor / 100.0,
            rating = 0.0,
            ratingCount = 0,
            returnPolicy = product.returnPolicy,
            deliveryDetails = product.shippingInformation,
        )
    }

    private fun WishlistItemEntity.toModel() = WishlistItemsModel(
        id = id,
        imageUrl = imageUrl,
        title = title,
        description = description,
        mrp = mrp,
        price = price,
        rating = rating,
        ratingCount = ratingCount,
        returnPolicy = returnPolicy,
        deliveryDetails = deliveryDetails,
    )

    private fun WishlistItemsModel.toEntity() = WishlistItemEntity(
        id = id,
        imageUrl = imageUrl,
        title = title,
        description = description,
        mrp = mrp,
        price = price,
        rating = rating,
        ratingCount = ratingCount,
        returnPolicy = returnPolicy,
        deliveryDetails = deliveryDetails,
    )

    private fun FirebaseFirestore.userCollection(uid: String, collection: String) =
        collection("users").document(uid).collection(collection)

    private companion object {
        const val TAG = "WishlistRepository"
        const val NEW_WISHLIST_COLLECTION = "wishlistItems"
        const val LEGACY_WISHLIST_COLLECTION = "wishlist"
    }
}
