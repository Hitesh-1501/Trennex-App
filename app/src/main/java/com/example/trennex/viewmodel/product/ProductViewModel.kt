package com.example.trennex.viewmodel.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trennex.data.model.CategoryApiResponse
import com.example.trennex.data.model.ProductResponse
import com.example.trennex.repository.product.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.lang.Exception

class ProductViewModel: ViewModel() {
    private val repository = ProductRepository()
    private val _products = MutableStateFlow<List<ProductResponse>>(emptyList())
    val products : StateFlow<List<ProductResponse>> = _products.asStateFlow()

    private val _categories = MutableStateFlow<List<CategoryApiResponse>>(emptyList())
    val categories : StateFlow<List<CategoryApiResponse>> = _categories.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun fetchProducts(){
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val data  = repository.getProducts()
                _products.value = data
            }catch (e: Exception){
                _error.value = e.localizedMessage
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchCategories(){
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val data = repository.getCategory()
                _categories.value = data
            }catch (e: Exception){
                _error.value = e.localizedMessage
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
    fun fetchProductsByCategory(slug: String){
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                _products.value = repository.getProductByCategory(slug)
            }catch (e: Exception){
                _error.value = e.localizedMessage
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}