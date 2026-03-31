package com.example.trennex.data.repository

import com.example.trennex.data.model.ProductResponse
import com.example.trennex.data.remote.network.RetrofitInstance

class ProductRepository {
    suspend fun getProducts(): List<ProductResponse>{
        return RetrofitInstance.api.getProducts().products
    }
}