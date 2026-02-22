package com.example.e_commerceelectrocart.firebase

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized Firebase Manager for all Firebase operations.
 * Provides a unified interface for Authentication, Firestore, Storage, Functions, and Analytics.
 */
@Singleton
class FirebaseManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "FirebaseManager"
        
        // Firestore Collections
        const val COLLECTION_USERS = "users"
        const val COLLECTION_PRODUCTS = "products"
        const val COLLECTION_ORDERS = "orders"
        const val COLLECTION_CATEGORIES = "categories"
        const val COLLECTION_ADDRESSES = "addresses"
        const val COLLECTION_NOTIFICATIONS = "notifications"
        const val COLLECTION_CHATS = "chats"
        
        // User Roles
        const val ROLE_ADMIN = "admin"
        const val ROLE_USER = "user"
        const val ROLE_VENDOR = "vendor"
    }

    // Firebase Services
    val auth: FirebaseAuth = FirebaseAuth.getInstance()
    val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    val storage: FirebaseStorage = FirebaseStorage.getInstance()
    val functions: FirebaseFunctions = FirebaseFunctions.getInstance()
    val analytics: FirebaseAnalytics = FirebaseAnalytics.getInstance(context)
    val crashlytics: FirebaseCrashlytics = FirebaseCrashlytics.getInstance()

    // Storage References
    val productsStorageRef: StorageReference = storage.reference.child("products")
    val profilesStorageRef: StorageReference = storage.reference.child("profiles")
    val bannersStorageRef: StorageReference = storage.reference.child("banners")

    // Current User
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    val isUserLoggedIn: Boolean
        get() = currentUser != null

    val currentUserId: String?
        get() = currentUser?.uid

    val currentUserEmail: String?
        get() = currentUser?.email

    /**
     * Check if current user is admin
     */
    suspend fun isCurrentUserAdmin(): Boolean {
        val userId = currentUserId ?: return false
        return try {
            val document = db.collection(COLLECTION_USERS).document(userId).get().await()
            document.getString("role") == ROLE_ADMIN
        } catch (e: Exception) {
            Log.e(TAG, "Error checking admin status", e)
            false
        }
    }

    /**
     * Log analytics event
     */
    fun logEvent(eventName: String, params: Map<String, Any>? = null) {
        val bundle = android.os.Bundle()
        params?.forEach { (key, value) ->
            when (value) {
                is String -> bundle.putString(key, value)
                is Int -> bundle.putInt(key, value)
                is Long -> bundle.putLong(key, value)
                is Double -> bundle.putDouble(key, value)
                is Boolean -> bundle.putBoolean(key, value)
            }
        }
        analytics.logEvent(eventName, bundle)
        Log.d(TAG, "Analytics event logged: $eventName")
    }

    /**
     * Log crash to Firebase Crashlytics
     */
    fun logCrash(message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            crashlytics.recordException(throwable)
        } else {
            crashlytics.log(message)
        }
    }

    /**
     * Set user properties for analytics
     */
    fun setUserProperties(userId: String, role: String, email: String?) {
        analytics.setUserId(userId)
        analytics.setUserProperty("user_role", role)
        email?.let { analytics.setUserProperty("user_email_domain", it.substringAfter("@")) }
    }

    /**
     * Subscribe to FCM topic
     */
    fun subscribeToTopic(topic: String) {
        FirebaseMessaging.getInstance().subscribeToTopic(topic)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Subscribed to topic: $topic")
                } else {
                    Log.e(TAG, "Failed to subscribe to topic: $topic", task.exception)
                }
            }
    }

    /**
     * Unsubscribe from FCM topic
     */
    fun unsubscribeFromTopic(topic: String) {
        FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Unsubscribed from topic: $topic")
                } else {
                    Log.e(TAG, "Failed to unsubscribe from topic: $topic", task.exception)
                }
            }
    }

    /**
     * Get FCM token for push notifications
     */
    fun getFCMToken(onTokenReceived: (String) -> Unit) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d(TAG, "FCM Token: $token")
                onTokenReceived(token)
            } else {
                Log.e(TAG, "Failed to get FCM token", task.exception)
            }
        }
    }

    /**
     * Upload image to Firebase Storage
     */
    suspend fun uploadImage(
        storageRef: StorageReference,
        imageUri: Uri,
        onProgress: (Double) -> Unit = {}
    ): String {
        val fileName = "${System.currentTimeMillis()}_${imageUri.lastPathSegment}"
        val ref = storageRef.child(fileName)

        return try {
            val uploadTask = ref.putFile(imageUri)
            
            // Monitor upload progress
            uploadTask.addOnProgressListener { taskSnapshot ->
                val progress = (100.0 * taskSnapshot.bytesTransferred) / taskSnapshot.totalByteCount
                onProgress(progress)
            }.await()

            // Get download URL
            ref.downloadUrl.await().toString()
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading image", e)
            throw e
        }
    }

    /**
     * Delete image from Firebase Storage
     */
    suspend fun deleteImage(imageUrl: String) {
        try {
            val ref = storage.getReferenceFromUrl(imageUrl)
            ref.delete().await()
            Log.d(TAG, "Image deleted successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting image", e)
        }
    }

    /**
     * Generic Firestore query as Flow
     */
    fun <T> getCollectionFlow(
        collection: String,
        query: Query? = null,
        mapper: (com.google.firebase.firestore.DocumentSnapshot) -> T
    ): Flow<List<T>> = callbackFlow {
        val reference = query ?: db.collection(collection)
        
        val listener = reference.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error fetching collection: $collection", error)
                close(error)
                return@addSnapshotListener
            }

            val items = snapshot?.documents?.mapNotNull { doc ->
                try {
                    mapper(doc)
                } catch (e: Exception) {
                    Log.e(TAG, "Error mapping document: ${doc.id}", e)
                    null
                }
            } ?: emptyList()
            
            trySend(items)
        }

        awaitClose { listener.remove() }
    }

    /**
     * Get document by ID
     */
    suspend fun <T> getDocument(
        collection: String,
        documentId: String,
        mapper: (com.google.firebase.firestore.DocumentSnapshot) -> T
    ): T? {
        return try {
            val doc = db.collection(collection).document(documentId).get().await()
            if (doc.exists()) mapper(doc) else null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting document: $documentId", e)
            null
        }
    }

    /**
     * Create or update document
     */
    suspend fun setDocument(
        collection: String,
        documentId: String,
        data: Map<String, Any?>,
        merge: Boolean = true
    ): Boolean {
        return try {
            db.collection(collection).document(documentId)
                .set(data, com.google.firebase.firestore.SetOptions.merge())
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error setting document: $documentId", e)
            false
        }
    }

    /**
     * Delete document
     */
    suspend fun deleteDocument(collection: String, documentId: String): Boolean {
        return try {
            db.collection(collection).document(documentId).delete().await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting document: $documentId", e)
            false
        }
    }

    /**
     * Call Firebase Cloud Function
     */
    suspend fun callFunction(functionName: String, data: Map<String, Any>): Any? {
        return try {
            functions.getHttpsCallable(functionName)
                .call(data)
                .await()
                .data
        } catch (e: Exception) {
            Log.e(TAG, "Error calling function: $functionName", e)
            null
        }
    }

    /**
     * Sign out current user
     */
    fun signOut() {
        auth.signOut()
        Log.d(TAG, "User signed out")
    }
}
