package com.example.trennex.ui.auth.otp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OtpViewModel: ViewModel(){
    private val _state = MutableStateFlow<OtpUiState>(OtpUiState.Idle)
    val state = _state.asStateFlow()

    private var otp = ""

    fun onOtpChanged(value: String){
        otp = value
        _state.value = when{
            value.isEmpty() -> OtpUiState.Idle
            value.length < 4 -> OtpUiState.Typing
            value.length == 4 -> OtpUiState.Ready
            else -> OtpUiState.Error("Invalid OTP")
        }
    }

    fun verifyOtp(value: String){
        if(value.length != 4){
            _state.value = OtpUiState.Error("Enter valid OTP")
            return
        }
        _state.value = OtpUiState.Loading

        viewModelScope.launch {
            delay(1500)
            if(otp == "1234"){
                _state.value = OtpUiState.Success
            }else{
                _state.value = OtpUiState.Error("Invalid OTP")
            }
        }
    }

    fun setTimer(){
        viewModelScope.launch {
            for(i in 10 downTo 0){
                _state.value = OtpUiState.Timer(i,i == 0)
                delay(1000)
            }
        }
    }
}