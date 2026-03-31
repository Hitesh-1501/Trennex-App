package com.example.trennex.data.remote.api

import com.example.trennex.data.model.ProductListResponse
import retrofit2.http.GET

interface ApiService {
    @GET("products")
    suspend fun getProducts(): ProductListResponse
}