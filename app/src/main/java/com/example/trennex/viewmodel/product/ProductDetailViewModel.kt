package com.example.trennex.viewmodel.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trennex.data.model.ProductResponse
import com.example.trennex.repository.product.ProductRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException

class ProductDetailViewModel : ViewModel(){
    private val repository = ProductRepository()
    private val _products = MutableStateFlow<ProductResponse?>(null)
    val products : StateFlow<ProductResponse?> = _products.asStateFlow()
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    private var fetchJob: Job? = null

    fun fetchProductDetail(id: Int){
        fetchJob?.cancel()
        _isLoading.value = true
        _error.value = null
        fetchJob = viewModelScope.launch {
            try {
                val data = withTimeout(15_000) { repository.getProductDetail(id) }
                _products.value = data
            }catch (e: Exception){
                if (e is CancellationException && e !is TimeoutCancellationException) throw e
                _error.value = e.localizedMessage ?: "Could not load product"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
