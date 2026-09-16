package com.example.backup

import com.example.data.CustomCategory
import com.example.data.RecurringTransaction
import com.example.data.SavingsGoal
import com.example.data.Transaction
import com.squareup.moshi.JsonClass

/**
 * مدل ساختاریافته محتوای پشتیبان کامل برنامه کیسه
 * نکته حیاتی: اطلاعات امنیتی و رمز عبور (PIN) به هیچ عنوان در بکاپ ذخیره نمی‌شوند.
 */
@JsonClass(generateAdapter = true)
data class KisehBackupPayload(
    val backupVersion: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val appVersion: String = "1.6.0",
    val transactions: List<Transaction> = emptyList(),
    val savingsGoals: List<SavingsGoal> = emptyList(),
    val recurringTransactions: List<RecurringTransaction> = emptyList(),
    val customCategories: List<CustomCategory> = emptyList(),
    val learnedMerchants: Map<String, String> = emptyMap(),
    val appPreferences: AppPreferencesBackup = AppPreferencesBackup(),
    val checksum: String = ""
)

@JsonClass(generateAdapter = true)
data class AppPreferencesBackup(
    val budgetLimit: Long = 5000000L,
    val isCurrencyInRial: Boolean = false,
    val selectedPeriod: String = "ماهانه",
    val isVip: Boolean = false
)

/**
 * متادیتای نسخه پشتیبان موجود در کلود یا دیسک
 */
@JsonClass(generateAdapter = true)
data class BackupMetadata(
    val fileId: String = "",
    val fileName: String = "kiseh_backup.enc",
    val timestamp: Long = 0L,
    val sizeBytes: Long = 0L,
    val transactionCount: Int = 0,
    val goalsCount: Int = 0,
    val appVersion: String = "1.6.0"
)

/**
 * وضعیت‌های عملیات بکاپ و بازیابی ابری در رابط کاربری
 */
sealed class CloudBackupState {
    data object Idle : CloudBackupState()
    data class CheckingBackups(val message: String = "در حال بررسی نسخه‌های پشتیبان...") : CloudBackupState()
    data class BackupsListReady(val backups: List<BackupMetadata>) : CloudBackupState()
    data class InProgress(val message: String, val progress: Float? = null) : CloudBackupState()
    data class Success(val message: String, val metadata: BackupMetadata? = null) : CloudBackupState()
    data class Error(val errorMessage: String) : CloudBackupState()
}

/**
 * وضعیت حساب ابری گوگل
 */
sealed class GoogleAccountState {
    data object Disconnected : GoogleAccountState()
    data object Connecting : GoogleAccountState()
    data class Connected(
        val email: String,
        val displayName: String? = null,
        val photoUrl: String? = null
    ) : GoogleAccountState()
}
