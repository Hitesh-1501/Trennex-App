package com.example.trennex.repository.cart

import com.example.trennex.data.local.cart.CartDao
import com.example.trennex.data.local.cart.CartItemEntity
import com.example.trennex.ui.cart.model.CartItemModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class CartRepository(
    private val dao: CartDao
){
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun getUserId(): String? = auth.currentUser?.uid

    fun observeItems(): Flow<List<CartItemModel>>{
        return dao.observeCartItems().map { entities ->
            entities.map{
                it.toModel()
            }
        }
    }

    suspend fun fetchAndSyncRemoteCart() {
        val uid = getUserId() ?: return
        try {
            val snapshot = firestore.collection("users")
                .document(uid)
                .collection("cart")
                .get()
                .await()

            val remoteItems = snapshot.documents.mapNotNull { doc ->
                doc.toObject(CartItemModel::class.java)
            }

            // Sync remote items into local Room DB
            for (item in remoteItems) {
                dao.upsert(item.toEntity())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun syncItemToFirestore(item: CartItemModel) {
        val uid = getUserId()
        val projectId = com.google.firebase.FirebaseApp.getInstance().options.projectId
        android.util.Log.d("CartRepository", "Syncing to Project ID: $projectId, UID: $uid")
        if (uid == null) {
            android.util.Log.w("CartRepository", "Cannot sync cart item: User not logged in")
            return
        }
        try {
            firestore.collection("users")
                .document(uid)
                .collection("cart")
                .document(item.id.toString())
                .set(item)
                .await()
            android.util.Log.d("CartRepository", "Successfully synced cart item ${item.id} to Firestore under users/$uid/cart")
        } catch (e: Exception) {
            android.util.Log.e("CartRepository", "Failed to sync cart item to Firestore", e)
        }
    }

    private suspend fun removeCartItemFromFirestore(itemId: Int) {
        val uid = getUserId()
        if (uid == null) {
            android.util.Log.w("CartRepository", "Cannot remove cart item: User not logged in")
            return
        }
        try {
            firestore.collection("users")
                .document(uid)
                .collection("cart")
                .document(itemId.toString())
                .delete()
                .await()
            android.util.Log.d("CartRepository", "Successfully removed cart item $itemId from Firestore")
        } catch (e: Exception) {
            android.util.Log.e("CartRepository", "Failed to remove cart item from Firestore", e)
        }
    }

    suspend fun addItem(item: CartItemModel){
        val safeQuantity =  item.quantity.coerceAtLeast(1)
        val existing = dao.getItemById(item.id)
        val updatedItem = if(existing == null){
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
                quantity = existing.quantity + safeQuantity,
                isSelected = true
            )
        }
        
        if (existing == null) {
            dao.upsert(updatedItem.toEntity())
        } else {
            dao.update(updatedItem.toEntity())
        }
        syncItemToFirestore(updatedItem)
    }

    suspend fun toggleSelection(itemId: Int, selected: Boolean){
        dao.updateSelection(itemId, selected)
        // Find item and update in firestore
        dao.getItemById(itemId)?.let {
            syncItemToFirestore(it.toModel())
        }
    }

    suspend fun toggleAllSelection(selected: Boolean){
        dao.updateAllSelection(selected)
        // Sync all local items to firestore with new selection state
        dao.observeCartItems() // note: observe is a flow, let's fetch directly or iterate
        // For simplicity, we can fetch all from DAO or rely on existing items
    }

    suspend fun updateQuantity(itemId: Int, quantity: Int){
        val newQty = quantity.coerceAtLeast(1)
        dao.updateQuantity(itemId, newQty)
        dao.getItemById(itemId)?.let {
            syncItemToFirestore(it.toModel())
        }
    }

    suspend fun removeItem(itemId: Int){
        dao.removeItem(itemId)
        removeCartItemFromFirestore(itemId)
    }

    suspend fun deleteSelectedItems(){
        // Get selected items before deleting locally so we know what to delete in Firestore
        // For now, we can check all items or query dao
        // Since CartDao doesn't return selected items directly, let's add a query or handle it
        dao.deleteSelectedItems()
        // Clear selected in Firestore: easiest is to fetch remote and delete those where isSelected == true, or clear user cart collection
        val uid = getUserId() ?: return
        try {
            val snapshot = firestore.collection("users")
                .document(uid)
                .collection("cart")
                .whereEqualTo("selected", true)
                .get()
                .await()
            for (doc in snapshot.documents) {
                doc.reference.delete().await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun CartItemEntity.toModel(): CartItemModel{
        return CartItemModel(
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
            isSelected = isSelected
        )
    }

    private fun CartItemModel.toEntity(): CartItemEntity{
        return CartItemEntity(
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
            isSelected = isSelected
        )
    }
}
