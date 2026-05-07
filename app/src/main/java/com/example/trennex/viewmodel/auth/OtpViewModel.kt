package com.example.trennex.viewmodel.auth

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trennex.ui.auth.otp.OtpUiState
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class OtpViewModel: ViewModel(){
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val _state = MutableStateFlow<OtpUiState>(OtpUiState.Idle)
    val state = _state.asStateFlow()

    private var otp = ""
    private var timerJob: Job? = null

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

    fun resendOtp(activity: Activity,phone: String){
        val formattedPhone = phone.filterIndexed { index, ch ->
            ch.isDigit() || (index == 0 && ch == '+')
        }
        if (formattedPhone.isBlank()) {
            _state.value = OtpUiState.Error("Missing phone number. Please go back and request OTP again.")
            return
        }
        if (FirebaseApp.getApps(activity).isEmpty()) {
            _state.value = OtpUiState.Error(
                "Firebase is not connected. Add google-services.json and enable Phone Authentication."
            )
            return
        }
        _state.value = OtpUiState.Loading
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                auth.signInWithCredential(credential)
                    .addOnSuccessListener { _state.value = OtpUiState.Success }
                    .addOnFailureListener {exception ->
                        _state.value = OtpUiState.Error(
                            exception.localizedMessage ?: "Phone verification failed"
                        )
                    }
            }

            override fun onVerificationFailed(exception: FirebaseException) {
                _state.value = OtpUiState.Error(
                    exception.localizedMessage ?: "Could not resend OTP. Please try again."
                )
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                _state.value = OtpUiState.CodeResent(verificationId)
            }
        }
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(formattedPhone)
            .setTimeout(TIMER_SECONDS.toLong(), TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun setTimer(){
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            for(i in TIMER_SECONDS downTo 0){
                _state.value = OtpUiState.Timer(i,i == 0)
                delay(1000)
            }
        }
    }
    private companion object {
        const val OTP_LENGTH = 6
        const val TIMER_SECONDS = 60
    }
}