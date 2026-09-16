package com.example.billing

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * سیستم مرکزی مدیریت و پایش محدودیت‌های نسخه رایگان (Smart Free Trial Model).
 * بدون نیاز به تغییر در اسکیمای پایگاه داده یا Migration.
 */
object FeatureUsageManager {

    private const val TAG = "FeatureUsageManager"
    private const val PREFS_NAME = "kiseh_feature_usage_prefs"

    // سهمیه‌های نسخه رایگان (Free) ارجاع داده شده به مرجع مرکزی FreePlanLimits
    const val MAX_FREE_VOICE_DAILY = FreePlanLimits.VOICE_DAILY_LIMIT
    const val MAX_FREE_BANK_SMS_TRIAL = FreePlanLimits.BANK_SMS_TRIAL_LIMIT
    const val MAX_FREE_INVOICE_SCAN_MONTHLY = FreePlanLimits.INVOICE_SCAN_LIMIT
    const val MAX_FREE_SAVINGS_GOALS = FreePlanLimits.MAX_SAVINGS_GOALS
    const val MAX_FREE_RECURRING_TRANSACTIONS = FreePlanLimits.MAX_RECURRING_TRANSACTIONS
    const val MAX_FREE_CUSTOM_CATEGORIES = FreePlanLimits.MAX_CUSTOM_CATEGORIES

    // کلیدهای SharedPreferences
    private const val KEY_VOICE_DATE = "usage_voice_date"
    private const val KEY_VOICE_COUNT = "usage_voice_count"

    private const val KEY_BANK_SMS_COUNT = "usage_bank_sms_count"

    private const val KEY_INVOICE_MONTH = "usage_invoice_month"
    private const val KEY_INVOICE_COUNT = "usage_invoice_count"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val monthFormat = SimpleDateFormat("yyyy-MM", Locale.US)

    // ==========================================
    // ۱. ثبت گفتاری (Voice Input) - ریست روزانه
    // ==========================================

    private fun checkAndResetDailyVoice(context: Context) {
        try {
            val prefs = getPrefs(context)
            val today = dateFormat.format(Date())
            val lastDate = prefs.getString(KEY_VOICE_DATE, "") ?: ""
            if (lastDate.isEmpty() || lastDate != today) {
                prefs.edit()
                    .putString(KEY_VOICE_DATE, today)
                    .putInt(KEY_VOICE_COUNT, 0)
                    .apply()
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Error resetting daily voice quota", t)
        }
    }

    fun getVoiceUsageCount(context: Context): Int {
        checkAndResetDailyVoice(context)
        return try {
            getPrefs(context).getInt(KEY_VOICE_COUNT, 0)
        } catch (_: Throwable) {
            0
        }
    }

    fun getRemainingVoiceTrial(context: Context): Int {
        if (BillingManager.isVipUser(context)) return Int.MAX_VALUE
        val count = getVoiceUsageCount(context)
        return (MAX_FREE_VOICE_DAILY - count).coerceAtLeast(0)
    }

    fun canUseVoiceInput(context: Context): Boolean {
        if (BillingManager.isVipUser(context)) return true
        return getVoiceUsageCount(context) < MAX_FREE_VOICE_DAILY
    }

    fun incrementVoiceUsage(context: Context) {
        checkAndResetDailyVoice(context)
        try {
            val prefs = getPrefs(context)
            val current = prefs.getInt(KEY_VOICE_COUNT, 0)
            prefs.edit().putInt(KEY_VOICE_COUNT, current + 1).apply()
            // همچنین برای حفظ سازگاری در BillingManager نیز ثبت می‌شود
            BillingManager.incrementVoiceUsage(context)
        } catch (t: Throwable) {
            Log.e(TAG, "Error incrementing voice usage", t)
        }
    }

    // ==========================================
    // ۲. هوش پیامکی بانکی (Bank SMS Intelligence)
    // ==========================================

    fun getBankSmsUsageCount(context: Context): Int {
        return try {
            getPrefs(context).getInt(KEY_BANK_SMS_COUNT, 0)
        } catch (_: Throwable) {
            0
        }
    }

    fun getRemainingBankSmsTrial(context: Context): Int {
        if (BillingManager.isVipUser(context)) return Int.MAX_VALUE
        val count = getBankSmsUsageCount(context)
        return (MAX_FREE_BANK_SMS_TRIAL - count).coerceAtLeast(0)
    }

    fun canProcessBankSms(context: Context): Boolean {
        if (BillingManager.isVipUser(context)) return true
        return getBankSmsUsageCount(context) < MAX_FREE_BANK_SMS_TRIAL
    }

    fun incrementBankSmsUsage(context: Context) {
        if (BillingManager.isVipUser(context)) return
        try {
            val prefs = getPrefs(context)
            val current = prefs.getInt(KEY_BANK_SMS_COUNT, 0)
            prefs.edit().putInt(KEY_BANK_SMS_COUNT, current + 1).apply()
        } catch (t: Throwable) {
            Log.e(TAG, "Error incrementing bank sms usage", t)
        }
    }

    // ==========================================
    // ۳. اسکن و OCR فاکتور (Invoice Scanner) - ریست ماهانه
    // ==========================================

    private fun checkAndResetMonthlyInvoice(context: Context) {
        try {
            val prefs = getPrefs(context)
            val currentMonth = monthFormat.format(Date())
            val lastMonth = prefs.getString(KEY_INVOICE_MONTH, "") ?: ""
            if (lastMonth.isEmpty() || lastMonth != currentMonth) {
                prefs.edit()
                    .putString(KEY_INVOICE_MONTH, currentMonth)
                    .putInt(KEY_INVOICE_COUNT, 0)
                    .apply()
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Error resetting monthly invoice quota", t)
        }
    }

    fun getInvoiceScanUsageCount(context: Context): Int {
        checkAndResetMonthlyInvoice(context)
        return try {
            getPrefs(context).getInt(KEY_INVOICE_COUNT, 0)
        } catch (_: Throwable) {
            0
        }
    }

    fun getRemainingInvoiceScans(context: Context): Int {
        if (BillingManager.isVipUser(context)) return Int.MAX_VALUE
        val count = getInvoiceScanUsageCount(context)
        return (MAX_FREE_INVOICE_SCAN_MONTHLY - count).coerceAtLeast(0)
    }

    fun canScanInvoice(context: Context): Boolean {
        if (BillingManager.isVipUser(context)) return true
        return getInvoiceScanUsageCount(context) < MAX_FREE_INVOICE_SCAN_MONTHLY
    }

    fun incrementInvoiceScanUsage(context: Context) {
        checkAndResetMonthlyInvoice(context)
        try {
            val prefs = getPrefs(context)
            val current = prefs.getInt(KEY_INVOICE_COUNT, 0)
            prefs.edit().putInt(KEY_INVOICE_COUNT, current + 1).apply()
        } catch (t: Throwable) {
            Log.e(TAG, "Error incrementing invoice scan usage", t)
        }
    }

    // ==========================================
    // ۴. اهداف پس‌انداز (Savings Goals)
    // ==========================================

    fun canAddSavingsGoal(context: Context, currentActiveGoalsCount: Int = 0): Boolean {
        if (BillingManager.isVipUser(context)) return true
        return currentActiveGoalsCount < MAX_FREE_SAVINGS_GOALS
    }

    // ==========================================
    // ۵. تراکنش‌های دوره‌ای (Recurring Transactions)
    // ==========================================

    fun canAddRecurringTransaction(context: Context, currentActiveCount: Int = 0): Boolean {
        if (BillingManager.isVipUser(context)) return true
        return currentActiveCount < MAX_FREE_RECURRING_TRANSACTIONS
    }

    // ==========================================
    // ۶. دسته‌بندی‌های اختصاصی (Custom Categories)
    // ==========================================

    fun canAddCustomCategory(context: Context, currentCustomCount: Int): Boolean {
        if (BillingManager.isVipUser(context)) return true
        return currentCustomCount < MAX_FREE_CUSTOM_CATEGORIES
    }

    fun getInvoiceScanRemaining(context: Context): Int = getRemainingInvoiceScans(context)

    // ==========================================
    // متدهای کمکی جهت تست و ریست دستی (Testing Helpers)
    // ==========================================

    fun resetAllForTesting(context: Context) {
        try {
            getPrefs(context).edit().clear().apply()
            BillingManager.resetVoiceUsageForTesting(context, count = 0)
        } catch (t: Throwable) {
            Log.e(TAG, "Error resetting usage for testing", t)
        }
    }

    fun setVoiceUsageForTesting(context: Context, count: Int, date: String? = null) {
        try {
            val d = date ?: dateFormat.format(Date())
            getPrefs(context).edit()
                .putString(KEY_VOICE_DATE, d)
                .putInt(KEY_VOICE_COUNT, count)
                .apply()
            BillingManager.resetVoiceUsageForTesting(context, count, d)
        } catch (t: Throwable) {
            Log.e(TAG, "Error setting voice usage for testing", t)
        }
    }

    fun setBankSmsUsageForTesting(context: Context, count: Int) {
        try {
            getPrefs(context).edit()
                .putInt(KEY_BANK_SMS_COUNT, count)
                .apply()
        } catch (t: Throwable) {
            Log.e(TAG, "Error setting bank sms usage for testing", t)
        }
    }

    fun setInvoiceUsageForTesting(context: Context, count: Int, month: String? = null) {
        try {
            val m = month ?: monthFormat.format(Date())
            getPrefs(context).edit()
                .putString(KEY_INVOICE_MONTH, m)
                .putInt(KEY_INVOICE_COUNT, count)
                .apply()
        } catch (t: Throwable) {
            Log.e(TAG, "Error setting invoice usage for testing", t)
        }
    }
}
