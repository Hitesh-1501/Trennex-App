package com.example.trennex.ui.location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trennex.repository.user.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LocationViewModel : ViewModel() {
    private val userRepository = UserRepository()

    private val _uiState = MutableStateFlow(LocationUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadUserDetails()
    }

    private fun loadUserDetails() {
        viewModelScope.launch {
            userRepository.getUserDetails()?.let { details ->
                _uiState.update { it.copy(
                    userName = details["name"].orEmpty(),
                    userPhone = details["phone"].orEmpty()
                )}
            }
        }
    }

    fun saveAddress(addressData: Map<String, Any>) {
        viewModelScope.launch {
            _uiState.update { it.copy(status = LocationUiState.Status.Loading) }
            try {
                val id = userRepository.saveAddress(addressData)
                if (id != null) {
                    userRepository.updateSelectedAddressId(id)
                    _uiState.update { it.copy(status = LocationUiState.Status.Success) }
                } else {
                    _uiState.update { it.copy(status = LocationUiState.Status.Error, error = "Failed to save address") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(status = LocationUiState.Status.Error, error = e.localizedMessage) }
            }
        }
    }

    fun updateAddress(addressId: String, addressData: Map<String, Any>) {
        viewModelScope.launch {
            _uiState.update { it.copy(status = LocationUiState.Status.Loading) }
            try {
                userRepository.updateAddress(addressId, addressData)
                userRepository.updateSelectedAddressId(addressId)
                _uiState.update { it.copy(status = LocationUiState.Status.Success) }
            } catch (e: Exception) {
                _uiState.update { it.copy(status = LocationUiState.Status.Error, error = e.localizedMessage) }
            }
        }
    }
    
    fun resetStatus() {
        _uiState.update { it.copy(status = LocationUiState.Status.Idle, error = null) }
    }
}

data class LocationUiState(
    val status: Status = Status.Idle,
    val userName: String = "",
    val userPhone: String = "",
    val error: String? = null
) {
    enum class Status { Idle, Loading, Success, Error }
}
