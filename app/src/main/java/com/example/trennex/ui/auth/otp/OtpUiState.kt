package com.example.trennex.ui.auth.otp

sealed class OtpUiState {
    object Idle : OtpUiState()
    object Typing: OtpUiState()
    object Ready: OtpUiState()
    object Loading: OtpUiState()
    object Success: OtpUiState()
    data class Error(val message: String): OtpUiState()
    data class Timer(val time : Int , val canResend: Boolean) : OtpUiState()
}