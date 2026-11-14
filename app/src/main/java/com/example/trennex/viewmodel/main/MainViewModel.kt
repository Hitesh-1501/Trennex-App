package com.example.trennex.viewmodel.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trennex.repository.user.UserRepository
import com.example.trennex.utils.cart.CartStore
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

class MainViewModel : ViewModel() {
    private val userRepository = UserRepository()
    private val auth = FirebaseAuth.getInstance()
    private var addressJob: Job? = null
    private val authListener = FirebaseAuth.AuthStateListener { observeAddress() }

    val cartCount: StateFlow<Int> = CartStore.totalQuantity
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _selectedAddress = MutableStateFlow<String?>(null)
    val selectedAddress: StateFlow<String?> = _selectedAddress.asStateFlow()

    init {
        auth.addAuthStateListener(authListener)
    }

    private fun observeAddress() {
        addressJob?.cancel()
        _selectedAddress.value = null
        addressJob = viewModelScope.launch {
            combine(
                userRepository.observeSavedAddresses().catch {
                    android.util.Log.w("MainViewModel", "Address listener stopped", it)
                    emit(emptyList())
                },
                userRepository.observeSelectedAddressId().catch {
                    android.util.Log.w("MainViewModel", "Selected-address listener stopped", it)
                    emit(null)
                }
            ) { addresses, selectedId ->
                val selected = addresses.find { it.id == selectedId } ?: addresses.firstOrNull()
                selected?.displayAddress
            }.collectLatest { address ->
                _selectedAddress.value = address
            }
        }
    }
    
    fun updateSelectedAddress(address: String?) {
        _selectedAddress.value = address
    }

    override fun onCleared() {
        auth.removeAuthStateListener(authListener)
        super.onCleared()
    }
}
