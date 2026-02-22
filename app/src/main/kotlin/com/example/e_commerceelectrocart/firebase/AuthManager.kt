package com.example.e_commerceelectrocart.firebase

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Authentication result wrapper
 */
sealed class AuthResult {
    data class Success(val user: FirebaseUser) : AuthResult()
    data class Error(val message: String, val exception: Exception? = null) : AuthResult()
}

/**
 * Manager class for all authentication operations including email/password,
 * social login (Google, Facebook), and password recovery.
 */
@Singleton
class AuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firebaseManager: FirebaseManager
) {
    companion object {
        private const val TAG = "AuthManager"
        
        // Admin email for role-based access
        const val ADMIN_EMAIL = "admin@electrocart.com"
        
        // Google Sign-In request ID token
        private const val WEB_CLIENT_ID = "YOUR_WEB_CLIENT_ID" // Replace with actual client ID
    }

    private val auth: FirebaseAuth = firebaseManager.auth
    private val db: FirebaseFirestore = firebaseManager.db

    // Google Sign-In Client
    private val googleSignInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(WEB_CLIENT_ID)
            .requestEmail()
            .requestProfile()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    /**
     * Get Google Sign-In Intent
     */
    fun getGoogleSignInIntent() = googleSignInClient.signInIntent

    /**
     * Observe authentication state changes
     */
    fun observeAuthState(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    /**
     * Register new user with email and password
     */
    suspend fun registerWithEmail(email: String, password: String, name: String): AuthResult {
        return try {
            // Validate input
            if (!isValidEmail(email)) {
                return AuthResult.Error("Invalid email format")
            }
            if (!isValidPassword(password)) {
                return AuthResult.Error("Password must be at least 8 characters with uppercase, lowercase, and numbers")
            }

            // Create user
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user

            if (user != null) {
                // Create user profile in Firestore
                createUserProfile(user.uid, name, email, "user")
                
                // Send email verification
                sendEmailVerification()
                
                // Log analytics
                firebaseManager.logEvent("sign_up", mapOf("method" to "email"))
                
                AuthResult.Success(user)
            } else {
                AuthResult.Error("Failed to create user")
            }
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Log.e(TAG, "Invalid credentials", e)
            AuthResult.Error("Invalid email or password format", e)
        } catch (e: FirebaseAuthUserCollisionException) {
            Log.e(TAG, "User already exists", e)
            AuthResult.Error("An account with this email already exists", e)
        } catch (e: Exception) {
            Log.e(TAG, "Registration failed", e)
            AuthResult.Error(e.message ?: "Registration failed", e)
        }
    }

    /**
     * Login with email and password
     */
    suspend fun loginWithEmail(email: String, password: String): AuthResult {
        return try {
            // Validate input
            if (email.isBlank() || password.isBlank()) {
                return AuthResult.Error("Email and password are required")
            }

            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user

            if (user != null) {
                // Check if email is verified
                if (!user.isEmailVerified) {
                    auth.signOut()
                    return AuthResult.Error("Please verify your email before logging in")
                }

                // Update user profile with last login
                updateLastLogin(user.uid)

                // Log analytics
                firebaseManager.logEvent("login", mapOf("method" to "email"))

                // Subscribe to user topic for notifications
                firebaseManager.subscribeToTopic("user_${user.uid}")

                AuthResult.Success(user)
            } else {
                AuthResult.Error("Login failed")
            }
        } catch (e: FirebaseAuthInvalidUserException) {
            Log.e(TAG, "Invalid user", e)
            AuthResult.Error("No account found with this email", e)
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Log.e(TAG, "Invalid credentials", e)
            AuthResult.Error("Incorrect password", e)
        } catch (e: Exception) {
            Log.e(TAG, "Login failed", e)
            AuthResult.Error(e.message ?: "Login failed", e)
        }
    }

    /**
     * Login with Google
     */
    suspend fun loginWithGoogle(idToken: String): AuthResult {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user

            if (user != null) {
                // Check if this is a new user
                if (result.additionalUserInfo?.isNewUser == true) {
                    createUserProfile(user.uid, user.displayName ?: "User", user.email ?: "", "user")
                }

                // Update last login
                updateLastLogin(user.uid)

                // Log analytics
                firebaseManager.logEvent("login", mapOf("method" to "google"))

                // Subscribe to user topic
                firebaseManager.subscribeToTopic("user_${user.uid}")

                AuthResult.Success(user)
            } else {
                AuthResult.Error("Google sign-in failed")
            }
        } catch (e: ApiException) {
            Log.e(TAG, "Google sign-in failed", e)
            AuthResult.Error("Google sign-in failed: ${e.message}", e)
        } catch (e: Exception) {
            Log.e(TAG, "Login failed", e)
            AuthResult.Error(e.message ?: "Login failed", e)
        }
    }

    /**
     * Handle Google Sign-In result
     */
    suspend fun handleGoogleSignInResult(data: android.content.Intent?): AuthResult {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        return try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken != null) {
                loginWithGoogle(idToken)
            } else {
                AuthResult.Error("Failed to get Google ID token")
            }
        } catch (e: ApiException) {
            Log.e(TAG, "Google sign-in failed", e)
            AuthResult.Error("Google sign-in failed: ${e.message}", e)
        }
    }

    /**
     * Login with Facebook
     */
    suspend fun loginWithFacebook(accessToken: String): AuthResult {
        return try {
            // Facebook credential would be handled here
            // For simplicity, using a placeholder - implement with actual Facebook SDK
            AuthResult.Error("Facebook login not fully implemented")
        } catch (e: Exception) {
            Log.e(TAG, "Facebook login failed", e)
            AuthResult.Error(e.message ?: "Facebook login failed", e)
        }
    }

    /**
     * Send password reset email
     */
    suspend fun sendPasswordResetEmail(email: String): AuthResult {
        return try {
            if (!isValidEmail(email)) {
                return AuthResult.Error("Invalid email format")
            }

            auth.sendPasswordResetEmail(email).await()

            // Log analytics
            firebaseManager.logEvent("password_reset_requested", mapOf("method" to "email"))

            AuthResult.Success(auth.currentUser!!)
        } catch (e: FirebaseAuthInvalidUserException) {
            AuthResult.Error("No account found with this email", e)
        } catch (e: Exception) {
            Log.e(TAG, "Password reset failed", e)
            AuthResult.Error(e.message ?: "Failed to send reset email", e)
        }
    }

    /**
     * Send email verification
     */
    suspend fun sendEmailVerification(): AuthResult {
        return try {
            val user = auth.currentUser
            if (user != null && !user.isEmailVerified) {
                user.sendEmailVerification().await()
                AuthResult.Success(user)
            } else {
                AuthResult.Error("No user logged in or already verified")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Email verification failed", e)
            AuthResult.Error(e.message ?: "Failed to send verification email", e)
        }
    }

    /**
     * Update user password (when logged in)
     */
    suspend fun updatePassword(currentPassword: String, newPassword: String): AuthResult {
        return try {
            val user = auth.currentUser
                ?: return AuthResult.Error("No user logged in")

            // Re-authenticate user
            val credential = com.google.firebase.auth.EmailAuthProvider
                .getCredential(user.email!!, currentPassword)
            user.reauthenticate(credential).await()

            // Update password
            if (!isValidPassword(newPassword)) {
                return AuthResult.Error("Password must be at least 8 characters with uppercase, lowercase, and numbers")
            }

            user.updatePassword(newPassword).await()

            // Log analytics
            firebaseManager.logEvent("password_updated")

            AuthResult.Success(user)
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            AuthResult.Error("Current password is incorrect", e)
        } catch (e: Exception) {
            Log.e(TAG, "Password update failed", e)
            AuthResult.Error(e.message ?: "Failed to update password", e)
        }
    }

    /**
     * Update user profile
     */
    suspend fun updateProfile(name: String, phone: String? = null): AuthResult {
        return try {
            val user = auth.currentUser
                ?: return AuthResult.Error("No user logged in")

            // Update Firebase Auth profile
            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build()
            user.updateProfile(profileUpdates).await()

            // Update Firestore profile
            val updates = mutableMapOf<String, Any?>(
                "name" to name,
                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
            phone?.let { updates["phone"] = it }

            db.collection("users").document(user.uid)
                .update(updates)
                .await()

            // Log analytics
            firebaseManager.logEvent("profile_updated")

            AuthResult.Success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Profile update failed", e)
            AuthResult.Error(e.message ?: "Failed to update profile", e)
        }
    }

    /**
     * Delete user account
     */
    suspend fun deleteAccount(): AuthResult {
        return try {
            val user = auth.currentUser
                ?: return AuthResult.Error("No user logged in")

            // Delete user data from Firestore
            db.collection("users").document(user.uid).delete().await()

            // Delete user account
            user.delete().await()

            // Log analytics
            firebaseManager.logEvent("account_deleted")

            AuthResult.Success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Account deletion failed", e)
            AuthResult.Error(e.message ?: "Failed to delete account", e)
        }
    }

    /**
     * Sign out current user
     */
    fun signOut() {
        val userId = firebaseManager.currentUserId
        userId?.let { firebaseManager.unsubscribeFromTopic("user_$it") }
        
        // Sign out from Google if signed in
        googleSignInClient.signOut()
        
        // Sign out from Firebase
        firebaseManager.signOut()

        // Log analytics
        firebaseManager.logEvent("logout")
    }

    /**
     * Check if user is logged in
     */
    fun isLoggedIn(): Boolean = firebaseManager.isUserLoggedIn

    /**
     * Get current user
     */
    fun getCurrentUser(): FirebaseUser? = firebaseManager.currentUser

    /**
     * Validate email format
     */
    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    /**
     * Validate password strength
     */
    private fun isValidPassword(password: String): Boolean {
        if (password.length < 8) return false
        val hasUppercase = password.any { it.isUpperCase() }
        val hasLowercase = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        return hasUppercase && hasLowercase && hasDigit
    }

    /**
     * Create user profile in Firestore
     */
    private suspend fun createUserProfile(userId: String, name: String, email: String, role: String) {
        val userProfile = hashMapOf(
            "uid" to userId,
            "name" to name,
            "email" to email,
            "role" to role,
            "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            "lastLogin" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            "isActive" to true
        )

        db.collection("users").document(userId)
            .set(userProfile)
            .await()
    }

    /**
     * Update last login time
     */
    private suspend fun updateLastLogin(userId: String) {
        try {
            db.collection("users").document(userId)
                .update("lastLogin", com.google.firebase.firestore.FieldValue.serverTimestamp())
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating last login", e)
        }
    }
}
