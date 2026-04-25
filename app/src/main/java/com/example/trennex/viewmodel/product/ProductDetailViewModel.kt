package com.example.trennex.viewmodel.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trennex.data.model.ProductResponse
import com.example.trennex.repository.product.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProductDetailViewModel : ViewModel(){
    private val repository = ProductRepository()
    private val _products = MutableStateFlow<ProductResponse?>(null)
    val products : StateFlow<ProductResponse?> = _products.asStateFlow()

    fun fetchProductDetail(id: Int){
        viewModelScope.launch {
            try {
                val data = repository.getProductDetail(id)
                _products.value = data
            }catch (e: Exception){
                e.printStackTrace()
            }
        }
    }
}