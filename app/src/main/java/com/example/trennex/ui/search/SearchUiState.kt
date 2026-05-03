package com.example.trennex.ui.search

import com.example.trennex.data.model.ProductResponse

data class SearchUiState(
    val isLoading: Boolean = false,
    val searchResults: List<ProductResponse> = emptyList(),
    val recommendations: List<String> = emptyList(),
    val isGridLayout: Boolean = false,
    val showGenderFilter: Boolean = false,
    val error: String? = null,
    val hasResults: Boolean = false,
    val showRecommendations: Boolean = false
)