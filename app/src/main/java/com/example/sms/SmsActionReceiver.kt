package com.example.sms

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.AppDatabase
import com.example.data.PendingBankSmsRepository
import com.example.data.SmsStatus
import com.example.data.Transaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * دریافت امن و پردازش اکشن‌های کلیک روی دکمه‌های نوتیفیکیشن پیامک بانکی
 */
class SmsActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_CONFIRM = "com.example.sms.ACTION_CONFIRM"
        const val ACTION_REJECT = "com.example.sms.ACTION_REJECT"
        const val EXTRA_SMS_ID = "extra_sms_id"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        val appContext = context.applicationContext

        // ⭐ ۱. اطمینان از مقداردهی اولیه BillingManager قبل از بررسی VIP
        try {
            com.example.billing.BillingManager.init(appContext)
        } catch (_: Throwable) {}

        // ⭐ ۲. حالا چک VIP معتبر است
        if (!com.example.billing.BillingManager.isProUser(appContext)) {
            android.util.Log.d("BANK_SMS_DEBUG", "SmsActionReceiver: user is not VIP, skipping")
            return
        }

        val action = intent.action ?: return
        if (action != ACTION_CONFIRM && action != ACTION_REJECT) return

        val smsId = intent.getLongExtra(EXTRA_SMS_ID, -1L)
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)

        // بستن نوتیفیکیشن
        if (notificationId != -1) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.cancel(notificationId)
        }

        if (smsId <= 0L) {
            android.util.Log.d("BANK_SMS_DEBUG", "SmsActionReceiver: invalid smsId=$smsId")
            return
        }

        val prefsManager = BankSmsPreferencesManager.getInstance(appContext)
        if (!prefsManager.isQuickActionEnabled()) {
            android.util.Log.d("BANK_SMS_DEBUG", "SmsActionReceiver: quick actions disabled")
            return
        }

        val pendingPendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = AppDatabase.getDatabase(appContext)
                val repository = PendingBankSmsRepository(database.pendingBankSmsDao())

                when (action) {
                    ACTION_CONFIRM -> {
                        val pending = repository.getById(smsId)
                        if (pending != null && pending.getSmsStatus() == SmsStatus.PENDING) {
                            if (pending.merchant.isNotBlank() && prefsManager.isUserLearningEnabled()) {
                                UserCategoryLearner.getInstance(appContext).learn(pending.merchant, pending.suggestedCategory)
                            }
                            val description = buildString {
                                append(pending.bankName)
                                if (pending.merchant.isNotBlank()) {
                                    append(" - ")
                                    append(pending.merchant)
                                }
                                if (!pending.cardLastDigits.isNullOrBlank()) {
                                    append(" (کارت *")
                                    append(pending.cardLastDigits)
                                    append(")")
                                }
                            }

                            val transaction = Transaction(
                                amount = pending.amount,
                                category = SmartCategoryMatcher.toCanonicalKisehCategory(pending.suggestedCategory),
                                description = description,
                                isIncome = pending.isIncome,
                                date = pending.createdAt
                            )
                            database.transactionDao().insert(transaction)
                            repository.markAsConfirmed(smsId)
                            android.util.Log.d("BANK_SMS_DEBUG", "ACTION_CONFIRM: transaction inserted (id=${transaction.id})")
                        }
                    }
                    ACTION_REJECT -> {
                        repository.markAsRejected(smsId)
                        android.util.Log.d("BANK_SMS_DEBUG", "ACTION_REJECT: pending marked as rejected")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("BANK_SMS_DEBUG", "SmsActionReceiver error", e)
            } finally {
                pendingPendingResult.finish()
            }
        }
    }
}