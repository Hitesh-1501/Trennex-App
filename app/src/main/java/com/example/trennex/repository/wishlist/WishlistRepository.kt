package com.example.trennex.repository.wishlist

import com.example.trennex.data.local.wihslist.WishlistDao
import com.example.trennex.data.local.wihslist.WishlistItemEntity
import com.example.trennex.ui.wishlist.model.WishlistItemsModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class WishlistRepository(
    private val dao: WishlistDao
){
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun getUserId(): String? = auth.currentUser?.uid

    fun observeItems(): Flow<List<WishlistItemsModel>>{
        return dao.observeWishlistItems().map {entities ->
            entities.map{
                it.toModel()
            }
        }
    }

    suspend fun fetchAndSyncRemoteWishlist() {
        val uid = getUserId() ?: return
        try {
            val snapshot = firestore.collection("users")
                .document(uid)
                .collection("wishlist")
                .get()
                .await()

            val remoteItems = snapshot.documents.mapNotNull { doc ->
                doc.toObject(WishlistItemsModel::class.java)
            }

            for (item in remoteItems) {
                dao.upsert(item.toEntity())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun syncItemToFirestore(item: WishlistItemsModel) {
        val uid = getUserId()
        if (uid == null) {
            android.util.Log.w("WishlistRepository", "Cannot sync wishlist item: User not logged in")
            return
        }
        try {
            firestore.collection("users")
                .document(uid)
                .collection("wishlist")
                .document(item.id.toString())
                .set(item)
                .await()
            android.util.Log.d("WishlistRepository", "Successfully synced wishlist item ${item.id} to Firestore")
        } catch (e: Exception) {
            android.util.Log.e("WishlistRepository", "Failed to sync wishlist item to Firestore", e)
        }
    }

    private suspend fun removeWishlistItemFromFirestore(itemId: Int) {
        val uid = getUserId()
        if (uid == null) {
            android.util.Log.w("WishlistRepository", "Cannot remove wishlist item: User not logged in")
            return
        }
        try {
            firestore.collection("users")
                .document(uid)
                .collection("wishlist")
                .document(itemId.toString())
                .delete()
                .await()
            android.util.Log.d("WishlistRepository", "Successfully removed wishlist item $itemId from Firestore")
        } catch (e: Exception) {
            android.util.Log.e("WishlistRepository", "Failed to remove wishlist item from Firestore", e)
        }
    }

    suspend fun addOrUpdate(item: WishlistItemsModel){
        dao.upsert(item.toEntity())
        syncItemToFirestore(item)
    }

    suspend fun removeItem(itemId: Int){
        dao.removeItem(itemId)
        removeWishlistItemFromFirestore(itemId)
    }

    private fun WishlistItemEntity.toModel(): WishlistItemsModel{
        return WishlistItemsModel(
            id = id,
            imageUrl = imageUrl,
            title = title,
            description = description,
            mrp = mrp,
            price = price,
            rating = rating,
            ratingCount = ratingCount,
            returnPolicy = returnPolicy,
            deliveryDetails = deliveryDetails
        )
    }

    private fun WishlistItemsModel.toEntity(): WishlistItemEntity{
        return WishlistItemEntity(
            id = id,
            imageUrl = imageUrl,
            title = title,
            description = description,
            mrp = mrp,
            price = price,
            rating = rating,
            ratingCount = ratingCount,
            returnPolicy = returnPolicy,
            deliveryDetails = deliveryDetails
        )
    }
}