package com.example.trennex.repository.product

import com.example.trennex.data.model.CategoryApiResponse
import com.example.trennex.data.model.ProductResponse
import com.example.trennex.data.remote.network.RetrofitInstance
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ProductRepository {
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun getProducts(limit: Int = 30, skip: Int = 0): List<ProductResponse> {
        return try {
            val snapshot = firestore.collection("products").get().await()
            val list = snapshot.documents.mapNotNull { doc ->
                doc.toObject(ProductResponse::class.java)
            }
            if (list.isEmpty()) {
                RetrofitInstance.api.getProducts(limit, skip).products
            } else {
                list
            }
        } catch (e: Exception) {
            RetrofitInstance.api.getProducts(limit, skip).products
        }
    }

    suspend fun getProductDetail(id: Int): ProductResponse {
        return try {
            val snapshot = firestore.collection("products")
                .whereEqualTo("id", id)
                .get()
                .await()
            val product = snapshot.documents.firstOrNull()?.toObject(ProductResponse::class.java)
            product ?: RetrofitInstance.api.getProductDetail(id)
        } catch (e: Exception) {
            RetrofitInstance.api.getProductDetail(id)
        }
    }

    suspend fun getCategory(): List<CategoryApiResponse> {
        return try {
            val categories = firestore.collection("categories").get().await().documents.mapNotNull {
                it.toObject(CategoryApiResponse::class.java)
            }
            if (categories.isEmpty()) RetrofitInstance.api.getCategories() else categories
        } catch (e: Exception) {
            RetrofitInstance.api.getCategories()
        }
    }

    suspend fun getProductByCategory(slug: String): List<ProductResponse> {
        return try {
            val snapshot = firestore.collection("products")
                .whereEqualTo("category", slug)
                .get()
                .await()
            val list = snapshot.documents.mapNotNull { doc ->
                doc.toObject(ProductResponse::class.java)
            }
            if (list.isEmpty()) RetrofitInstance.api.getProductByCategory(slug).products else list
        } catch (e: Exception) {
            RetrofitInstance.api.getProductByCategory(slug).products
        }
    }

    suspend fun searchProducts(query: String): List<ProductResponse> {
        return try {
            val snapshot = firestore.collection("products").get().await()
            val list = snapshot.documents.mapNotNull { doc ->
                doc.toObject(ProductResponse::class.java)
            }.filter { it.title.contains(query, ignoreCase = true) || it.description.contains(query, ignoreCase = true) }
            if (list.isEmpty()) RetrofitInstance.api.searchProducts(query).products else list
        } catch (e: Exception) {
            RetrofitInstance.api.searchProducts(query).products
        }
    }
}
