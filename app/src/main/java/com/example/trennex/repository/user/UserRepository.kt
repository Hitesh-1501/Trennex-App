package com.example.trennex.repository.user

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun getUserId(): String? = auth.currentUser?.uid

    suspend fun getUserName(): String {
        val uid = getUserId() ?: return "Guest User"
        return try {
            val snapshot = firestore.collection("users").document(uid).get().await()
            snapshot.getString("name").orEmpty().trim().ifBlank { "Guest User" }
        } catch (e: Exception) {
            "Guest User"
        }
    }

    fun observeSavedAddresses(): Flow<List<AddressEntity>> = callbackFlow {
        val uid = getUserId()
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("users")
            .document(uid)
            .collection("savedAddresses")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val addresses = snapshot?.documents?.mapNotNull { doc ->
                    val address = doc.getString("address").orEmpty()
                    if (address.isBlank()) null
                    else AddressEntity(
                        id = doc.id,
                        userName = doc.getString("userName").orEmpty(),
                        flatNo = doc.getString("flatNo").orEmpty(),
                        address = address,
                        mobile = doc.getString("mobile").orEmpty(),
                        latitude = doc.getDouble("latitude") ?: 0.0,
                        longitude = doc.getDouble("longitude") ?: 0.0,
                        placeName = doc.getString("placeName").orEmpty(),
                        addressType = doc.getString("addressType") ?: "Home"
                    )
                } ?: emptyList()
                trySend(addresses)
            }
        awaitClose { listener.remove() }
    }

    suspend fun getSelectedAddressId(): String? {
        val uid = getUserId() ?: return null
        return try {
            val snapshot = firestore.collection("users").document(uid).get().await()
            snapshot.getString("selectedAddressId")
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateSelectedAddressId(addressId: String?) {
        val uid = getUserId() ?: return
        val data = if (addressId != null) {
            mapOf("selectedAddressId" to addressId)
        } else {
            mapOf("selectedAddressId" to FieldValue.delete())
        }
        firestore.collection("users").document(uid).set(data, SetOptions.merge()).await()
    }

    suspend fun saveAddress(addressData: Map<String, Any>): String? {
        val uid = getUserId() ?: return null
        val docRef = firestore.collection("users")
            .document(uid)
            .collection("savedAddresses")
            .add(addressData + mapOf(
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )).await()
        return docRef.id
    }

    suspend fun deleteAddress(addressId: String) {
        val uid = getUserId() ?: return
        firestore.collection("users")
            .document(uid)
            .collection("savedAddresses")
            .document(addressId)
            .delete()
            .await()
    }
}

data class AddressEntity(
    val id: String,
    val userName: String,
    val flatNo: String,
    val address: String,
    val mobile: String,
    val latitude: Double,
    val longitude: Double,
    val placeName: String,
    val addressType: String
) {
    val displayAddress: String
        get() = if (flatNo.isNotBlank()) "$flatNo, $address" else address
}
