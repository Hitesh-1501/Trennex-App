package com.example.trennex.viewmodel.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trennex.ui.auth.otp.OtpUiState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OtpViewModel: ViewModel(){
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val _state = MutableStateFlow<OtpUiState>(OtpUiState.Idle)
    val state = _state.asStateFlow()

    private var otp = ""

    fun onOtpChanged(value: String){
        otp = value.filter { it.isDigit() }
        _state.value = when {
            otp.isEmpty() -> OtpUiState.Idle
            otp.length < OTP_LENGTH -> OtpUiState.Typing
            otp.length == OTP_LENGTH -> OtpUiState.Ready
            else -> OtpUiState.Error("Invalid OTP")
        }
    }

    fun verifyOtp(verificationId: String, value: String){
        val code = value.filter { it.isDigit() }
        if(verificationId.isBlank()){
            _state.value = OtpUiState.Error("Missing verification session. Please request OTP again.")
            return
        }
        if(code.length != OTP_LENGTH){
            _state.value = OtpUiState.Error("Enter valid OTP")
            return
        }
        _state.value = OtpUiState.Loading
        val credential = PhoneAuthProvider.getCredential(verificationId,code)
        auth.signInWithCredential(credential)
            .addOnSuccessListener { _state.value = OtpUiState.Success }
            .addOnFailureListener { exception ->
                _state.value = OtpUiState.Error(exception.localizedMessage ?: "Invalid OTP")
            }
    }

    fun setTimer(){
        viewModelScope.launch {
            for(i in 60 downTo 0){
                _state.value = OtpUiState.Timer(i,i == 0)
                delay(1000)
            }
        }
    }
    private companion object {
        const val OTP_LENGTH = 6
    }
}