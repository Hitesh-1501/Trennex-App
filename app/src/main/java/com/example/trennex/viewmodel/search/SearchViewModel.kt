package com.example.trennex.viewmodel.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trennex.config.CategoryLayoutConfig
import com.example.trennex.data.model.ProductResponse
import com.example.trennex.repository.product.ProductRepository
import com.example.trennex.ui.search.SearchUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SearchViewModel: ViewModel(){
    private val repository = ProductRepository()
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    fun searchProducts(query: String){
        if(query.isBlank()){
            _uiState.value = SearchUiState()
            return
        }
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                val results = repository.searchProducts(query)
                val isGrid = determineLayoutType(query,results)
                val shouldShowGender = shouldShowGenderFilter(query, results)
                if(results.isEmpty()){
                    _uiState.value = SearchUiState(
                        hasResults = false,
                        error = "NO PRODUCTS FOUND",
                        isGridLayout = false,
                        showGenderFilter = false,
                        showRecommendations = false
                    )
                }else{
                    _uiState.value = SearchUiState(
                        isLoading = false,
                        searchResults = results,
                        isGridLayout = isGrid,
                        hasResults = true,
                        showGenderFilter = shouldShowGender,
                        showRecommendations = false
                    )
                }
            }catch (e: Exception){
                _uiState.value = SearchUiState(
                    isLoading = false,
                    error = "ERROR LOADING PRODUCTS",
                    showRecommendations = false,
                    showGenderFilter = false
                )
            }
        }
    }

    fun loadRecommendations(query: String){
        viewModelScope.launch {
            try {
                val items = repository.searchProducts(query).take(8).map { it.title }
                _uiState.value = if(items.isNotEmpty()){
                    _uiState.value.copy(
                        recommendations = items,
                        showRecommendations = true,
                        hasResults = false

                    )
                }else{
                    _uiState.value.copy(
                        recommendations = emptyList(),
                        showRecommendations = false,
                        hasResults = false
                    )
                }
            }catch (e: Exception){
                _uiState.value = _uiState.value.copy(
                    recommendations = emptyList(),
                    showRecommendations = false
                )
            }
        }
    }

    fun clearRecommendations(){
        _uiState.value = _uiState.value.copy(
            recommendations = emptyList(),
            showRecommendations = false,
            searchResults = emptyList(),
            hasResults = false,
            showGenderFilter = false

        )
    }

    private fun determineLayoutType(query: String,results: List<ProductResponse>): Boolean{
        if(CategoryLayoutConfig.containsGridCategory(query)){
            return true
        }
        if(CategoryLayoutConfig.containsListCategory(query)){
            return false
        }
        if(results.isNotEmpty()){
            return CategoryLayoutConfig.isGridLayout(query, results[0].category)
        }
        return false
    }
    private fun shouldShowGenderFilter(query: String, results: List<ProductResponse>): Boolean{
        val queryLower = query.lowercase()
        val clothingCategories = setOf(
            "shirt", "dress", "cloth", "tshirt", "t-shirt", "polo", "hoodie",
            "sweater", "jacket", "coat", "blazer", "cardigan", "pant", "pants",
            "jeans", "shorts", "skirt", "saree", "lehenga", "top", "camisole",
            "tank", "vest", "robe", "gown", "suit",
            "shoe", "shoes", "sneaker", "boot", "sandal", "flip", "flop",
            "slipper", "heel", "pump", "loafer", "oxford", "athletic",
            "accessory", "accessories", "belt", "bag", "purse", "wallet",
            "watch", "jewelry", "necklace", "bracelet", "earring", "ring",
            "pendant", "chain", "brooch", "pin", "tie", "necktie", "scarf",
            "cap", "hat", "beret", "beanie", "glove", "mitten", "sock",
            "sunglasses", "glass", "headband", "hairpin"
        )
        if(clothingCategories.any{queryLower.contains(it)}){
            return true
        }
        if(results.isNotEmpty()){
            val category = results[0].category?.lowercase() ?: ""
            if (clothingCategories.any { category.contains(it) }) {
                return true
            }
        }
        return false
    }
}