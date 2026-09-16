package com.example

import com.example.security.SecurityPreferencesManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * تست‌های امنیت و الگوریتم‌های رمزنگاری فاز ۵
 */
class SecurityTest {

    @Test
    fun testSaltGeneration_isRandomAndUnique() {
        val salt1 = SecurityPreferencesManager.generateSalt()
        val salt2 = SecurityPreferencesManager.generateSalt()

        // نمک باید ۱۶ بایت (۳۲ کاراکتر هگز) باشد
        assertEquals(32, salt1.length)
        assertEquals(32, salt2.length)
        // دو نمک متوالی نباید یکسان باشند
        assertNotEquals(salt1, salt2)
    }

    @Test
    fun testSaltedSha256_hashesCorrectly() {
        val pin = "1379"
        val salt = SecurityPreferencesManager.generateSalt()

        val hash = SecurityPreferencesManager.hashPinWithSalt(pin, salt)

        // هش نباید متنی برابر با پین باشد
        assertNotEquals(pin, hash)
        // طول خروجی SHA-256 هگز باید ۶۴ کاراکتر باشد
        assertEquals(64, hash.length)

        // تایید با همان پین و نمک باید دقیقاً یکسان شود
        val recomputedHash = SecurityPreferencesManager.hashPinWithSalt(pin, salt)
        assertEquals(hash, recomputedHash)

        // تایید با پین اشتباه باید متفاوت باشد
        val wrongPinHash = SecurityPreferencesManager.hashPinWithSalt("1234", salt)
        assertNotEquals(hash, wrongPinHash)
    }

    @Test
    fun testRainbowTableResistance_differentSaltsProduceDifferentHashes() {
        val samePin = "4567"
        val saltA = SecurityPreferencesManager.generateSalt()
        val saltB = SecurityPreferencesManager.generateSalt()

        val hashA = SecurityPreferencesManager.hashPinWithSalt(samePin, saltA)
        val hashB = SecurityPreferencesManager.hashPinWithSalt(samePin, saltB)

        // حتی اگر پین یکسان باشد، به دلیل سالت مجزا هش‌ها باید کاملاً متفاوت باشند
        assertNotEquals(hashA, hashB)
    }

    @Test
    fun testCloudBackupPrivacy_doesNotContainSensitiveSmsData() {
        val payloadFields = com.example.backup.KisehBackupPayload::class.java.declaredFields.map { it.name }

        // اطمینان از اینکه هیچ فیلد مربوط به پیامک خام، جدول pending_bank_sms یا لاگ پیامکی در KisehBackupPayload نیست
        assertFalse(payloadFields.contains("pendingSms"))
        assertFalse(payloadFields.contains("rawSms"))
        assertFalse(payloadFields.contains("smsList"))
        assertFalse(payloadFields.contains("pendingBankSms"))

        // اطمینان از اینکه در موجودیت PendingBankSms فیلد متن خام پیامک (rawBody) وجود ندارد
        val pendingFields = com.example.data.PendingBankSms::class.java.declaredFields.map { it.name }
        assertFalse(pendingFields.contains("rawBody"))
        assertFalse(pendingFields.contains("rawText"))
        assertFalse(pendingFields.contains("fullCardNumber"))
    }
}
