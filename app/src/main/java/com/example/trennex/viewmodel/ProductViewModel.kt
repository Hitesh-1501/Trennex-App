package com.example.trennex.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trennex.data.model.ProductResponse
import com.example.trennex.data.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.lang.Exception

class ProductViewModel: ViewModel() {
    private val repository = ProductRepository()
    private val _products = MutableStateFlow<List<ProductResponse>>(emptyList())
    val products : StateFlow<List<ProductResponse>> = _products.asStateFlow()

    fun fetchProducts(){
        viewModelScope.launch {
            try {
                val data  = repository.getProducts()
                _products.value = data
            }catch (e: Exception){
                e.printStackTrace()
            }
        }
    }
}