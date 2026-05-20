package com.example.trennex.viewmodel.profile

import android.app.Activity
import android.util.Patterns
import androidx.lifecycle.ViewModel
import com.example.trennex.ui.profile.AccountDetailsUiStat
import com.example.trennex.ui.profile.AccountOtpState
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit

class AccountDetailsViewModel: ViewModel() {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private val _state = MutableStateFlow(AccountDetailsUiStat())
    val state = _state.asStateFlow()

    private var verificationId: String = ""

    fun loadAccountDetails(){
        val user = auth.currentUser ?: return
        _state.value = _state.value.copy(
            phone = user.phoneNumber.orEmpty().removePrefix(COUNTRY_CODE),
            isPhoneVerified = true,
            isPhoneEditable = false
        )
        firestore.collection(USERS_COLLECTION)
            .document(user.uid)
            .get()
            .addOnSuccessListener {snapshot ->
                val phone  = snapshot.getString(PHONE_FIELD).orEmpty().removePrefix(COUNTRY_CODE)
                _state.value = _state.value.copy(
                    phone = phone.ifBlank { _state.value.phone },
                    name = snapshot.getString(NAME_FIELD).orEmpty(),
                    email = snapshot.getString(EMAIL_FIELD).orEmpty(),
                    message = null
                )
            }
            .addOnFailureListener {
                _state.value = _state.value.copy(message = "Could not load account details")
            }
    }


    fun enablePhoneEditing(){
        _state.value = _state.value.copy(
            isPhoneEditable = true,
            isPhoneVerified = false,
            otpState = AccountOtpState.Idle,
            message = null
        )
    }

    fun requestPhoneOtp(activity: Activity, rawPhone: String){
        val phone = rawPhone.filter(Char::isDigit )
        if(phone.length != PHONE_NUMBER_LENGTH){
            _state.value = _state.value.copy(message = "Enter a valid $PHONE_NUMBER_LENGTH digit mobile number")
            return
        }
        val currentPhone = _state.value.phone.filter(Char::isDigit)
        if (phone == currentPhone) {
            _state.value = _state.value.copy(message = "Please enter a new mobile number")
            return
        }
        if (FirebaseApp.getApps(activity).isEmpty()) {
            _state.value = _state.value.copy(message = "Firebase is not connected. Add google-services.json and enable Phone Authentication.")
            return
        }

        verificationId = ""
        _state.value = _state.value.copy(otpState = AccountOtpState.Sending(phone), message = null)
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks(){
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                updateAuthPhoneNumber(credential,phone)
            }

            override fun onVerificationFailed(exception: FirebaseException) {
                _state.value = _state.value.copy(
                    otpState = AccountOtpState.Error(
                        phone,
                        exception.localizedMessage ?: "Phone verification failed. Please try again."
                    )
                )
            }

            override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                verificationId = id
                _state.value = _state.value.copy(otpState = AccountOtpState.CodeSent(phone))
            }
        }
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber("$COUNTRY_CODE$phone")
            .setTimeout(OTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun onOtpChanged(code: String){
        val phone = otpPhone() ?: return
        _state.value = if(code.filter(Char::isDigit).length == OTP_LENGTH){
            _state.value.copy(otpState = AccountOtpState.Ready(phone))
        }else{
            _state.value.copy(otpState = AccountOtpState.CodeSent(phone))
        }
    }

    fun verifyPhoneOtp(code: String) {
        val phone = otpPhone() ?: return
        val otp = code.filter(Char::isDigit)
        if(otp.length != OTP_LENGTH){
            _state.value = _state.value.copy(otpState = AccountOtpState.Error(phone, "Enter the $OTP_LENGTH digit OTP"))
            return
        }
        if(verificationId.isBlank()){
            _state.value = _state.value.copy(otpState = AccountOtpState.Error(phone, "Missing verification session. Please request OTP again."))
            return
        }
        _state.value = _state.value.copy(otpState = AccountOtpState.Verifying(phone))
        val credential = PhoneAuthProvider.getCredential(verificationId,otp)
        updateAuthPhoneNumber(credential,phone)
    }

    fun saveDetails(name: String, email: String, rawPhone: String){
        val user = auth.currentUser ?: return
        val trimmedName = name.trim()
        val trimmedEmail = email.trim()
        val phone = rawPhone.filter(Char::isDigit)

        when {
            phone.length != PHONE_NUMBER_LENGTH -> {
                _state.value = _state.value.copy(message = "Enter a valid $PHONE_NUMBER_LENGTH digit mobile number")
                return
            }
            !_state.value.isPhoneVerified -> {
                _state.value = _state.value.copy(message = "Please verify your new mobile number")
                return
            }
            trimmedName.isBlank() -> {
                _state.value = _state.value.copy(message = "Name is required")
                return
            }
            trimmedEmail.isNotBlank() && !Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches() -> {
                _state.value = _state.value.copy(message = "Enter a valid email")
                return
            }
        }
        _state.value = _state.value.copy(isSaving = true, message = null)
        val details = mapOf(
            NAME_FIELD to trimmedName,
            EMAIL_FIELD to trimmedEmail,
            PHONE_FIELD to "$COUNTRY_CODE$phone"
        )
        firestore.collection(USERS_COLLECTION)
            .document(user.uid)
            .set(details, SetOptions.merge())
            .addOnSuccessListener {
                _state.value = _state.value.copy(
                    phone = phone,
                    name = trimmedName,
                    email = trimmedEmail,
                    isSaving = false,
                    message = "Details updated successfully"
                )
            }
            .addOnFailureListener {exception ->
                _state.value = _state.value.copy(
                    isSaving = false,
                    message = exception.localizedMessage ?: "Could not update details. Please try again."
                )
            }
    }

    fun clearMessage(){
        _state.value = _state.value.copy(message = null)
    }
    fun resetOtpState() {
        _state.value = _state.value.copy(otpState = AccountOtpState.Idle)
    }

    private fun updateAuthPhoneNumber(credential: PhoneAuthCredential, phone: String){
        val user = auth.currentUser
        if(user == null){
            _state.value = _state.value.copy(otpState = AccountOtpState.Error(phone, "Please login again to update your phone number."))
            return
        }
        user.updatePhoneNumber(credential)
            .addOnSuccessListener {
                firestore.collection(USERS_COLLECTION)
                    .document(user.uid)
                    .set(mapOf(PHONE_FIELD to "$COUNTRY_CODE$phone"), SetOptions.merge())
                _state.value = _state.value.copy(
                    phone = phone,
                    isPhoneEditable = false,
                    isPhoneVerified = true,
                    otpState = AccountOtpState.Verified(phone),
                    message = null
                )
            }
            .addOnFailureListener {exception ->
                _state.value = _state.value.copy(
                    otpState = AccountOtpState.Error(
                        phone,
                        exception.localizedMessage ?: "Invalid OTP. Please try again."
                    )
                )
            }

    }


    private fun otpPhone(): String? {
        return when(val otpState = _state.value.otpState) {
            is AccountOtpState.Sending -> otpState.phone
            is AccountOtpState.CodeSent -> otpState.phone
            is AccountOtpState.Ready -> otpState.phone
            is AccountOtpState.Verifying -> otpState.phone
            is AccountOtpState.Verified -> otpState.phone
            is AccountOtpState.Error -> otpState.phone
            AccountOtpState.Idle -> null
        }
    }

    private companion object {
        const val USERS_COLLECTION = "users"
        const val NAME_FIELD = "name"
        const val EMAIL_FIELD = "email"
        const val PHONE_FIELD = "phone"
        const val COUNTRY_CODE = "+91"
        const val PHONE_NUMBER_LENGTH = 10
        const val OTP_LENGTH = 6
        const val OTP_TIMEOUT_SECONDS = 60L
    }
}