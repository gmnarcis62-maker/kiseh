package com.example.backup

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.room.withTransaction
import com.example.data.AppDatabase
import com.example.data.CustomCategoryDao
import com.example.data.RecurringTransactionDao
import com.example.data.SavingsGoalDao
import com.example.data.TransactionDao
import com.example.sms.UserCategoryLearner
import com.example.ui.util.PersianUtils
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * ریپازیتوری بکاپ ابری با Firebase Firestore (بدون نیاز به Storage/Blaze):
 * - فایل رمزنگاری‌شده به صورت Base64 در Firestore ذخیره می‌شود
 * - هر کاربر فقط به بکاپ‌های خودش دسترسی دارد (طبق قوانین امنیتی)
 * - محدودیت حجم هر سند Firestore: ۱ مگابایت
 */
class FirebaseBackupRepository(
    private val database: AppDatabase,
    private val transactionDao: TransactionDao,
    private val savingsGoalDao: SavingsGoalDao,
    private val recurringTransactionDao: RecurringTransactionDao,
    private val customCategoryDao: CustomCategoryDao,
    private val authManager: FirebaseAuthManager,
    private val context: Context? = null
) {

    companion object {
        private const val TAG = "FirebaseBackup"
        private const val COLLECTION_USERS = "users"
        private const val COLLECTION_BACKUPS = "backups"

        /** حداکثر حجم فایل رمزنگاری‌شده قبل از Base64 (برای اطمینان از جای گرفتن در Firestore) */
        private const val MAX_ENCRYPTED_BYTES = 650_000L
    }

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    /**
     * تولید نام فایل بکاپ با تاریخ شمسی + timestamp
     */
    private fun generateBackupFileName(timestamp: Long): String {
        val parts = PersianUtils.getShamsiDateParts(timestamp)
        val y = parts[0]
        val m = parts[1].toString().padStart(2, '0')
        val d = parts[2].toString().padStart(2, '0')
        return "Backup_${y}_${m}_${d}_${timestamp}.hpb"
    }

    /**
     * آماده‌سازی، رمزنگاری و آپلود بکاپ به Firestore
     */
    suspend fun createAndUploadBackup(
        budgetLimit: Long,
        isCurrencyInRial: Boolean,
        selectedPeriod: String,
        isVip: Boolean
    ): Result<BackupMetadata> = withContext(Dispatchers.IO) {
        try {
            val userId = authManager.getUserId()
                ?: return@withContext Result.failure(
                    Exception("لطفاً ابتدا وارد حساب کاربری خود شوید")
                )

            Log.i(TAG, "Backup Start: extracting data from local database...")

            // ۱. استخراج داده‌ها
            val transactions = transactionDao.getAllTransactionsList()
            val goals = savingsGoalDao.getAllGoalsList()
            val recurring = recurringTransactionDao.getAllList()
            val categories = customCategoryDao.getAllCategoriesList()

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
            val fileName = generateBackupFileName(timestamp)

            val payload = KisehBackupPayload(
                timestamp = timestamp,
                transactions = transactions,
                savingsGoals = goals,
                recurringTransactions = recurring,
                customCategories = categories,
                learnedMerchants = learnedMerchants,
                appPreferences = prefsBackup
            )

            // ۲. رمزنگاری AES-256-GCM
            val encryptedBytes = BackupCryptoManager.encryptBackup(payload)
            Log.i(TAG, "Encryption done: ${encryptedBytes.size} bytes")

            // ۳. بررسی حجم قبل از ذخیره در Firestore
            if (encryptedBytes.size > MAX_ENCRYPTED_BYTES) {
                val sizeKb = encryptedBytes.size / 1024
                val maxKb = MAX_ENCRYPTED_BYTES / 1024
                return@withContext Result.failure(
                    Exception(
                        "حجم بکاپ ($sizeKb کیلوبایت) بیش از حد مجاز ($maxKb کیلوبایت) است.\n" +
                                "لطفاً تراکنش‌های قدیمی را پاک کنید یا از بکاپ لوکال استفاده کنید."
                    )
                )
            }

            // ۴. تبدیل به Base64 برای ذخیره در Firestore
            val encryptedBase64 = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)

            // ۵. ذخیره در Firestore
            val backupDoc = hashMapOf(
                "fileName" to fileName,
                "timestamp" to timestamp,
                "sizeBytes" to encryptedBytes.size.toLong(),
                "transactionCount" to transactions.size,
                "goalsCount" to goals.size,
                "appVersion" to "1.6.0",
                "encryptedData" to encryptedBase64
            )

            firestore.collection(COLLECTION_USERS).document(userId)
                .collection(COLLECTION_BACKUPS).document(fileName)
                .set(backupDoc).await()

            Log.i(TAG, "Backup Success: saved to Firestore with ${encryptedBase64.length} chars")

            val metadata = BackupMetadata(
                fileId = fileName,
                fileName = fileName,
                timestamp = timestamp,
                sizeBytes = encryptedBytes.size.toLong(),
                transactionCount = transactions.size,
                goalsCount = goals.size,
                appVersion = "1.6.0"
            )

            Result.success(metadata)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating cloud backup", e)
            Result.failure(Exception("خطا در بکاپ‌گیری ابری: ${e.message}"))
        }
    }

    /**
     * دریافت لیست بکاپ‌های موجود در Firestore
     */
    suspend fun listAvailableBackups(): Result<List<BackupMetadata>> = withContext(Dispatchers.IO) {
        try {
            val userId = authManager.getUserId()
                ?: return@withContext Result.failure(
                    Exception("لطفاً ابتدا وارد حساب کاربری خود شوید")
                )

            val snapshot = firestore.collection(COLLECTION_USERS).document(userId)
                .collection(COLLECTION_BACKUPS)
                .get()
                .await()

            val backups = snapshot.documents.mapNotNull { doc ->
                val fileName = doc.getString("fileName") ?: doc.id
                BackupMetadata(
                    fileId = fileName,
                    fileName = fileName,
                    timestamp = doc.getLong("timestamp") ?: 0L,
                    sizeBytes = doc.getLong("sizeBytes") ?: 0L,
                    transactionCount = doc.getLong("transactionCount")?.toInt() ?: 0,
                    goalsCount = doc.getLong("goalsCount")?.toInt() ?: 0,
                    appVersion = doc.getString("appVersion") ?: "1.6.0"
                )
            }.sortedByDescending { it.timestamp }

            if (backups.isEmpty()) {
                Result.failure(Exception("هیچ نسخه پشتیبانی در حساب شما یافت نشد"))
            } else {
                Log.i(TAG, "List Backups: found ${backups.size} backup(s)")
                Result.success(backups)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error listing backups", e)
            Result.failure(Exception("خطا در دریافت لیست بکاپ‌ها: ${e.message}"))
        }
    }

    /**
     * دانلود، رمزگشایی و بازیابی بکاپ مشخص (یا آخرین بکاپ)
     */
    suspend fun restoreBackup(fileId: String? = null): Result<KisehBackupPayload> =
        withContext(Dispatchers.IO) {
            try {
                val userId = authManager.getUserId()
                    ?: return@withContext Result.failure(
                        Exception("لطفاً ابتدا وارد حساب کاربری خود شوید")
                    )

                // ۱. اگر نام فایل مشخص نبود، آخرین بکاپ را پیدا کن
                val targetFileName = fileId ?: run {
                    val listResult = listAvailableBackups()
                    val list = listResult.getOrNull()
                        ?: return@withContext Result.failure(
                            listResult.exceptionOrNull()
                                ?: Exception("خطا در دریافت لیست بکاپ‌ها")
                        )
                    list.firstOrNull()?.fileName
                        ?: return@withContext Result.failure(
                            Exception("هیچ نسخه پشتیبانی یافت نشد")
                        )
                }

                Log.i(TAG, "Restore: downloading $targetFileName")

                // ۲. خواندن از Firestore
                val doc = firestore.collection(COLLECTION_USERS).document(userId)
                    .collection(COLLECTION_BACKUPS).document(targetFileName)
                    .get()
                    .await()

                if (!doc.exists()) {
                    return@withContext Result.failure(
                        Exception("فایل بکاپ مورد نظر در حساب شما یافت نشد")
                    )
                }

                val encryptedBase64 = doc.getString("encryptedData")
                    ?: return@withContext Result.failure(
                        Exception("داده‌های بکاپ در سند یافت نشد")
                    )

                // ۳. تبدیل از Base64 به بایت
                val encryptedBytes = try {
                    Base64.decode(encryptedBase64, Base64.NO_WRAP)
                } catch (e: Exception) {
                    Log.e(TAG, "Base64 decode failed", e)
                    return@withContext Result.failure(
                        Exception("خطا در رمزگشایی داده Base64")
                    )
                }

                Log.i(TAG, "Downloaded ${encryptedBytes.size} bytes from Firestore")

                // ۴. رمزگشایی
                val payload = try {
                    BackupCryptoManager.decryptBackup(encryptedBytes)
                } catch (e: Exception) {
                    Log.e(TAG, "Decryption failed", e)
                    return@withContext Result.failure(
                        Exception("خطا در رمزگشایی فایل بکاپ. ممکن است فایل خراب یا کلید نامعتبر باشد")
                    )
                }

                // ۵. بازنشانی اتمیک در دیتابیس
                Log.i(TAG, "Restoring data to Room database...")
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

                // ۶. بازیابی داده‌های یادگیری پیامک بانکی
                if (context != null && payload.learnedMerchants.isNotEmpty()) {
                    try {
                        UserCategoryLearner.getInstance(context)
                            .learnAll(payload.learnedMerchants)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to restore learned merchants: ${e.message}")
                    }
                }

                Log.i(TAG, "Restore Complete: tx=${payload.transactions.size}, goals=${payload.savingsGoals.size}")
                Result.success(payload)
            } catch (e: Exception) {
                Log.e(TAG, "Error restoring backup", e)
                Result.failure(Exception("خطا در بازیابی بکاپ: ${e.message}"))
            }
        }
}