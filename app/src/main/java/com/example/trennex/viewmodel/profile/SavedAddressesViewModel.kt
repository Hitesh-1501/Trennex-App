package com.example.trennex.viewmodel.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trennex.repository.user.AddressEntity
import com.example.trennex.repository.user.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SavedAddressesViewModel : ViewModel() {
    private val userRepository = UserRepository()

    private val _addresses = MutableStateFlow<List<AddressEntity>>(emptyList())
    val addresses: StateFlow<List<AddressEntity>> = _addresses.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        fetchAddresses()
    }

    private fun fetchAddresses() {
        viewModelScope.launch {
            val userDetails = userRepository.getUserDetails()
            val userPhone = userDetails?.get("phone").orEmpty()

            userRepository.observeSavedAddresses().collect { list ->
                val updatedList = list.map { address ->
                    if (userPhone.isNotBlank()) {
                        address.copy(mobile = userPhone)
                    } else {
                        address
                    }
                }
                _addresses.value = updatedList
            }
        }
    }

    fun deleteAddress(address: AddressEntity, onComplete: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                userRepository.deleteAddress(address.id)
                onComplete()
            } catch (e: Exception) {
                // handle error
            } finally {
                _isLoading.value = false
            }
        }
    }
}
