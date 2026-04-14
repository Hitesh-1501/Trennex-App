package com.example.trennex.data.remote.api

import com.example.trennex.data.model.CategoryApiResponse
import com.example.trennex.data.model.ProductListResponse
import com.example.trennex.data.model.ProductResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface ApiService {
    @GET("products")
    suspend fun getProducts(): ProductListResponse

    @GET("products/{id}")
    suspend fun getProductDetail(
        @Path("id") id: Int
    ): ProductResponse

    @GET("products/categories")
    suspend fun getCategories(): List<CategoryApiResponse>

}