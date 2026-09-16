package com.example.ui.onboarding

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val Context.onboardingDataStore: DataStore<Preferences> by preferencesDataStore(name = "kiseh_onboarding_prefs")

/**
 * مدیریت ماندگار وضعیت نمایش معرفی اولیه برنامه (First Launch Onboarding) با استفاده از DataStore
 */
class OnboardingPreferencesManager(private val context: Context) {

    companion object {
        val KEY_HAS_SEEN_FIRST_LAUNCH = booleanPreferencesKey("has_seen_first_launch")
        val KEY_LAST_REMINDER_MONTH = stringPreferencesKey("last_reminder_month")
        
        private val monthFormat = SimpleDateFormat("yyyy-MM", Locale.US)
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

    /**
     * بررسی می‌کند که آیا کاربر قبلاً یادآوری نسخه رایگان را در ماه جاری دیده است یا خیر.
     * این متد به صورت خودکار با تغییر ماه، مقدار را ریست می‌کند تا از اسپم جلوگیری شود.
     */
    suspend fun hasSeenFreePlanReminderThisMonth(): Boolean {
        return try {
            val prefs = safePrefsFlow.first()
            val lastReminderMonth = prefs[KEY_LAST_REMINDER_MONTH] ?: ""
            val currentMonth = monthFormat.format(Date())
            
            // اگر ماه فعلی با ماه آخرین یادآوری متفاوت باشد، یعنی باید دوباره نمایش داده شود
            // و مقدار جدید ذخیره شود
            if (lastReminderMonth != currentMonth) {
                return false
            }
            true
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * ثبت زمان نمایش یادآوری نسخه رایگان برای ماه جاری
     */
    suspend fun markFreePlanReminderShown() {
        try {
            val currentMonth = monthFormat.format(Date())
            context.onboardingDataStore.edit { prefs ->
                prefs[KEY_LAST_REMINDER_MONTH] = currentMonth
            }
        } catch (_: Throwable) {}
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
