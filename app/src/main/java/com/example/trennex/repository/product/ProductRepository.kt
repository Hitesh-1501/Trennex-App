package com.example.trennex.repository.product

import com.example.trennex.data.model.CategoryApiResponse
import com.example.trennex.data.model.ProductResponse
import com.example.trennex.data.remote.network.RetrofitInstance

class ProductRepository {
    suspend fun getProducts(): List<ProductResponse>{
        return RetrofitInstance.api.getProducts().products
    }

    suspend fun getProductDetail(id: Int) : ProductResponse {
        return RetrofitInstance.api.getProductDetail(id)
    }
    suspend fun getCategory(): List<CategoryApiResponse> {
        return RetrofitInstance.api.getCategories()
    }

    suspend fun getProductByCategory(slug: String): List<ProductResponse>{
        return RetrofitInstance.api.getProductByCategory(slug).products
    }

    suspend fun searchProducts(query: String): List<ProductResponse>{
        return RetrofitInstance.api.searchProducts(query).products
    }

}