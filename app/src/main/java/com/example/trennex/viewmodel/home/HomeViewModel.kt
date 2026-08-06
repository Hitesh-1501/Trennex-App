package com.example.trennex.viewmodel.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trennex.repository.product.ProductRepository
import com.example.trennex.repository.user.AddressEntity
import com.example.trennex.repository.user.UserRepository
import com.example.trennex.ui.home.HomeUiState
import com.example.trennex.ui.home.model.CategoryModel
import com.example.trennex.ui.home.model.ProductModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
            _uiState.update { it.copy(isLoading = true) }
            try {
                coroutineScope {
                    val categoriesDeferred = async { productRepository.getCategory() }
                    val productsDeferred = async { productRepository.getProducts() }
                    
                    val categoryResponse = categoriesDeferred.await()
                    val productResponse = productsDeferred.await()

                    val categories = categoryResponse.mapIndexed { index, cat ->
                        CategoryModel(id = index + 1, title = cat.name, slug = cat.slug)
                    }
                    val products = productResponse.map {
                        ProductModel(id = it.id, image = it.thumbnail, name = it.title, price = it.price)
                    }
                    val banners = products.mapNotNull { it.image }.filter { it.isNotBlank() }.take(5)
                    
                    _uiState.update { 
                        it.copy(
                            categories = listOf(CategoryModel(0, "All", null)) + categories,
                            products = products,
                            banners = banners,
                            isLoading = false
                        ) 
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) }
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

    fun fetchProductsByCategory(slug: String?) {
        viewModelScope.launch {
            try {
                val products = if (slug == null) {
                    productRepository.getProducts()
                } else {
                    productRepository.getProductByCategory(slug)
                }
                _uiState.update { state ->
                    state.copy(products = products.map {
                        ProductModel(id = it.id, image = it.thumbnail, name = it.title, price = it.price)
                    })
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
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
                "placeName" to (cleanedAddress.take(20) + if(cleanedAddress.length > 20) "..." else "")
            )
            val id = userRepository.saveAddress(data)
            if (id != null) {
                userRepository.updateSelectedAddressId(id)
            }
        }
    }

    private fun String.normalizeAddressKey(): String = trim()
        .replace(Regex("\\s+"), " ")
        .lowercase(java.util.Locale.getDefault())
}
