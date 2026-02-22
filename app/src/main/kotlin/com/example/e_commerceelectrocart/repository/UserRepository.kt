package com.example.e_commerceelectrocart.repository

import android.util.Log
import com.example.e_commerceelectrocart.firebase.FirebaseManager
import com.example.e_commerceelectrocart.firestore.Address
import com.example.e_commerceelectrocart.firestore.AppNotification
import com.example.e_commerceelectrocart.firestore.UserProfile
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for User-related Firestore operations
 */
@Singleton
class UserRepository @Inject constructor(
    private val firebaseManager: FirebaseManager
) {
    companion object {
        private const val TAG = "UserRepository"
    }

    private val usersCollection = firebaseManager.db.collection(FirebaseManager.COLLECTION_USERS)
    private val addressesCollection = firebaseManager.db.collection(FirebaseManager.COLLECTION_ADDRESSES)
    private val notificationsCollection = firebaseManager.db.collection(FirebaseManager.COLLECTION_NOTIFICATIONS)

    /**
     * Get user profile
     */
    fun getUserProfile(userId: String): Flow<UserProfile?> = callbackFlow {
        val listener = usersCollection.document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error fetching user profile", error)
                    close(error)
                    return@addSnapshotListener
                }

                val user = snapshot?.toObject(UserProfile::class.java)
                trySend(user)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get user profile (suspend)
     */
    suspend fun getUserProfileSync(userId: String): UserProfile? {
        return try {
            val doc = usersCollection.document(userId).get().await()
            doc.toObject(UserProfile::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user profile", e)
            null
        }
    }

    /**
     * Update user profile
     */
    suspend fun updateUserProfile(userId: String, updates: Map<String, Any?>): Boolean {
        return try {
            usersCollection.document(userId)
                .update(updates + mapOf("updatedAt" to FieldValue.serverTimestamp()))
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user profile", e)
            false
        }
    }

    /**
     * Update user profile photo
     */
    suspend fun updateProfilePhoto(userId: String, photoUrl: String): Boolean {
        return try {
            usersCollection.document(userId)
                .update(mapOf(
                    "photoUrl" to photoUrl,
                    "updatedAt" to FieldValue.serverTimestamp()
                ))
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating profile photo", e)
            false
        }
    }

    /**
     * Add product to wishlist
     */
    suspend fun addToWishlist(userId: String, productId: String): Boolean {
        return try {
            usersCollection.document(userId)
                .update("wishlist", FieldValue.arrayUnion(productId))
                .await()
            firebaseManager.logEvent("wishlist_added", mapOf("product_id" to productId))
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error adding to wishlist", e)
            false
        }
    }

    /**
     * Remove product from wishlist
     */
    suspend fun removeFromWishlist(userId: String, productId: String): Boolean {
        return try {
            usersCollection.document(userId)
                .update("wishlist", FieldValue.arrayRemove(productId))
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error removing from wishlist", e)
            false
        }
    }

    /**
     * Get user's addresses
     */
    fun getUserAddresses(userId: String): Flow<List<Address>> = callbackFlow {
        val listener = addressesCollection
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error fetching addresses", error)
                    close(error)
                    return@addSnapshotListener
                }

                val addresses = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Address::class.java)
                } ?: emptyList()

                trySend(addresses)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Add new address
     */
    suspend fun addAddress(userId: String, address: Address): String? {
        return try {
            val addressWithUserId = address.copy(userId = userId)
            val docRef = addressesCollection.add(addressWithUserId).await()
            
            // If this is the first address, make it default
            val existingAddresses = addressesCollection.whereEqualTo("userId", userId).get().await()
            if (existingAddresses.isEmpty) {
                addressesCollection.document(docRef.id)
                    .update("isDefault", true)
                    .await()
            }
            
            docRef.id
        } catch (e: Exception) {
            Log.e(TAG, "Error adding address", e)
            null
        }
    }

    /**
     * Update address
     */
    suspend fun updateAddress(addressId: String, updates: Map<String, Any?>): Boolean {
        return try {
            addressesCollection.document(addressId).update(updates).await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating address", e)
            false
        }
    }

    /**
     * Delete address
     */
    suspend fun deleteAddress(addressId: String): Boolean {
        return try {
            addressesCollection.document(addressId).delete().await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting address", e)
            false
        }
    }

    /**
     * Set default address
     */
    suspend fun setDefaultAddress(userId: String, addressId: String): Boolean {
        return try {
            // Remove default from all user addresses
            val addresses = addressesCollection.whereEqualTo("userId", userId).get().await()
            for (doc in addresses.documents) {
                if (doc.getBoolean("isDefault") == true) {
                    doc.reference.update("isDefault", false).await()
                }
            }

            // Set new default
            addressesCollection.document(addressId)
                .update("isDefault", true)
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error setting default address", e)
            false
        }
    }

    /**
     * Get user's notifications
     */
    fun getUserNotifications(userId: String): Flow<List<AppNotification>> = callbackFlow {
        val listener = notificationsCollection
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error fetching notifications", error)
                    close(error)
                    return@addSnapshotListener
                }

                val notifications = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(AppNotification::class.java)
                } ?: emptyList()

                trySend(notifications)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Mark notification as read
     */
    suspend fun markNotificationAsRead(notificationId: String): Boolean {
        return try {
            notificationsCollection.document(notificationId)
                .update("isRead", true)
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error marking notification as read", e)
            false
        }
    }

    /**
     * Mark all notifications as read
     */
    suspend fun markAllNotificationsAsRead(userId: String): Boolean {
        return try {
            val notifications = notificationsCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("isRead", false)
                .get()
                .await()

            for (doc in notifications.documents) {
                doc.reference.update("isRead", true).await()
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error marking all notifications as read", e)
            false
        }
    }

    /**
     * Delete notification
     */
    suspend fun deleteNotification(notificationId: String): Boolean {
        return try {
            notificationsCollection.document(notificationId).delete().await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting notification", e)
            false
        }
    }

    /**
     * Get unread notification count
     */
    fun getUnreadNotificationCount(userId: String): Flow<Int> = callbackFlow {
        val listener = notificationsCollection
            .whereEqualTo("userId", userId)
            .whereEqualTo("isRead", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val count = snapshot?.size() ?: 0
                trySend(count)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get all users (for admin)
     */
    fun getAllUsers(): Flow<List<UserProfile>> = callbackFlow {
        val listener = usersCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error fetching all users", error)
                    close(error)
                    return@addSnapshotListener
                }

                val users = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(UserProfile::class.java)
                } ?: emptyList()

                trySend(users)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Update user role (for admin)
     */
    suspend fun updateUserRole(userId: String, role: String): Boolean {
        return try {
            usersCollection.document(userId)
                .update(mapOf("role" to role))
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user role", e)
            false
        }
    }

    /**
     * Toggle user active status (for admin)
     */
    suspend fun toggleUserActive(userId: String, isActive: Boolean): Boolean {
        return try {
            usersCollection.document(userId)
                .update(mapOf("isActive" to isActive))
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling user active status", e)
            false
        }
    }

    /**
     * Store FCM token for user
     */
    suspend fun storeFCMToken(userId: String, token: String): Boolean {
        return try {
            usersCollection.document(userId)
                .update("fcmTokens", FieldValue.arrayUnion(token))
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error storing FCM token", e)
            false
        }
    }
}
