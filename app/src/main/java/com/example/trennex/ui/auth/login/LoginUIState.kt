package com.example.trennex.ui.auth.login

sealed class LoginUIState {
    object Idle : LoginUIState()
    object Valid: LoginUIState()
    object Loading: LoginUIState()
    object Success: LoginUIState()
    data class Error(val message: String): LoginUIState()
}