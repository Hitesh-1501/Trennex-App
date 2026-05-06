package com.example.trennex.viewmodel.auth

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trennex.ui.auth.login.LoginUIState
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class LoginViewModel: ViewModel() {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val _state = MutableStateFlow<LoginUIState>(LoginUIState.Idle)
    val state = _state.asStateFlow()

    fun onPhoneChange(input: String) {
        val phone = input.filter { it.isDigit() }
        _state.value = when{
            phone.isEmpty() -> LoginUIState.Idle
            phone.length < PHONE_NUMBER_LENGTH -> LoginUIState.Idle
            phone.length == PHONE_NUMBER_LENGTH -> LoginUIState.Valid
            else -> LoginUIState.Error("Only $PHONE_NUMBER_LENGTH digits allowed")
        }
    }

    fun sendOtp(activity: Activity, phone: String){
        val input = phone.filter { it.isDigit() }
        if(input.length != PHONE_NUMBER_LENGTH){
            _state.value = LoginUIState.Error("Enter a valid $PHONE_NUMBER_LENGTH digit phone number")
            return
        }
        if(FirebaseApp.getApps(activity).isEmpty()){
            _state.value = LoginUIState.Error(
                "Firebase is not connected. Add google-services.json and enable Phone Authentication."
            )
            return
        }
        val formattedPhone = "$INDIA_COUNTRY_CODE$input"
        _state.value = LoginUIState.Loading
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks(){
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                auth.signInWithCredential(credential)
                    .addOnSuccessListener { _state.value = LoginUIState.AutoVerified }
                    .addOnFailureListener { exception ->
                        _state.value = LoginUIState.Error(
                            exception.localizedMessage ?: "Phone verification failed"
                        )
                    }
            }

            override fun onVerificationFailed(exception: FirebaseException) {
                _state.value = LoginUIState.Error(
                    exception.localizedMessage ?: "Could not send OTP. Please try again."
                )
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
               _state.value = LoginUIState.CodeSent(verificationId, formattedPhone)
            }
        }

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(formattedPhone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun consumeNavigation(){
        _state.value = LoginUIState.Idle
    }

    private companion object {
        const val PHONE_NUMBER_LENGTH = 10
        const val INDIA_COUNTRY_CODE = "+91"
    }
}