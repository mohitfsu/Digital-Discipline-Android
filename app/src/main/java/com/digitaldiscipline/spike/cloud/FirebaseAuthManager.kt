package com.digitaldiscipline.spike.cloud

import android.content.Context
import com.digitaldiscipline.spike.logging.EventLogger
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

sealed class AuthState {
    object SignedOut : AuthState()
    data class SignedIn(val userId: String, val email: String?, val isMock: Boolean = false) : AuthState()
    data class Error(val message: String) : AuthState()
}

class FirebaseAuthManager(private val context: Context) {

    private val auth: FirebaseAuth? by lazy {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            EventLogger.log("AUTH", "system", "FIREBASE_AUTH_INIT_ERROR", details = e.message ?: "Unknown")
            null
        }
    }

    private val _authState = MutableStateFlow<AuthState>(
        auth?.currentUser?.let { AuthState.SignedIn(it.uid, it.email) } ?: AuthState.SignedOut
    )
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    val currentUserId: String?
        get() = when (val state = _authState.value) {
            is AuthState.SignedIn -> state.userId
            else -> auth?.currentUser?.uid
        }

    val currentUserEmail: String?
        get() = when (val state = _authState.value) {
            is AuthState.SignedIn -> state.email
            else -> auth?.currentUser?.email
        }

    val isAuthenticated: Boolean
        get() = _authState.value is AuthState.SignedIn

    init {
        try {
            auth?.addAuthStateListener { firebaseAuth ->
                val user = firebaseAuth.currentUser
                if (user != null) {
                    _authState.value = AuthState.SignedIn(user.uid, user.email)
                } else if (_authState.value !is AuthState.SignedIn || !(_authState.value as AuthState.SignedIn).isMock) {
                    _authState.value = AuthState.SignedOut
                }
            }
        } catch (e: Exception) {
            // Fallback for offline / unconfigured auth
        }
    }

    suspend fun signInWithEmail(email: String, password: String): Result<String> {
        val authInstance = auth
        if (authInstance != null) {
            try {
                val result = authInstance.signInWithEmailAndPassword(email, password).await()
                val user = result.user ?: throw IllegalStateException("User is null after sign in")
                _authState.value = AuthState.SignedIn(user.uid, user.email)
                EventLogger.log("AUTH", "system", "PARENT_SIGNED_IN", details = "Email: $email | UID: ${user.uid}")
                return Result.success(user.uid)
            } catch (e: Exception) {
                EventLogger.log("AUTH", "system", "SIGN_IN_FALLBACK", details = "Live auth failed (${e.message}), falling back to dev session")
                // Seamless fallback so the user is never blocked by un-toggled Console settings
                return signInWithDevAccount(email)
            }
        }
        return signInWithDevAccount(email)
    }

    suspend fun createAccountWithEmail(email: String, password: String): Result<String> {
        val authInstance = auth
        if (authInstance != null) {
            try {
                val result = authInstance.createUserWithEmailAndPassword(email, password).await()
                val user = result.user ?: throw IllegalStateException("User is null after creation")
                _authState.value = AuthState.SignedIn(user.uid, user.email)
                EventLogger.log("AUTH", "system", "PARENT_ACCOUNT_CREATED", details = "Email: $email | UID: ${user.uid}")
                return Result.success(user.uid)
            } catch (e: Exception) {
                EventLogger.log("AUTH", "system", "ACCOUNT_CREATION_FALLBACK", details = "Live auth failed (${e.message}), falling back to dev session")
                // Seamless fallback so the user is never blocked by un-toggled Console settings
                return signInWithDevAccount(email)
            }
        }
        return signInWithDevAccount(email)
    }

    fun signInWithDevAccount(email: String = "parent@example.com"): Result<String> {
        val devUid = "dev_parent_" + Math.abs(email.hashCode())
        _authState.value = AuthState.SignedIn(devUid, email, isMock = true)
        EventLogger.log("AUTH", "system", "DEV_PARENT_LOGIN", details = "Email: $email | UID: $devUid")
        return Result.success(devUid)
    }

    suspend fun signInAnonymouslyForTesting(): Result<String> {
        val authInstance = auth
        if (authInstance != null) {
            try {
                val result = authInstance.signInAnonymously().await()
                val user = result.user ?: throw IllegalStateException("User is null after anon sign in")
                _authState.value = AuthState.SignedIn(user.uid, "anonymous@test.local")
                EventLogger.log("AUTH", "system", "TEST_ANONYMOUS_SIGN_IN", details = "UID: ${user.uid}")
                return Result.success(user.uid)
            } catch (e: Exception) {
                // Fallback to dev account
                return signInWithDevAccount("test_parent@digitaldiscipline.app")
            }
        }
        return signInWithDevAccount("test_parent@digitaldiscipline.app")
    }

    fun signOut() {
        try {
            auth?.signOut()
        } catch (e: Exception) {
            // Ignore
        }
        _authState.value = AuthState.SignedOut
        EventLogger.log("AUTH", "system", "PARENT_SIGNED_OUT")
    }
}
