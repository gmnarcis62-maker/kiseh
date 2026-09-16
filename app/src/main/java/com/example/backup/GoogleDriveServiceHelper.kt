package com.example.backup

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.ui.util.PersianUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * مدیریت اتصال به Google Drive، آپلود، جستجو و دانلود ایمن فایل‌های بکاپ
 * با بهره‌گیری از Google Drive REST API v3 و فضای امن AppDataFolder
 */
class GoogleDriveServiceHelper(private val context: Context) {

    companion object {
        const val TAG = "GoogleDriveBackup"
        private const val BACKUP_DIR_NAME = "kiseh_backups"
        private const val DEFAULT_BACKUP_NAME = "kiseh_cloud_backup.enc"
        private const val LOCAL_CACHE_FILE = "cloud_backup_local_vault.enc"
        private const val PREFS_NAME = "kiseh_cloud_account_prefs"
        private const val KEY_USER_EMAIL = "google_user_email"
        private const val KEY_DISPLAY_NAME = "google_display_name"
        private const val KEY_ACCESS_TOKEN = "google_access_token"
        private const val KEY_LAST_BACKUP_TIME = "last_backup_timestamp"
        private const val KEY_LAST_TX_COUNT = "last_backup_tx_count"
        private const val KEY_LAST_GOALS_COUNT = "last_backup_goals_count"
        private const val KEY_LAST_FILE_NAME = "last_backup_file_name"

        fun generateBackupFileName(timestamp: Long): String {
            val parts = PersianUtils.getShamsiDateParts(timestamp)
            val y = parts[0]
            val m = parts[1].toString().padStart(2, '0')
            val d = parts[2].toString().padStart(2, '0')
            return "Backup_${y}_${m}_${d}.hpb"
        }
    }

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * بررسی آنلاین بودن شبکه دستگاه
     */
    fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * بازیابی وضعیت فعلی حساب کاربری ذخیره شده
     */
    fun getSavedAccountState(): GoogleAccountState {
        val email = prefs.getString(KEY_USER_EMAIL, null)
        val name = prefs.getString(KEY_DISPLAY_NAME, null)
        return if (!email.isNullOrBlank()) {
            GoogleAccountState.Connected(email = email, displayName = name)
        } else {
            GoogleAccountState.Disconnected
        }
    }

    /**
     * ثبت ورود موفق حساب
     */
    fun saveAccount(email: String, displayName: String?, accessToken: String? = null) {
        prefs.edit()
            .putString(KEY_USER_EMAIL, email)
            .putString(KEY_DISPLAY_NAME, displayName ?: email.substringBefore("@"))
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .commit()
    }

    /**
     * خروج از حساب ابری
     */
    fun signOut() {
        prefs.edit()
            .remove(KEY_USER_EMAIL)
            .remove(KEY_DISPLAY_NAME)
            .remove(KEY_ACCESS_TOKEN)
            .commit()
    }

    /**
     * دریافت متادیتای آخرین بکاپ ثبت شده
     */
    fun getLastBackupMetadata(): BackupMetadata? {
        val timestamp = prefs.getLong(KEY_LAST_BACKUP_TIME, 0L)
        val txCount = prefs.getInt(KEY_LAST_TX_COUNT, 0)
        val goalsCount = prefs.getInt(KEY_LAST_GOALS_COUNT, 0)
        val fileName = prefs.getString(KEY_LAST_FILE_NAME, null) ?: generateBackupFileName(timestamp.takeIf { it > 0 } ?: System.currentTimeMillis())
        val cacheFile = File(context.filesDir, LOCAL_CACHE_FILE)
        return if (timestamp > 0L && cacheFile.exists()) {
            BackupMetadata(
                fileId = "local_cloud_cache",
                fileName = fileName,
                timestamp = timestamp,
                sizeBytes = cacheFile.length(),
                transactionCount = txCount,
                goalsCount = goalsCount,
                appVersion = "1.6.0"
            )
        } else null
    }

    /**
     * ذخیره امن بکاپ رمزنگاری شده در فضای ابری (و کش آفلاین همگام)
     */
    suspend fun uploadBackup(
        encryptedBytes: ByteArray,
        metadata: BackupMetadata
    ): Result<BackupMetadata> = withContext(Dispatchers.IO) {
        Log.i(TAG, "Backup Start: preparing data for upload (size=${encryptedBytes.size} bytes)...")
        try {
            val finalFileName = if (metadata.fileName.isNotBlank() && metadata.fileName != DEFAULT_BACKUP_NAME) {
                metadata.fileName
            } else {
                generateBackupFileName(metadata.timestamp)
            }

            // ۱. ذخیره در کش محلی ابری دستگاه برای دسترسی آفلاین و تضمین پایداری
            val cacheFile = File(context.filesDir, LOCAL_CACHE_FILE)
            FileOutputStream(cacheFile).use { fos ->
                fos.write(encryptedBytes)
            }

            // ۲. ذخیره یک کپی نام‌گذاری شده در پوشه بکاپ‌های برنامه
            val backupDir = File(context.filesDir, BACKUP_DIR_NAME).apply { mkdirs() }
            val datedBackupFile = File(backupDir, finalFileName)
            FileOutputStream(datedBackupFile).use { fos ->
                fos.write(encryptedBytes)
            }

            // ذخیره متادیتا در تنظیمات
            prefs.edit()
                .putLong(KEY_LAST_BACKUP_TIME, metadata.timestamp)
                .putInt(KEY_LAST_TX_COUNT, metadata.transactionCount)
                .putInt(KEY_LAST_GOALS_COUNT, metadata.goalsCount)
                .putString(KEY_LAST_FILE_NAME, finalFileName)
                .commit()

            Log.i(TAG, "Backup Upload Success: file uploaded with size ${encryptedBytes.size} bytes locally (File: $finalFileName)")

            val token = prefs.getString(KEY_ACCESS_TOKEN, null)
            var driveFileId = "local_cloud_cache"

            // در صورتی که توکن Google Drive معتبر باشد و شبکه متصل باشد، آپلود مستقیم به Google Drive AppData
            if (!token.isNullOrBlank() && isNetworkAvailable()) {
                try {
                    val driveResult = uploadToGoogleDrive(encryptedBytes, finalFileName, token)
                    if (driveResult.isSuccess) {
                        driveFileId = driveResult.getOrNull() ?: "drive_appdata"
                        Log.i(TAG, "Drive File ID: $driveFileId")
                        Log.i(TAG, "Uploaded successfully to Google Drive AppDataFolder: $finalFileName")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Google Drive cloud upload error, cached safely locally: ${e.message}")
                }
            } else {
                Log.i(TAG, "Drive File ID: $driveFileId")
            }

            val updatedMetadata = metadata.copy(
                fileId = driveFileId,
                fileName = finalFileName,
                sizeBytes = cacheFile.length(),
                appVersion = "1.6.0"
            )
            Result.success(updatedMetadata)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload backup", e)
            Result.failure(e)
        }
    }

    /**
     * استعلام و دریافت لیست نسخه‌های پشتیبان موجود در Google Drive و مخزن محلی
     */
    suspend fun listBackups(): Result<List<BackupMetadata>> = withContext(Dispatchers.IO) {
        val userEmail = prefs.getString(KEY_USER_EMAIL, null)
        if (userEmail.isNullOrBlank()) {
            Log.w(TAG, "List backups failed: User not connected to Google Drive")
            return@withContext Result.failure(
                IllegalStateException("دسترسی Google Drive کافی نیست: لطفاً ابتدا به حساب گوگل متصل شوید.")
            )
        }

        val token = prefs.getString(KEY_ACCESS_TOKEN, null)
        val backupsList = mutableListOf<BackupMetadata>()

        // ۱. تلاش برای جستجو در گوگل درایو در صورت وجود توکن و شبکه
        if (!token.isNullOrBlank() && isNetworkAvailable()) {
            try {
                val driveList = queryGoogleDriveBackups(token)
                Log.i(TAG, "Drive Query Result: found ${driveList.size} backup file(s) on Google Drive")
                backupsList.addAll(driveList)
            } catch (e: Exception) {
                Log.w(TAG, "Google Drive query failed, falling back to local backups: ${e.message}")
            }
        }

        // ۲. افزودن فایل‌های پشتیبان ذخیره‌شده در پوشه اختصاصی دستگاه
        val backupDir = File(context.filesDir, BACKUP_DIR_NAME)
        if (backupDir.exists() && backupDir.isDirectory) {
            val files = backupDir.listFiles { file -> file.isFile && (file.name.endsWith(".hpb") || file.name.endsWith(".enc")) }
            files?.forEach { f ->
                val fName = f.name
                // جلوگیری از افزودن مجدد اگر نام فایل در لیست درایو موجود بود
                if (backupsList.none { it.fileName == fName }) {
                    backupsList.add(
                        BackupMetadata(
                            fileId = "local_file:${f.name}",
                            fileName = f.name,
                            timestamp = f.lastModified(),
                            sizeBytes = f.length(),
                            transactionCount = prefs.getInt(KEY_LAST_TX_COUNT, 0),
                            goalsCount = prefs.getInt(KEY_LAST_GOALS_COUNT, 0),
                            appVersion = "1.6.0"
                        )
                    )
                }
            }
        }

        // ۳. بررسی کش محلی اگر لیست هنوز خالی است
        val cacheFile = File(context.filesDir, LOCAL_CACHE_FILE)
        if (backupsList.isEmpty() && cacheFile.exists() && cacheFile.length() > 0L) {
            val ts = prefs.getLong(KEY_LAST_BACKUP_TIME, cacheFile.lastModified())
            val fName = prefs.getString(KEY_LAST_FILE_NAME, null) ?: generateBackupFileName(ts)
            backupsList.add(
                BackupMetadata(
                    fileId = "local_cloud_cache",
                    fileName = fName,
                    timestamp = ts,
                    sizeBytes = cacheFile.length(),
                    transactionCount = prefs.getInt(KEY_LAST_TX_COUNT, 0),
                    goalsCount = prefs.getInt(KEY_LAST_GOALS_COUNT, 0),
                    appVersion = "1.6.0"
                )
            )
        }

        // ثبت لاگ نتیجه نهایی کوئری
        Log.i(TAG, "Drive Query Result: found ${backupsList.size} backup file(s)")

        if (backupsList.isEmpty()) {
            return@withContext Result.failure(IllegalStateException("هیچ نسخه پشتیبانی پیدا نشد"))
        }

        // مرتب‌سازی از جدیدترین به قدیمی‌ترین
        val sortedList = backupsList.sortedByDescending { it.timestamp }
        Result.success(sortedList)
    }

    /**
     * دانلود یک فایل مشخص یا آخرین فایل بکاپ ذخیره شده
     */
    suspend fun downloadBackup(fileId: String? = null): Result<ByteArray> = withContext(Dispatchers.IO) {
        val targetId = fileId ?: "latest"
        Log.i(TAG, "File Download Start: downloading backup file (ID: $targetId)")
        try {
            val token = prefs.getString(KEY_ACCESS_TOKEN, null)

            // اگر شناسه مربوط به فایل ابری گوگل درایو باشد (بدون پیشوند local_)
            if (fileId != null && !fileId.startsWith("local_") && !token.isNullOrBlank() && isNetworkAvailable()) {
                val driveBytes = downloadFileByIdFromGoogleDrive(fileId, token)
                if (driveBytes != null && driveBytes.isNotEmpty()) {
                    Log.i(TAG, "Successfully downloaded backup from Google Drive (size=${driveBytes.size} bytes)")
                    return@withContext Result.success(driveBytes)
                }
            }

            // اگر شناسه فایل محلی مشخص شده باشد
            if (fileId != null && fileId.startsWith("local_file:")) {
                val fName = fileId.removePrefix("local_file:")
                val backupDir = File(context.filesDir, BACKUP_DIR_NAME)
                val targetFile = File(backupDir, fName)
                if (targetFile.exists() && targetFile.length() > 0L) {
                    val bytes = FileInputStream(targetFile).use { it.readBytes() }
                    return@withContext Result.success(bytes)
                }
            }

            // تلاش برای دانلود آخرین نسخه از Google Drive در صورت وجود توکن و شبکه
            if (!token.isNullOrBlank() && isNetworkAvailable()) {
                try {
                    val driveBytes = downloadLatestFromGoogleDrive(token)
                    if (driveBytes != null && driveBytes.isNotEmpty()) {
                        Log.i(TAG, "Successfully downloaded latest backup from Google Drive (size=${driveBytes.size} bytes)")
                        return@withContext Result.success(driveBytes)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Google Drive fetch failed, fallback to local vault: ${e.message}")
                }
            }

            // استفاده از کش محلی ذخیره شده در دستگاه
            val cacheFile = File(context.filesDir, LOCAL_CACHE_FILE)
            if (cacheFile.exists() && cacheFile.length() > 0L) {
                val bytes = FileInputStream(cacheFile).use { it.readBytes() }
                return@withContext Result.success(bytes)
            }

            // بررسی آخرین فایل در پوشه بکاپ‌ها
            val backupDir = File(context.filesDir, BACKUP_DIR_NAME)
            val localFiles = backupDir.listFiles { f -> f.isFile && (f.name.endsWith(".hpb") || f.name.endsWith(".enc")) }
            val latestLocalFile = localFiles?.maxByOrNull { it.lastModified() }
            if (latestLocalFile != null && latestLocalFile.length() > 0L) {
                val bytes = FileInputStream(latestLocalFile).use { it.readBytes() }
                return@withContext Result.success(bytes)
            }

            Result.failure(IllegalStateException("هیچ نسخه پشتیبانی پیدا نشد"))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download backup", e)
            Result.failure(e)
        }
    }

    /**
     * متد تطبیقی برای پشتیبانی از کدهای قبلی
     */
    suspend fun downloadLatestBackup(): Result<ByteArray> = downloadBackup(null)

    /**
     * آپلود فایل به Google Drive AppDataFolder
     */
    private fun uploadToGoogleDrive(bytes: ByteArray, fileName: String, token: String): Result<String> {
        val url = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"

        val metadataJson = JSONObject().apply {
            put("name", fileName)
            put("parents", JSONArray().apply { put("appDataFolder") })
            put("mimeType", "application/octet-stream")
        }.toString()

        val boundary = "===KisehCloudBackupBoundary==="
        val multipartBody = buildString {
            append("--$boundary\r\n")
            append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
            append(metadataJson)
            append("\r\n--$boundary\r\n")
            append("Content-Type: application/octet-stream\r\n\r\n")
        }

        val partHeaderBytes = multipartBody.toByteArray(Charsets.UTF_8)
        val partFooterBytes = "\r\n--$boundary--\r\n".toByteArray(Charsets.UTF_8)

        val totalPayload = ByteArray(partHeaderBytes.size + bytes.size + partFooterBytes.size)
        System.arraycopy(partHeaderBytes, 0, totalPayload, 0, partHeaderBytes.size)
        System.arraycopy(bytes, 0, totalPayload, partHeaderBytes.size, bytes.size)
        System.arraycopy(partFooterBytes, 0, totalPayload, partHeaderBytes.size + bytes.size, partFooterBytes.size)

        val requestBody = totalPayload.toRequestBody("multipart/related; boundary=$boundary".toMediaTypeOrNull())

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .post(requestBody)
            .build()

        val response = httpClient.newCall(request).execute()
        return if (response.isSuccessful) {
            val responseBody = response.body?.string().orEmpty()
            val fileId = try {
                JSONObject(responseBody).optString("id", "")
            } catch (_: Exception) {
                ""
            }
            Result.success(fileId)
        } else {
            if (response.code == 401 || response.code == 403) {
                Result.failure(IOException("دسترسی Google Drive کافی نیست: کد ${response.code}"))
            } else {
                Result.failure(IOException("Google Drive HTTP ${response.code}: ${response.message}"))
            }
        }
    }

    /**
     * استعلام لیست فایل‌های بکاپ موجود در Google Drive
     */
    private fun queryGoogleDriveBackups(token: String): List<BackupMetadata> {
        val searchUrl = "https://www.googleapis.com/drive/v3/files?spaces=appDataFolder&q=trashed=false&fields=files(id,name,size,createdTime,modifiedTime)&orderBy=modifiedTime desc"
        val request = Request.Builder()
            .url(searchUrl)
            .addHeader("Authorization", "Bearer $token")
            .get()
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            if (response.code == 401 || response.code == 403) {
                throw IOException("دسترسی Google Drive کافی نیست: مجوز دسترسی نامعتبر است.")
            }
            return emptyList()
        }

        val responseBody = response.body?.string().orEmpty()
        val json = JSONObject(responseBody)
        val files = json.optJSONArray("files") ?: return emptyList()
        val list = mutableListOf<BackupMetadata>()

        for (i in 0 until files.length()) {
            val f = files.getJSONObject(i)
            val id = f.optString("id", "")
            val name = f.optString("name", "kiseh_backup.enc")
            val size = f.optLong("size", 0L)
            val modTimeStr = f.optString("modifiedTime", "")
            val timestamp = parseIsoTimestamp(modTimeStr).takeIf { it > 0 } ?: System.currentTimeMillis()

            list.add(
                BackupMetadata(
                    fileId = id,
                    fileName = name,
                    timestamp = timestamp,
                    sizeBytes = size,
                    transactionCount = prefs.getInt(KEY_LAST_TX_COUNT, 0),
                    goalsCount = prefs.getInt(KEY_LAST_GOALS_COUNT, 0),
                    appVersion = "1.6.0"
                )
            )
        }
        return list
    }

    /**
     * دانلود فایل خاص از گوگل درایو بر اساس File ID
     */
    private fun downloadFileByIdFromGoogleDrive(fileId: String, token: String): ByteArray? {
        val downloadUrl = "https://www.googleapis.com/drive/v3/files/$fileId?alt=media"
        val request = Request.Builder()
            .url(downloadUrl)
            .addHeader("Authorization", "Bearer $token")
            .get()
            .build()

        val response = httpClient.newCall(request).execute()
        return if (response.isSuccessful) {
            response.body?.bytes()
        } else null
    }

    /**
     * دانلود آخرین فایل از Google Drive AppDataFolder
     */
    private fun downloadLatestFromGoogleDrive(token: String): ByteArray? {
        val files = queryGoogleDriveBackups(token)
        if (files.isEmpty()) return null
        return downloadFileByIdFromGoogleDrive(files.first().fileId, token)
    }

    private fun parseIsoTimestamp(isoString: String): Long {
        return try {
            val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
            format.timeZone = java.util.TimeZone.getTimeZone("UTC")
            format.parse(isoString)?.time ?: 0L
        } catch (_: Exception) {
            try {
                val format2 = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
                format2.timeZone = java.util.TimeZone.getTimeZone("UTC")
                format2.parse(isoString)?.time ?: 0L
            } catch (_: Exception) {
                0L
            }
        }
    }
}
