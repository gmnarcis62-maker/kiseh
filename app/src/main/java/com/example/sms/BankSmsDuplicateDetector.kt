package com.example.sms

import android.content.Context
import com.example.data.AppDatabase
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * سیستم تشخیص امن و هوشمند پیامک‌ها و تراکنش‌های تکراری (Duplicate Protection)
 *
 * بدون ذخیره‌سازی متن خام پیامک (حفظ کامل حریم خصوصی)
 */
object BankSmsDuplicateDetector {

    /**
     * پنجره زمانی بررسی تراکنش‌های تکراری: ۱۰ دقیقه
     */
    const val DUPLICATE_WINDOW_MS = 10 * 60 * 1000L

    // کش درون‌حافظه‌ای جهت بررسی آنی و جلوگیری از ارسال همزمان دو پیامک مشابه توسط سیستم
    private val memoryCache = ConcurrentHashMap<String, Long>()

    /**
     * تولید اثر انگشت امن هش‌شده از مؤلفه‌های کلیدی مالی
     */
    fun generateFingerprint(
        bankName: String,
        amount: Long,
        isIncome: Boolean,
        cardLastDigits: String?,
        merchant: String,
        timeBucket: Long = 0L
    ): String {
        val cleanCard = cardLastDigits?.trim() ?: ""
        val cleanMerchant = merchant.trim().lowercase()
        val rawInput = "$bankName|$amount|$isIncome|$cleanCard|$cleanMerchant|$timeBucket"
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(rawInput.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * بررسی جامع تکراری بودن تراکنش هم در حافظه موقت و هم در پایگاه داده
     */
    suspend fun isDuplicate(
        context: Context,
        bankName: String,
        amount: Long,
        isIncome: Boolean,
        cardLastDigits: String?,
        merchant: String,
        timestamp: Long = System.currentTimeMillis()
    ): Boolean {
        cleanupExpiredMemoryEntries(timestamp)

        val cleanCard = cardLastDigits?.trim() ?: ""
        val cleanMerchant = merchant.trim()

        // ۱. بررسی کش درون‌حافظه‌ای با باکت زمانی ۵ دقیقه‌ای
        val timeBucket = timestamp / (5 * 60 * 1000L)
        val fingerprintCurrent = generateFingerprint(bankName, amount, isIncome, cleanCard, cleanMerchant, timeBucket)
        val fingerprintPrev = generateFingerprint(bankName, amount, isIncome, cleanCard, cleanMerchant, timeBucket - 1)

        if (memoryCache.containsKey(fingerprintCurrent) || memoryCache.containsKey(fingerprintPrev)) {
            return true
        }

        // ۲. بررسی دیتابیس صف پیامک‌های معلق و پردازش‌شده اخیر
        try {
            val db = AppDatabase.getDatabase(context.applicationContext)
            val sinceTimestamp = timestamp - DUPLICATE_WINDOW_MS
            val recentSimilarSms = db.pendingBankSmsDao().findRecentSimilar(
                bankName = bankName,
                amount = amount,
                isIncome = isIncome,
                sinceTimestamp = sinceTimestamp
            )

            for (item in recentSimilarSms) {
                val cardMatch = isCardMatching(cleanCard, item.cardLastDigits)
                val merchantMatch = isMerchantMatching(cleanMerchant, item.merchant)
                if (cardMatch && merchantMatch) {
                    return true
                }
            }

            // ۳. بررسی جدول تراکنش‌های قطعی برنامه در بازه زمانی اخیر
            val recentTransactions = db.transactionDao().getTransactionsByDateRange(
                start = sinceTimestamp,
                end = timestamp + 60_000L
            )

            for (tx in recentTransactions) {
                if (tx.amount == amount && tx.isIncome == isIncome) {
                    val desc = tx.description
                    val hasBank = desc.contains(bankName, ignoreCase = true)
                    val hasMerchant = cleanMerchant.isBlank() || desc.contains(cleanMerchant, ignoreCase = true)
                    val hasCard = cleanCard.isBlank() || desc.contains(cleanCard)
                    if (hasBank && (hasMerchant || hasCard)) {
                        return true
                    }
                }
            }
        } catch (e: Exception) {
            // در صورت بروز هرگونه خطای پایگاه داده، فرض بر عدم تکراری بودن است
        }

        return false
    }

    /**
     * ثبت اثر انگشت در کش درون‌حافظه‌ای
     */
    fun recordProcessed(
        bankName: String,
        amount: Long,
        isIncome: Boolean,
        cardLastDigits: String?,
        merchant: String,
        timestamp: Long = System.currentTimeMillis()
    ) {
        val cleanCard = cardLastDigits?.trim() ?: ""
        val cleanMerchant = merchant.trim()
        val timeBucket = timestamp / (5 * 60 * 1000L)
        val fingerprint = generateFingerprint(bankName, amount, isIncome, cleanCard, cleanMerchant, timeBucket)
        memoryCache[fingerprint] = timestamp
    }

    private fun isCardMatching(cardA: String, cardB: String?): Boolean {
        val b = cardB?.trim() ?: ""
        if (cardA.isBlank() || b.isBlank()) return true
        return cardA == b
    }

    private fun isMerchantMatching(merchantA: String, merchantB: String?): Boolean {
        val b = merchantB?.trim() ?: ""
        if (merchantA.isBlank() || b.isBlank()) return true
        return merchantA.equals(b, ignoreCase = true)
    }

    private fun cleanupExpiredMemoryEntries(currentTime: Long) {
        val cutoff = currentTime - DUPLICATE_WINDOW_MS
        val iterator = memoryCache.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value < cutoff) {
                iterator.remove()
            }
        }
    }

    /**
     * پاکسازی کامل کش (مخصوص تست‌ها)
     */
    fun clearCache() {
        memoryCache.clear()
    }
}
