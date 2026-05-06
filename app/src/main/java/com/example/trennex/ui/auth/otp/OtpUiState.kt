package com.example.trennex.ui.auth.otp

sealed class OtpUiState {
    data object Idle : OtpUiState()
    data object Typing: OtpUiState()
    data object Ready: OtpUiState()
    data object Loading: OtpUiState()
    data object Success: OtpUiState()
    data class Error(val message: String): OtpUiState()
    data class Timer(val time : Int , val canResend: Boolean) : OtpUiState()
}