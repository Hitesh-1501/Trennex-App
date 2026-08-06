package com.example.trennex.data.remote.api


import com.example.trennex.data.model.CategoryApiResponse
import com.example.trennex.data.model.ProductListResponse
import com.example.trennex.data.model.ProductResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @GET("products")
    suspend fun getProducts(
        @Query("limit") limit: Int = 30,
        @Query("skip") skip: Int = 0
    ): ProductListResponse

    @GET("products/{id}")
    suspend fun getProductDetail(
        @Path("id") id: Int
    ): ProductResponse

    @GET("products/categories")
    suspend fun getCategories(): List<CategoryApiResponse>

    @GET("products/category/{slug}")
    suspend fun getProductByCategory(
        @Path("slug") slug: String
    ): ProductListResponse

    @GET("products/search")
    suspend fun searchProducts(@Query("q") query: String): ProductListResponse

}