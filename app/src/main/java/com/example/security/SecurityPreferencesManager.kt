package com.example.security

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.security.MessageDigest
import java.security.SecureRandom

val Context.securityDataStore: DataStore<Preferences> by preferencesDataStore(name = "kiseh_security_prefs")

/**
 * مدیریت امن ترجیحات امنیتی و رمز عبور هش‌شده با DataStore
 */
class SecurityPreferencesManager(private val context: Context) {

    companion object {
        private val KEY_APP_LOCK_ENABLED = booleanPreferencesKey("key_app_lock_enabled")
        private val KEY_LOCK_ON_LAUNCH = booleanPreferencesKey("key_lock_on_launch")
        private val KEY_PASSWORD_HASH = stringPreferencesKey("key_password_hash")
        private val KEY_PASSWORD_SALT = stringPreferencesKey("key_password_salt")
        private val KEY_PIN_LENGTH = intPreferencesKey("key_pin_length")

        fun generateSalt(): String {
            val random = SecureRandom()
            val saltBytes = ByteArray(16)
            random.nextBytes(saltBytes)
            return saltBytes.joinToString("") { "%02x".format(it) }
        }

        fun hashPinWithSalt(pin: String, salt: String): String {
            val input = "$salt:$pin"
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
            return hashBytes.joinToString("") { "%02x".format(it) }
        }
    }

    private val safeData: Flow<Preferences> = context.securityDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }

    val isAppLockEnabled: Flow<Boolean> = safeData.map { prefs ->
        prefs[KEY_APP_LOCK_ENABLED] ?: false
    }

    val isLockOnLaunchEnabled: Flow<Boolean> = safeData.map { prefs ->
        prefs[KEY_LOCK_ON_LAUNCH] ?: true
    }

    val pinLength: Flow<Int> = safeData.map { prefs ->
        prefs[KEY_PIN_LENGTH] ?: 4
    }

    val hasPinSet: Flow<Boolean> = safeData.map { prefs ->
        val hash = prefs[KEY_PASSWORD_HASH]
        !hash.isNullOrBlank()
    }

    suspend fun savePin(pin: String) {
        val salt = generateSalt()
        val hash = hashPinWithSalt(pin, salt)
        val length = pin.length

        context.securityDataStore.edit { prefs ->
            prefs[KEY_PASSWORD_HASH] = hash
            prefs[KEY_PASSWORD_SALT] = salt
            prefs[KEY_PIN_LENGTH] = length
            prefs[KEY_APP_LOCK_ENABLED] = true
        }
    }

    suspend fun verifyPin(enteredPin: String): Boolean {
        var isValid = false
        context.securityDataStore.edit { prefs ->
            val storedHash = prefs[KEY_PASSWORD_HASH]
            val storedSalt = prefs[KEY_PASSWORD_SALT]
            if (storedHash != null && storedSalt != null) {
                val computedHash = hashPinWithSalt(enteredPin, storedSalt)
                isValid = (computedHash == storedHash)
            }
        }
        return isValid
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        context.securityDataStore.edit { prefs ->
            prefs[KEY_APP_LOCK_ENABLED] = enabled
        }
    }

    suspend fun setLockOnLaunchEnabled(enabled: Boolean) {
        context.securityDataStore.edit { prefs ->
            prefs[KEY_LOCK_ON_LAUNCH] = enabled
        }
    }

    suspend fun clearSecurityData() {
        context.securityDataStore.edit { prefs ->
            prefs[KEY_APP_LOCK_ENABLED] = false
            prefs.remove(KEY_PASSWORD_HASH)
            prefs.remove(KEY_PASSWORD_SALT)
        }
    }
}