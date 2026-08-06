package com.example.trennex.viewmodel.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trennex.config.CategoryLayoutConfig
import com.example.trennex.data.local.search.RecentSearchDao
import com.example.trennex.data.local.search.RecentSearchEntity
import com.example.trennex.data.model.ProductResponse
import com.example.trennex.repository.product.ProductRepository
import com.example.trennex.ui.search.SearchUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SearchViewModel: ViewModel(){
    private val repository = ProductRepository()
    private var recentSearchDao: RecentSearchDao? = null

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _recentSearches = MutableStateFlow<List<String>>(emptyList())
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

    fun initDao(dao: RecentSearchDao) {
        if (recentSearchDao != null) return
        recentSearchDao = dao
        viewModelScope.launch {
            dao.getRecentSearches().collectLatest { searches ->
                _recentSearches.value = searches.map { it.query }
            }
        }
    }

    fun searchProducts(query: String){
        if(query.isBlank()){
            _uiState.value = SearchUiState()
            return
        }
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isLoading = true, 
                    error = null, 
                    hasResults = false,
                    showRecommendations = false
                )
                
                recentSearchDao?.let { dao ->
                    dao.deleteRecentSearch(query)
                    dao.insertRecentSearch(RecentSearchEntity(query = query))
                }

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

    fun deleteRecentSearch(query: String) {
        viewModelScope.launch {
            recentSearchDao?.deleteRecentSearch(query)
        }
    }

    fun clearSearch() {
        _uiState.value = SearchUiState()
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
        
        // Define non-clothing items to exclude early (exact or partial matches that shouldn't trigger gender)
        val nonClothingKeywords = setOf(
            "laptop", "phone", "mobile", "tv", "television", "computer", "pc", 
            "camera", "watch", "smartwatch", "headphone", "earphone", "speaker",
            "tablet", "ipad", "macbook", "desktop", "tabletop", "stop", "photoshop",
            "fridge", "refrigerator", "ac", "conditioner", "oven", "microwave"
        )
        if (nonClothingKeywords.any { queryLower.contains(it) }) return false

        val clothingKeywords = setOf(
            "shirt", "dress", "cloth", "tshirt", "t-shirt", "polo", "hoodie",
            "sweater", "jacket", "coat", "blazer", "cardigan", "pant", "pants",
            "jeans", "shorts", "skirt", "saree", "lehenga", "top", "camisole",
            "tank", "vest", "robe", "gown", "suit", "wear", "bottom", "apparel"
        )
        
        // Split query into words to avoid partial matches like "laptop" matching "top"
        val words = queryLower.split(Regex("[\\s,.-]+")).filter { it.isNotBlank() }
        
        // Check for exact word match
        if(words.any { word -> clothingKeywords.contains(word) }){
            return true
        }
        
        // Also check result categories if available
        if(results.isNotEmpty()){
            val category = results[0].category?.lowercase() ?: ""
            val clothingCategories = setOf("clothing", "apparel", "men's fashion", "women's fashion", "kids' fashion")
            if (clothingCategories.any { category.contains(it) }) {
                return true
            }
        }
        
        return false
    }
}