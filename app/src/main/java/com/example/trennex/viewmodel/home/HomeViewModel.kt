package com.example.trennex.viewmodel.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trennex.repository.product.ProductRepository
import com.example.trennex.repository.user.AddressEntity
import com.example.trennex.repository.user.UserRepository
import com.example.trennex.ui.home.HomeUiState
import com.example.trennex.ui.home.model.CategoryModel
import com.example.trennex.ui.home.model.ProductModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    private val productRepository = ProductRepository()
    private val userRepository = UserRepository()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        fetchInitialData()
        observeUserData()
    }

    private fun fetchInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                coroutineScope {
                    val categoriesDeferred = async { productRepository.getCategory() }
                    val homeCatalogDeferred = async { productRepository.getHomeCatalogContent() }

                    val categories = categoriesDeferred.await().mapIndexed { index, category ->
                        CategoryModel(id = index + 1, title = category.name, slug = category.slug)
                    }
                    val homeCatalog = homeCatalogDeferred.await()

                    if (homeCatalog != null) {
                        _uiState.update {
                            it.copy(
                                categories = listOf(CategoryModel(0, "All", null)) + categories,
                                products = homeCatalog.products.toProductModels(),
                                topDeals = homeCatalog.featuredProducts.toProductModels(),
                                newArrivals = homeCatalog.newArrivals.toProductModels(),
                                banners = homeCatalog.banners,
                                isLoading = false,
                            )
                        }
                    } else {
                        val productsDeferred = async { productRepository.getProducts() }
                        val topDealsDeferred = async { productRepository.getProducts(limit = 10, skip = 30) }
                        val newArrivalsDeferred = async { productRepository.getProducts(limit = 10, skip = 40) }

                        val products = productsDeferred.await()
                        val topDeals = topDealsDeferred.await()
                        val newArrivals = newArrivalsDeferred.await()
                        _uiState.update {
                            it.copy(
                                categories = listOf(CategoryModel(0, "All", null)) + categories,
                                products = products.toProductModels(),
                                topDeals = topDeals.toProductModels(),
                                newArrivals = newArrivals.toProductModels(),
                                banners = products.mapNotNull { product -> product.thumbnail.takeIf(String::isNotBlank) }.take(5),
                                isLoading = false,
                            )
                        }
                    }
                }
            } catch (exception: Exception) {
                _uiState.update { it.copy(isLoading = false, error = exception.localizedMessage) }
            }
        }
    }

    private fun observeUserData() {
        viewModelScope.launch {
            combine(
                userRepository.observeSavedAddresses(),
                userRepository.observeSelectedAddressId(),
                MutableStateFlow(userRepository.getUserName())
            ) { addresses, selectedId, name ->
                val selected = addresses.find { it.id == selectedId }
                _uiState.update {
                    it.copy(
                        savedAddresses = addresses,
                        selectedAddress = selected,
                        userName = name
                    )
                }
            }.collectLatest { }
        }
    }

    fun refreshData() {
        fetchInitialData()
    }

    fun fetchProductsByCategory(slug: String?) {
        viewModelScope.launch {
            try {
                val products = if (slug == null) {
                    productRepository.getProducts()
                } else {
                    productRepository.getProductByCategory(slug)
                }
                _uiState.update { state -> state.copy(products = products.toProductModels()) }
            } catch (exception: Exception) {
                _uiState.update { it.copy(error = exception.localizedMessage) }
            }
        }
    }

    fun selectAddress(address: AddressEntity) {
        viewModelScope.launch {
            userRepository.updateSelectedAddressId(address.id)
            _uiState.update { it.copy(selectedAddress = address) }
        }
    }

    fun deleteAddress(address: AddressEntity) {
        viewModelScope.launch {
            userRepository.deleteAddress(address.id)
        }
    }

    fun saveCurrentAddress(address: String, latitude: Double, longitude: Double) {
        viewModelScope.launch {
            val cleanedAddress = address.trim()
            if (cleanedAddress.isBlank()) return@launch

            val existingAddress = _uiState.value.savedAddresses.find {
                it.address.normalizeAddressKey() == cleanedAddress.normalizeAddressKey()
            }

            if (existingAddress != null) {
                selectAddress(existingAddress)
                return@launch
            }

            val data = mapOf(
                "address" to cleanedAddress,
                "userName" to _uiState.value.userName,
                "addressType" to "Home",
                "latitude" to latitude,
                "longitude" to longitude,
                "placeName" to (cleanedAddress.take(20) + if (cleanedAddress.length > 20) "..." else "")
            )
            val id = userRepository.saveAddress(data)
            if (id != null) userRepository.updateSelectedAddressId(id)
        }
    }

    private fun String.normalizeAddressKey(): String = trim()
        .replace(Regex("\\s+"), " ")
        .lowercase(java.util.Locale.getDefault())
}

private fun List<com.example.trennex.data.model.ProductResponse>.toProductModels(): List<ProductModel> =
    map { product ->
        ProductModel(
            id = product.id,
            image = product.thumbnail,
            name = product.title,
            price = product.price,
        )
    }
