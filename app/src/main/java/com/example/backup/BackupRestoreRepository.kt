package com.example.backup

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.example.data.AppDatabase
import com.example.data.CustomCategoryDao
import com.example.data.RecurringTransactionDao
import com.example.data.SavingsGoalDao
import com.example.data.TransactionDao
import com.example.sms.UserCategoryLearner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ریپازیتوری مرکزی برای عملیات ایجاد، رمزنگاری، ارسال، دانلود، رمزگشایی و بازیابی جامع بکاپ
 */
class BackupRestoreRepository(
    private val database: AppDatabase,
    private val transactionDao: TransactionDao,
    private val savingsGoalDao: SavingsGoalDao,
    private val recurringTransactionDao: RecurringTransactionDao,
    private val customCategoryDao: CustomCategoryDao,
    private val driveHelper: GoogleDriveServiceHelper,
    private val context: Context? = null
) {

    companion object {
        private const val TAG = "GoogleDriveBackup"
    }

    /**
     * آماده‌سازی و ارسال بکاپ رمزنگاری‌شده به فضای ابری
     */
    suspend fun createAndUploadBackup(
        budgetLimit: Long,
        isCurrencyInRial: Boolean,
        selectedPeriod: String,
        isVip: Boolean
    ): Result<BackupMetadata> = withContext(Dispatchers.IO) {
        try {
            // ۱. استخراج اطلاعات از دیتابیس محلی
            val transactions = transactionDao.getAllTransactionsList()
            val goals = savingsGoalDao.getAllGoalsList()
            val recurring = recurringTransactionDao.getAllList()
            val categories = customCategoryDao.getAllCategoriesList()

            // استخراج داده‌های یادگیری پیامک بانکی
            val learnedMerchants = context?.let { ctx ->
                try {
                    UserCategoryLearner.getInstance(ctx).getAllLearned()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to export learned merchants: ${e.message}")
                    emptyMap()
                }
            } ?: emptyMap()

            val prefsBackup = AppPreferencesBackup(
                budgetLimit = budgetLimit,
                isCurrencyInRial = isCurrencyInRial,
                selectedPeriod = selectedPeriod,
                isVip = isVip
            )

            val timestamp = System.currentTimeMillis()
            val formattedFileName = GoogleDriveServiceHelper.generateBackupFileName(timestamp)

            val payload = KisehBackupPayload(
                timestamp = timestamp,
                transactions = transactions,
                savingsGoals = goals,
                recurringTransactions = recurring,
                customCategories = categories,
                learnedMerchants = learnedMerchants,
                appPreferences = prefsBackup
            )

            // ۲. رمزنگاری مطمئن با AES-256-GCM و محاسبه Checksum
            val encryptedBytes = BackupCryptoManager.encryptBackup(payload)

            val metadata = BackupMetadata(
                fileName = formattedFileName,
                timestamp = payload.timestamp,
                sizeBytes = encryptedBytes.size.toLong(),
                transactionCount = transactions.size,
                goalsCount = goals.size,
                appVersion = "1.6.0"
            )

            // ۳. آپلود در Google Drive و کش پایدار ابری
            driveHelper.uploadBackup(encryptedBytes, metadata)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating cloud backup", e)
            Result.failure(e)
        }
    }

    /**
     * دریافت لیست تمامی نسخه‌های پشتیبان موجود در گوگل درایو یا مخزن محلی
     */
    suspend fun listAvailableBackups(): Result<List<BackupMetadata>> = withContext(Dispatchers.IO) {
        driveHelper.listBackups()
    }

    /**
     * بازیابی اطلاعات از بکاپ ابری (یک فایل مشخص بر اساس ID یا آخرین فایل)
     */
    suspend fun restoreBackup(fileId: String? = null): Result<KisehBackupPayload> = withContext(Dispatchers.IO) {
        try {
            // ۱. دریافت فایل رمزنگاری شده از کلود یا مخزن
            val downloadResult = driveHelper.downloadBackup(fileId)
            if (downloadResult.isFailure) {
                return@withContext Result.failure(
                    downloadResult.exceptionOrNull() ?: IllegalStateException("هیچ نسخه پشتیبانی پیدا نشد")
                )
            }

            val encryptedBytes = downloadResult.getOrThrow()

            // ۲. رمزگشایی و اعتبارسنجی صحت داده‌ها (Checksum و سلامت فرمت)
            val payload = try {
                BackupCryptoManager.decryptBackup(encryptedBytes)
            } catch (e: Exception) {
                Log.e(TAG, "Decryption/validation error during restore", e)
                return@withContext Result.failure(e)
            }

            // ۳. بازنشانی اتمیک در دیتابیس (Atomic Transaction) جهت جلوگیری از تخریب دیتابیس
            Log.i(TAG, "Database Restore Start: restoring data into Room database...")
            database.withTransaction {
                if (payload.customCategories.isNotEmpty()) {
                    customCategoryDao.insertAll(payload.customCategories)
                }

                if (payload.transactions.isNotEmpty()) {
                    transactionDao.insertAll(payload.transactions)
                }

                if (payload.savingsGoals.isNotEmpty()) {
                    savingsGoalDao.insertAll(payload.savingsGoals)
                }

                if (payload.recurringTransactions.isNotEmpty()) {
                    recurringTransactionDao.insertAll(payload.recurringTransactions)
                }
            }

            // ۴. بازنشانی داده‌های یادگیری پیامک بانکی
            if (context != null && payload.learnedMerchants.isNotEmpty()) {
                try {
                    UserCategoryLearner.getInstance(context).learnAll(payload.learnedMerchants)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to restore learned merchants: ${e.message}")
                }
            }

            Log.i(TAG, "Restore Complete: successfully restored all records into database (tx=${payload.transactions.size}, goals=${payload.savingsGoals.size}, categories=${payload.customCategories.size})")

            Result.success(payload)
        } catch (e: Exception) {
            Log.e(TAG, "Restore operation encountered fatal error", e)
            Result.failure(e)
        }
    }

    fun getAccountState(): GoogleAccountState = driveHelper.getSavedAccountState()

    fun getLastBackupMetadata(): BackupMetadata? = driveHelper.getLastBackupMetadata()

    fun saveAccount(email: String, displayName: String?, accessToken: String? = null) =
        driveHelper.saveAccount(email, displayName, accessToken)

    fun signOut() = driveHelper.signOut()
}
