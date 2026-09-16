package com.example.sms

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.io.IOException

val Context.bankSmsDataStore: DataStore<Preferences> by preferencesDataStore(name = "kiseh_bank_sms_prefs")

/**
 * مدیریت ماندگار تنظیمات هوش پیامک بانکی با Jetpack DataStore
 */
class BankSmsPreferencesManager(private val context: Context) {

    companion object {
        private val KEY_BANK_SMS_ENABLED = booleanPreferencesKey("key_bank_sms_enabled")
        private val KEY_NOTIFICATION_ENABLED = booleanPreferencesKey("key_notification_enabled")
        private val KEY_QUICK_ACTION_ENABLED = booleanPreferencesKey("key_quick_action_enabled")
        private val KEY_SMART_CATEGORY_ENABLED = booleanPreferencesKey("key_smart_category_enabled")
        private val KEY_USER_LEARNING_ENABLED = booleanPreferencesKey("key_user_learning_enabled")
        private val KEY_AUTO_CLEANUP_ENABLED = booleanPreferencesKey("key_auto_cleanup_enabled")
        private val KEY_AUTO_CONFIRM_ENABLED = booleanPreferencesKey("key_auto_confirm_enabled")

        @Volatile
        private var INSTANCE: BankSmsPreferencesManager? = null

        fun getInstance(context: Context): BankSmsPreferencesManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BankSmsPreferencesManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val safePrefsFlow: Flow<Preferences> = context.bankSmsDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }

    // --- Flows ---

    val isBankSmsEnabledFlow: Flow<Boolean> = safePrefsFlow.map { prefs ->
        prefs[KEY_BANK_SMS_ENABLED] ?: true
    }

    val isNotificationEnabledFlow: Flow<Boolean> = safePrefsFlow.map { prefs ->
        prefs[KEY_NOTIFICATION_ENABLED] ?: true
    }

    val isQuickActionEnabledFlow: Flow<Boolean> = safePrefsFlow.map { prefs ->
        prefs[KEY_QUICK_ACTION_ENABLED] ?: true
    }

    val isSmartCategoryEnabledFlow: Flow<Boolean> = safePrefsFlow.map { prefs ->
        prefs[KEY_SMART_CATEGORY_ENABLED] ?: true
    }

    val isUserLearningEnabledFlow: Flow<Boolean> = safePrefsFlow.map { prefs ->
        prefs[KEY_USER_LEARNING_ENABLED] ?: true
    }

    val isAutoCleanupEnabledFlow: Flow<Boolean> = safePrefsFlow.map { prefs ->
        prefs[KEY_AUTO_CLEANUP_ENABLED] ?: false
    }

    val isAutoConfirmEnabledFlow: Flow<Boolean> = safePrefsFlow.map { prefs ->
        prefs[KEY_AUTO_CONFIRM_ENABLED] ?: false
    }

    // --- Direct / Blocking Methods (for BroadcastReceivers) ---

    fun isBankSmsEnabled(): Boolean = runBlocking {
        try {
            isBankSmsEnabledFlow.first()
        } catch (e: Exception) {
            true
        }
    }

    fun isNotificationEnabled(): Boolean = runBlocking {
        try {
            isNotificationEnabledFlow.first()
        } catch (e: Exception) {
            true
        }
    }

    fun isQuickActionEnabled(): Boolean = runBlocking {
        try {
            isQuickActionEnabledFlow.first()
        } catch (e: Exception) {
            true
        }
    }

    fun isSmartCategoryEnabled(): Boolean = runBlocking {
        try {
            isSmartCategoryEnabledFlow.first()
        } catch (e: Exception) {
            true
        }
    }

    fun isUserLearningEnabled(): Boolean = runBlocking {
        try {
            isUserLearningEnabledFlow.first()
        } catch (e: Exception) {
            true
        }
    }

    fun isAutoCleanupEnabled(): Boolean = runBlocking {
        try {
            isAutoCleanupEnabledFlow.first()
        } catch (e: Exception) {
            false
        }
    }

    fun isAutoConfirmEnabled(): Boolean = runBlocking {
        try {
            isAutoConfirmEnabledFlow.first()
        } catch (e: Exception) {
            false
        }
    }

    // --- Suspending Setters ---

    suspend fun setBankSmsEnabled(enabled: Boolean) {
        context.bankSmsDataStore.edit { prefs ->
            prefs[KEY_BANK_SMS_ENABLED] = enabled
        }
    }

    suspend fun setNotificationEnabled(enabled: Boolean) {
        context.bankSmsDataStore.edit { prefs ->
            prefs[KEY_NOTIFICATION_ENABLED] = enabled
        }
    }

    suspend fun setQuickActionEnabled(enabled: Boolean) {
        context.bankSmsDataStore.edit { prefs ->
            prefs[KEY_QUICK_ACTION_ENABLED] = enabled
        }
    }

    suspend fun setSmartCategoryEnabled(enabled: Boolean) {
        context.bankSmsDataStore.edit { prefs ->
            prefs[KEY_SMART_CATEGORY_ENABLED] = enabled
        }
    }

    suspend fun setUserLearningEnabled(enabled: Boolean) {
        context.bankSmsDataStore.edit { prefs ->
            prefs[KEY_USER_LEARNING_ENABLED] = enabled
        }
    }

    suspend fun setAutoCleanupEnabled(enabled: Boolean) {
        context.bankSmsDataStore.edit { prefs ->
            prefs[KEY_AUTO_CLEANUP_ENABLED] = enabled
        }
    }

    suspend fun setAutoConfirmEnabled(enabled: Boolean) {
        context.bankSmsDataStore.edit { prefs ->
            prefs[KEY_AUTO_CONFIRM_ENABLED] = enabled
        }
    }

    suspend fun resetToDefaults() {
        setBankSmsEnabled(true)
        setNotificationEnabled(true)
        setQuickActionEnabled(true)
        setSmartCategoryEnabled(true)
        setUserLearningEnabled(true)
        setAutoCleanupEnabled(false)
        setAutoConfirmEnabled(false)
    }
}
