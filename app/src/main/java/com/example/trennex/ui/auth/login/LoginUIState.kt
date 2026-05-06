package com.example.trennex.ui.auth.login

sealed class LoginUIState {
    data object Idle : LoginUIState()
    data object Valid: LoginUIState()
    data object Loading: LoginUIState()

    data class CodeSent(
        val verificationId: String,
        val phoneNumber: String
    ): LoginUIState()

    data object AutoVerified: LoginUIState()
    data class Error(val message: String): LoginUIState()
}