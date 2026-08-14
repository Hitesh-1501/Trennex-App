package com.example.trennex.viewmodel.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        checkLoginStatus()
    }

    fun checkLoginStatus() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            _uiState.update { it.copy(isLoggedIn = true, phoneNumber = currentUser.phoneNumber, isLoading = true) }
            loadUserDetails(currentUser.uid)
        } else {
            _uiState.update { it.copy(isLoggedIn = false, userName = null, phoneNumber = null, isLoading = false) }
        }
    }

    private fun loadUserDetails(uid: String) {
        firestore.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { snapshot ->
                val name = snapshot.getString("name").orEmpty().trim()
                _uiState.update { it.copy(userName = if (name.isNotEmpty()) name else null, isLoading = false) }
            }
            .addOnFailureListener { e ->
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
    }

    fun signOut() {
        auth.signOut()
        checkLoginStatus()
    }
}