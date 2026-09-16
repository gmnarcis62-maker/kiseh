package com.example.data

import kotlinx.coroutines.flow.Flow

/**
 * مدیریت مخزن پیامک‌های بانکی معلق در صف تایید
 */
class PendingBankSmsRepository(
    private val pendingBankSmsDao: PendingBankSmsDao
) {

    /**
     * جریان زنده لیست تمامی پیامک‌های در انتظار تایید کاربر
     */
    val allPendingSms: Flow<List<PendingBankSms>> = pendingBankSmsDao.getAllPending()

    /**
     * ثبت پیامک جدید در صف تایید
     */
    suspend fun insert(sms: PendingBankSms): Long {
        return pendingBankSmsDao.insert(sms)
    }

    /**
     * دریافت لیست پیامک‌های معلق به صورت مستقیم
     */
    suspend fun getAllPendingList(): List<PendingBankSms> {
        return pendingBankSmsDao.getAllPendingList()
    }

    /**
     * دریافت یک پیامک بر اساس شناسه
     */
    suspend fun getById(id: Long): PendingBankSms? {
        return pendingBankSmsDao.getById(id)
    }

    /**
     * تغییر وضعیت به تایید شده (CONFIRMED)
     */
    suspend fun markAsConfirmed(id: Long) {
        pendingBankSmsDao.updateStatus(id, SmsStatus.CONFIRMED.name)
    }

    /**
     * تغییر وضعیت به رد شده (REJECTED)
     */
    suspend fun markAsRejected(id: Long) {
        pendingBankSmsDao.updateStatus(id, SmsStatus.REJECTED.name)
    }

    /**
     * حذف یک پیامک
     */
    suspend fun delete(sms: PendingBankSms) {
        pendingBankSmsDao.delete(sms)
    }

    /**
     * حذف پیامک با شناسه
     */
    suspend fun deleteById(id: Long) {
        pendingBankSmsDao.deleteById(id)
    }

    /**
     * پاکسازی پیامک‌های قدیمی پردازش شده (تایید یا رد شده)
     */
    suspend fun cleanupOldProcessedSms(thresholdTimestamp: Long) {
        pendingBankSmsDao.deleteOldProcessedSms(thresholdTimestamp)
    }
}
