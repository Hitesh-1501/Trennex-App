package com.example.trennex.data.local

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object AccountScope {
    const val GUEST = "guest"
    const val LEGACY_UNASSIGNED = "__legacy_unassigned__"

    private val mutableOwner = MutableStateFlow(GUEST)
    val owner: StateFlow<String> = mutableOwner

    @Volatile
    private var auth: FirebaseAuth? = null
    private val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        mutableOwner.value = firebaseAuth.currentUser?.uid?.let(::forUser) ?: GUEST
    }

    fun initialize(firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()) {
        if (auth != null) return
        synchronized(this) {
            if (auth == null) {
                mutableOwner.value = firebaseAuth.currentUser?.uid?.let(::forUser) ?: GUEST
                auth = firebaseAuth
                firebaseAuth.addAuthStateListener(listener)
            }
        }
    }

    fun forUser(uid: String): String = "user:$uid"
    fun currentOwner(): String = auth?.currentUser?.uid?.let(::forUser) ?: GUEST
    fun uid(ownerId: String): String? = ownerId.removePrefix("user:")
        .takeIf { ownerId.startsWith("user:") && it.isNotBlank() }
}
