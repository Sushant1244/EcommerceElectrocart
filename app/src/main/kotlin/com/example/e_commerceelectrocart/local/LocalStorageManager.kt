package com.example.e_commerceelectrocart.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Extension property for DataStore
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "electrocart_prefs")

/**
 * Local storage manager using DataStore for caching user preferences
 * and providing offline support
 */
@Singleton
class LocalStorageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        // Preference Keys
        private val KEY_USER_ID = stringPreferencesKey("user_id")
        private val KEY_USER_EMAIL = stringPreferencesKey("user_email")
        private val KEY_USER_NAME = stringPreferencesKey("user_name")
        private val KEY_USER_ROLE = stringPreferencesKey("user_role")
        private val KEY_IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        private val KEY_IS_ADMIN = booleanPreferencesKey("is_admin")
        private val KEY_LAST_SYNC_TIME = stringPreferencesKey("last_sync_time")
        private val KEY_NOTIFICATION_ENABLED = booleanPreferencesKey("notification_enabled")
        private val KEY_DARK_MODE_ENABLED = booleanPreferencesKey("dark_mode_enabled")
        private val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val KEY_FCM_TOKEN = stringPreferencesKey("fcm_token")
        private val KEY_CART_ITEMS_COUNT = stringPreferencesKey("cart_items_count")
    }

    // User Session
    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_IS_LOGGED_IN] ?: false
    }

    val userId: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_USER_ID]
    }

    val userEmail: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_USER_EMAIL]
    }

    val userName: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_USER_NAME]
    }

    val userRole: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_USER_ROLE]
    }

    val isAdmin: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_IS_ADMIN] ?: false
    }

    // App Settings
    val isDarkModeEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_DARK_MODE_ENABLED] ?: false
    }

    val isNotificationEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_NOTIFICATION_ENABLED] ?: true
    }

    val isOnboardingCompleted: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_ONBOARDING_COMPLETED] ?: false
    }

    // Cart
    val cartItemsCount: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_CART_ITEMS_COUNT]?.toIntOrNull() ?: 0
    }

    // Last Sync
    val lastSyncTime: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_LAST_SYNC_TIME]
    }

    // Save user session
    suspend fun saveUserSession(
        userId: String,
        email: String,
        name: String,
        role: String,
        isAdmin: Boolean
    ) {
        context.dataStore.edit { prefs ->
            prefs[KEY_USER_ID] = userId
            prefs[KEY_USER_EMAIL] = email
            prefs[KEY_USER_NAME] = name
            prefs[KEY_USER_ROLE] = role
            prefs[KEY_IS_ADMIN] = isAdmin
            prefs[KEY_IS_LOGGED_IN] = true
        }
    }

    // Clear user session (logout)
    suspend fun clearUserSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_USER_ID)
            prefs.remove(KEY_USER_EMAIL)
            prefs.remove(KEY_USER_NAME)
            prefs.remove(KEY_USER_ROLE)
            prefs[KEY_IS_ADMIN] = false
            prefs[KEY_IS_LOGGED_IN] = false
        }
    }

    // Save app settings
    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DARK_MODE_ENABLED] = enabled
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_NOTIFICATION_ENABLED] = enabled
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ONBOARDING_COMPLETED] = completed
        }
    }

    // Save FCM token
    suspend fun saveFCMToken(token: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_FCM_TOKEN] = token
        }
    }

    // Get FCM token
    suspend fun getFCMToken(): String? {
        return context.dataStore.data.first()[KEY_FCM_TOKEN]
    }

    // Save cart count
    suspend fun saveCartCount(count: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CART_ITEMS_COUNT] = count.toString()
        }
    }

    // Update last sync time
    suspend fun updateLastSyncTime() {
        context.dataStore.edit { prefs ->
            prefs[KEY_LAST_SYNC_TIME] = System.currentTimeMillis().toString()
        }
    }

    // Check if data needs refresh
    suspend fun needsRefresh(): Boolean {
        val lastSync = context.dataStore.data.first()[KEY_LAST_SYNC_TIME]?.toLongOrNull()
        if (lastSync == null) return true

        // Refresh if last sync was more than 1 hour ago
        val oneHourAgo = System.currentTimeMillis() - (60 * 60 * 1000)
        return lastSync < oneHourAgo
    }

    // Clear all preferences
    suspend fun clearAll() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
