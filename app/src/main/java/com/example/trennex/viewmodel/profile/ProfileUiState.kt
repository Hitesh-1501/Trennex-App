package com.example.trennex.viewmodel.profile

data class ProfileUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val userName: String? = null,
    val phoneNumber: String? = null,
    val error: String? = null
)