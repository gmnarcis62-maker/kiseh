package com.example.backup

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * مدیریت رمزنگاری پیشرفته و امن AES-256-GCM برای فایل‌های بکاپ
 * - تضمین محرمانگی و اصالت داده‌های مالی کاربر (Confidentiality & Integrity)
 * - استخراج Checksum معتبر با الگوریتم SHA-256
 */
object BackupCryptoManager {

    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128

    // نمک ثابت اختصاصی برنامه برای اشتقاق کلید امن ۲۵۶ بیتی
    private val APP_BACKUP_SALT = "kiseh_cloud_backup_salt_secure_2026".toByteArray(Charsets.UTF_8)
    private val DEFAULT_SEED = "kiseh_vault_key_aes256_financial_guard".toByteArray(Charsets.UTF_8)

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    /**
     * تولید کلید متقارن ۲۵۶ بیتی استاندارد AES از سید امن با SHA-256
     */
    private fun deriveSecretKey(customSeed: ByteArray = DEFAULT_SEED): SecretKey {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(APP_BACKUP_SALT)
        val keyBytes = md.digest(customSeed)
        return SecretKeySpec(keyBytes, ALGORITHM)
    }

    /**
     * محاسبه Checksum امن SHA-256 روی رشته متنی
     */
    fun calculateChecksum(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * تبدیل پی‌لود بکاپ به بایت‌های رمزنگاری شده AES-256-GCM
     */
    fun encryptBackup(payload: KisehBackupPayload): ByteArray {
        val adapter = moshi.adapter(KisehBackupPayload::class.java)

        // ابتدا بدون Checksum به JSON تبدیل می‌شود تا هش محاسبه شود
        val payloadWithoutChecksum = payload.copy(checksum = "")
        val rawJson = adapter.toJson(payloadWithoutChecksum)
        val computedChecksum = calculateChecksum(rawJson)

        // پی‌لود نهایی شامل Checksum
        val finalPayload = payload.copy(checksum = computedChecksum)
        val finalJson = adapter.toJson(finalPayload)
        val plainBytes = finalJson.toByteArray(Charsets.UTF_8)

        // ایجاد IV امن تصادفی
        val iv = ByteArray(GCM_IV_LENGTH)
        SecureRandom().nextBytes(iv)

        val secretKey = deriveSecretKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val parameterSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec)

        val cipherBytes = cipher.doFinal(plainBytes)

        // بسته‌بندی: ۴ بایت طول IV + بایت‌های IV + بایت‌های رمز شده
        val byteBuffer = ByteBuffer.allocate(4 + iv.size + cipherBytes.size)
        byteBuffer.putInt(iv.size)
        byteBuffer.put(iv)
        byteBuffer.put(cipherBytes)

        return byteBuffer.array()
    }

    /**
     * رمزگشایی و اعتبارسنجی فایل بکاپ
     * @throws IllegalStateException در صورت مخدوش بودن فایل، کلید اشتباه یا عدم تطابق Checksum
     */
    fun decryptBackup(encryptedBytes: ByteArray): KisehBackupPayload {
        android.util.Log.i("GoogleDriveBackup", "Decrypt Start: decrypting backup payload of ${encryptedBytes.size} bytes")

        if (encryptedBytes.size < 4 + GCM_IV_LENGTH) {
            throw IllegalArgumentException("فایل پشتیبان خراب است: اندازه فایل بکاپ نامعتبر است.")
        }

        val byteBuffer = ByteBuffer.wrap(encryptedBytes)
        val ivLength = try {
            byteBuffer.int
        } catch (e: Exception) {
            throw IllegalArgumentException("فایل پشتیبان خراب است: ساختار هدر نامعتبر است.")
        }

        if (ivLength != GCM_IV_LENGTH) {
            throw IllegalArgumentException("فایل پشتیبان خراب است: فرمت هدر فایل رمزنگاری شده پشتیبانی نمی‌شود.")
        }

        val iv = ByteArray(ivLength)
        byteBuffer.get(iv)

        val cipherBytes = ByteArray(byteBuffer.remaining())
        byteBuffer.get(cipherBytes)

        val decryptedBytes = try {
            val secretKey = deriveSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val parameterSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec)
            cipher.doFinal(cipherBytes)
        } catch (e: Exception) {
            android.util.Log.e("GoogleDriveBackup", "Decryption cipher failure", e)
            throw IllegalStateException("رمزگشایی فایل ناموفق بود: داده‌های رمزنگاری‌شده مخدوش هستند یا کلید امن نامعتبر است.")
        }

        val decryptedJson = String(decryptedBytes, Charsets.UTF_8)

        val adapter = moshi.adapter(KisehBackupPayload::class.java)
        val payload = try {
            adapter.fromJson(decryptedJson)
        } catch (e: Exception) {
            throw IllegalStateException("فایل پشتیبان خراب است: ساختار JSON داده‌های مالی قابل خواندن نیست.")
        } ?: throw IllegalStateException("فایل پشتیبان خراب است: امکان خواندن ساختار فایل بکاپ وجود ندارد.")

        // اعتبارسنجی Checksum
        val payloadWithoutChecksum = payload.copy(checksum = "")
        val rawJsonForCheck = adapter.toJson(payloadWithoutChecksum)
        val expectedChecksum = calculateChecksum(rawJsonForCheck)

        if (payload.checksum != expectedChecksum) {
            throw IllegalStateException("فایل پشتیبان خراب است: اعتبارسنجی چک‌سام با شکست مواجه شد! فایل ممکن است دستکاری شده یا ناقص باشد.")
        }

        return payload
    }
}
