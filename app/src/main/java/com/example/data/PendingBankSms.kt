package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * وضعیت پیامک در صف تایید
 */
enum class SmsStatus {
    PENDING,
    CONFIRMED,
    REJECTED
}

/**
 * جدول ذخیره موقت پیامک‌های بانکی استخراج‌شده قبل از ثبت قطعی به عنوان تراکنش
 *
 * نکته امنیتی: به هیچ وجه متن خام پیامک و شماره کارت کامل در دیتابیس ذخیره نمی‌شود.
 */
@Entity(tableName = "pending_bank_sms")
data class PendingBankSms(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bankName: String,
    val amount: Long,
    val isIncome: Boolean,
    val balance: Long? = null,
    val cardLastDigits: String? = null,
    val merchant: String = "",
    val suggestedCategory: String = "سایر",
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = SmsStatus.PENDING.name
) {
    fun getSmsStatus(): SmsStatus {
        return try {
            SmsStatus.valueOf(status)
        } catch (e: Exception) {
            SmsStatus.PENDING
        }
    }
}
