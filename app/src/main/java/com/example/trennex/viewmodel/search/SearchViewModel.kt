package com.example.trennex.viewmodel.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trennex.config.CategoryLayoutConfig
import com.example.trennex.data.local.search.RecentSearchDao
import com.example.trennex.data.local.search.RecentSearchEntity
import com.example.trennex.data.model.ProductResponse
import com.example.trennex.repository.product.ProductRepository
import com.example.trennex.ui.search.SearchUiState
import com.example.trennex.ui.search.SearchSortOrder
import com.example.trennex.ui.search.SearchGenderFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class SearchViewModel: ViewModel(){
    private val repository = ProductRepository()
    private var recentSearchDao: RecentSearchDao? = null
    private var recommendationsJob: Job? = null
    private var searchJob: Job? = null

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
        recommendationsJob?.cancel()
        searchJob?.cancel()
        if(query.isBlank()){
            _uiState.value = SearchUiState()
            return
        }
        searchJob = viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    query = query,
                    isLoading = true, 
                    error = null, 
                    hasResults = false,
                    showRecommendations = false,
                    allResults = emptyList(),
                    sortOrder = SearchSortOrder.RELEVANCE,
                    genderFilter = SearchGenderFilter.ALL
                )
                
                recentSearchDao?.let { dao ->
                    dao.deleteRecentSearch(query)
                    dao.insertRecentSearch(RecentSearchEntity(query = query))
                }

                val results = repository.searchProducts(query)
                val isGrid = determineLayoutType(query,results)
                if(results.isEmpty()){
                    _uiState.value = SearchUiState(
                        query = query,
                        hasResults = false,
                        error = "NO PRODUCTS FOUND",
                        isGridLayout = false,
                        showGenderFilter = false,
                        showRecommendations = false
                    )
                }else{
                    _uiState.value = SearchUiState(
                        query = query,
                        isLoading = false,
                        searchResults = results,
                        allResults = results,
                        isGridLayout = isGrid,
                        hasResults = true,
                        showGenderFilter = results.any(::hasGenderTag),
                        showRecommendations = false
                    )
                }
            }catch (e: CancellationException) {
                throw e
            }catch (e: Exception){
                _uiState.value = SearchUiState(
                    query = query,
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
        recommendationsJob?.cancel()
        searchJob?.cancel()
        _uiState.value = SearchUiState()
    }

    fun setSortOrder(order: SearchSortOrder) {
        _uiState.value = filteredState(_uiState.value.copy(sortOrder = order))
    }

    fun setGenderFilter(filter: SearchGenderFilter) {
        _uiState.value = filteredState(_uiState.value.copy(genderFilter = filter))
    }

    private fun filteredState(state: SearchUiState): SearchUiState {
        val filtered = state.allResults.filter { product ->
            when (state.genderFilter) {
                SearchGenderFilter.ALL -> true
                SearchGenderFilter.MEN -> product.tags.orEmpty().any { it.lowercase() in setOf("men", "male", "mens") }
                SearchGenderFilter.WOMEN -> product.tags.orEmpty().any { it.lowercase() in setOf("women", "female", "womens") }
            }
        }
        val sorted = when (state.sortOrder) {
            SearchSortOrder.RELEVANCE -> filtered
            SearchSortOrder.PRICE_LOW -> filtered.sortedBy { it.price }
            SearchSortOrder.PRICE_HIGH -> filtered.sortedByDescending { it.price }
            SearchSortOrder.NAME -> filtered.sortedBy { it.title.lowercase() }
        }
        return state.copy(searchResults = sorted)
    }

    private fun hasGenderTag(product: ProductResponse): Boolean =
        product.tags.orEmpty().any { it.lowercase() in setOf("men", "male", "mens", "women", "female", "womens") }

    fun searchByImageSignals(recognizedText: String, labels: List<String>) {
        recommendationsJob?.cancel()
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.value = SearchUiState(query = "Photo search", isLoading = true)
            try {
                val textWords = recognizedText.lowercase().split(Regex("[^a-z0-9]+"))
                    .filter { it.length >= 3 }.distinct()
                val labelWords = labels.flatMap { label ->
                    label.lowercase().split(Regex("[^a-z0-9]+"))
                }.filter { it.length >= 4 }.distinct()
                val rankedMatches = repository.searchProducts("", includeVariants = false).mapNotNull { product ->
                    val title = product.title.lowercase()
                    val brand = product.brand.orEmpty().lowercase()
                    val category = product.category.orEmpty().lowercase()
                    val tags = product.tags.orEmpty().joinToString(" ").lowercase()
                    val textScore = textWords.sumOf { word ->
                        when {
                            title.contains(word) -> 4
                            brand.contains(word) -> 3
                            tags.contains(word) -> 2
                            else -> 0
                        }
                    }
                    val labelScore = labelWords.sumOf { word ->
                        when {
                            category.contains(word) || word.contains(category) && category.isNotBlank() -> 2
                            tags.contains(word) || title.contains(word) -> 1
                            else -> 0
                        }
                    }
                    val score = textScore + labelScore
                    if (score >= 2) product to score else null
                }.sortedByDescending { it.second }.take(12).map { it.first }
                val matches = coroutineScope {
                    rankedMatches.map { product -> async { repository.getProductDetail(product.id) } }.awaitAll()
                }
                _uiState.value = if (matches.isEmpty()) {
                    SearchUiState(query = "Photo search", error = "NO PRODUCTS FOUND")
                } else {
                    SearchUiState(
                        query = "Photo search",
                        searchResults = matches,
                        allResults = matches,
                        hasResults = true,
                        showGenderFilter = matches.any(::hasGenderTag)
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = SearchUiState(query = "Photo search", error = "ERROR LOADING PRODUCTS")
            }
        }
    }

    fun loadRecommendations(query: String){
        recommendationsJob?.cancel()
        searchJob?.cancel()
        _uiState.value = _uiState.value.copy(
            query = query,
            isLoading = false,
            searchResults = emptyList(),
            allResults = emptyList(),
            hasResults = false,
            error = null,
            recommendations = emptyList(),
            showRecommendations = false
        )
        recommendationsJob = viewModelScope.launch {
            delay(250)
            try {
                val items = repository.searchProducts(query, includeVariants = false).take(8).map { it.title }
                _uiState.value = if(items.isNotEmpty()){
                    _uiState.value.copy(
                        query = query,
                        recommendations = items,
                        showRecommendations = true,
                        hasResults = false

                    )
                }else{
                    _uiState.value.copy(
                        query = query,
                        recommendations = emptyList(),
                        showRecommendations = false,
                        hasResults = false
                    )
                }
            }catch (e: CancellationException) {
                throw e
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
}
