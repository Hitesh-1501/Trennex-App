package com.example.trennex.ui.search

import com.example.trennex.data.model.ProductResponse

data class SearchUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val searchResults: List<ProductResponse> = emptyList(),
    val allResults: List<ProductResponse> = emptyList(),
    val sortOrder: SearchSortOrder = SearchSortOrder.RELEVANCE,
    val genderFilter: SearchGenderFilter = SearchGenderFilter.ALL,
    val recommendations: List<String> = emptyList(),
    val isGridLayout: Boolean = false,
    val showGenderFilter: Boolean = false,
    val error: String? = null,
    val hasResults: Boolean = false,
    val showRecommendations: Boolean = false
)

enum class SearchSortOrder { RELEVANCE, NEWEST, PRICE_LOW, PRICE_HIGH, POPULARITY, DISCOUNT, NAME }

enum class SearchGenderFilter { ALL, MEN, WOMEN }
