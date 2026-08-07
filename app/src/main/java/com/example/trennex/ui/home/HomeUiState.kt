package com.example.trennex.ui.home

import com.example.trennex.ui.home.model.CategoryModel
import com.example.trennex.ui.home.model.ProductModel
import com.example.trennex.repository.user.AddressEntity

data class HomeUiState(
    val categories: List<CategoryModel> = emptyList(),
    val products: List<ProductModel> = emptyList(),
    val topDeals: List<ProductModel> = emptyList(),
    val newArrivals: List<ProductModel> = emptyList(),
    val banners: List<String> = emptyList(),
    val savedAddresses: List<AddressEntity> = emptyList(),
    val selectedAddress: AddressEntity? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val userName: String = "Guest User"
)
