package com.example.trennex.viewmodel.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trennex.ui.auth.login.LoginUIState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel: ViewModel() {
    private val _state = MutableStateFlow<LoginUIState>(LoginUIState.Idle)
    val state = _state.asStateFlow()

    fun onPhoneChange(input: String) {
        val phone = input.filter { it.isDigit() }
        when {
            phone.isEmpty() -> {
                _state.value = LoginUIState.Idle
            }

            phone.length < 10 -> {
                _state.value = LoginUIState.Idle
            }

            phone.length == 10 -> {
                _state.value = LoginUIState.Valid
            }

            phone.length > 10 -> {
                _state.value = LoginUIState.Error("Only 10 digits allowed")
            }
        }
    }

    fun onGetOtpClick(phone: String){
        val input = phone.filter { it.isDigit() }
        if(input.length != 10){
            _state.value = LoginUIState.Error("Enter valid 10 digit number")
            return
        }
        _state.value = LoginUIState.Loading
        viewModelScope.launch {
            try {
                delay(1500)
                _state.value = LoginUIState.Success
            }catch (e: Exception){
                _state.value = LoginUIState.Error("Connection Failed ${e.message}")
            }
        }
    }
}