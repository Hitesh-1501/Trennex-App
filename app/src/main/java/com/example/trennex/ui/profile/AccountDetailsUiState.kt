package com.example.trennex.ui.profile

data class AccountDetailsUiStat(
    val phone: String = "",
    val name: String = "",
    val email: String = "",
    val isPhoneEditable: Boolean = false,
    val isPhoneVerified : Boolean = true,
    val isSaving: Boolean = false,
    val otpState: AccountOtpState = AccountOtpState.Idle,
    val message: String? = null
)

sealed class AccountOtpState{
    data object Idle: AccountOtpState()
    data class Sending(val phone: String) : AccountOtpState()
    data class CodeSent(val phone: String) : AccountOtpState()
    data class Ready(val phone: String) : AccountOtpState()
    data class Verifying(val phone: String) : AccountOtpState()
    data class Verified(val phone: String) : AccountOtpState()
    data class Error(val phone: String, val message: String) : AccountOtpState()
}