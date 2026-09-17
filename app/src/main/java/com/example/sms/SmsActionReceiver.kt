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

        // بررسی صریح VIP: کاربران غیر VIP امکان ثبت هیچ تراکنشی از پیامک را ندارند
        if (!com.example.billing.BillingManager.isProUser(context)) {
            return
        }

        val action = intent.action ?: return
        if (action != ACTION_CONFIRM && action != ACTION_REJECT) return

        val smsId = intent.getLongExtra(EXTRA_SMS_ID, -1L)
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)

        // بستن نوتیفیکیشن در صورت وجود شناسه معتبر
        if (notificationId != -1) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.cancel(notificationId)
        }

        if (smsId <= 0L) return

        val prefsManager = BankSmsPreferencesManager.getInstance(context)
        if (!prefsManager.isQuickActionEnabled()) {
            return
        }

        val pendingPendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (!com.example.billing.BillingManager.isProUser(appContext)) {
                    return@launch
                }
                val database = AppDatabase.getDatabase(appContext)
                val repository = PendingBankSmsRepository(database.pendingBankSmsDao())

                when (action) {
                    ACTION_CONFIRM -> {
                        val pending = repository.getById(smsId)
                        if (pending != null && pending.getSmsStatus() == SmsStatus.PENDING) {
                            if (pending.merchant.isNotBlank() && prefsManager.isUserLearningEnabled() && com.example.billing.BillingManager.isProUser(appContext)) {
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

                            // ⭐ اصلاح: چک تکراری حذف شد (قبلاً هنگام دریافت پیامک انجام شده)
                            // حالا مستقیماً تراکنش ثبت می‌شود
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