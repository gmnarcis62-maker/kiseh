package com.example.ui.onboarding

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
import java.io.IOException

val Context.onboardingDataStore: DataStore<Preferences> by preferencesDataStore(name = "kiseh_onboarding_prefs")

/**
 * مدیریت ماندگار وضعیت نمایش معرفی اولیه برنامه (First Launch Onboarding) با استفاده از DataStore
 */
class OnboardingPreferencesManager(private val context: Context) {

    companion object {
        val KEY_HAS_SEEN_FIRST_LAUNCH = booleanPreferencesKey("has_seen_first_launch")
    }

    private val safePrefsFlow: Flow<Preferences> = context.onboardingDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }

    val hasSeenFirstLaunchFlow: Flow<Boolean> = safePrefsFlow.map { prefs ->
        prefs[KEY_HAS_SEEN_FIRST_LAUNCH] ?: false
    }

    suspend fun hasSeenFirstLaunch(): Boolean {
        return try {
            safePrefsFlow.first()[KEY_HAS_SEEN_FIRST_LAUNCH] ?: false
        } catch (_: Throwable) {
            false
        }
    }

    suspend fun setHasSeenFirstLaunch(hasSeen: Boolean = true) {
        try {
            context.onboardingDataStore.edit { prefs ->
                prefs[KEY_HAS_SEEN_FIRST_LAUNCH] = hasSeen
            }
        } catch (_: Throwable) {}
    }

    suspend fun resetFirstLaunchForTesting() {
        try {
            context.onboardingDataStore.edit { prefs ->
                prefs[KEY_HAS_SEEN_FIRST_LAUNCH] = false
            }
        } catch (_: Throwable) {}
    }
}
