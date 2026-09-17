package com.example.sms

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.data.AppDatabase
import com.example.data.PendingBankSmsRepository
import com.example.data.Transaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * نسخه تشخیصی: برای پیدا کردن علت عدم واکنش به دکمه‌های اعلان
 */
class SmsActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_CONFIRM = "com.example.sms.ACTION_CONFIRM"
        const val ACTION_REJECT = "com.example.sms.ACTION_REJECT"
        const val EXTRA_SMS_ID = "extra_sms_id"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        android.util.Log.d("SmsActionReceiver", "========== onReceive CALLED ==========")

        // ✅ Toast #1: تایید اینکه receiver صدا زده شده
        Toast.makeText(context, "✅ مرحله ۱: دکمه دریافت شد\nاکشن: ${intent?.action}", Toast.LENGTH_LONG).show()

        if (intent == null) {
            Toast.makeText(context, "❌ intent خالی است", Toast.LENGTH_LONG).show()
            return
        }

        val action = intent.action
        if (action != ACTION_CONFIRM && action != ACTION_REJECT) {
            Toast.makeText(context, "❌ اکشن نامعتبر: $action", Toast.LENGTH_LONG).show()
            return
        }

        // استخراج مطمئن smsId (با پشتیبانی از Long، Int و String)
        val rawSmsId = intent.extras?.get(EXTRA_SMS_ID)
        val smsId: Long = when (rawSmsId) {
            is Long -> rawSmsId
            is Int -> rawSmsId.toLong()
            is String -> rawSmsId.toLongOrNull() ?: -1L
            else -> -1L
        }
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)

        // ✅ Toast #2: نمایش smsId و notificationId
        Toast.makeText(context, "✅ مرحله ۲: SMS ID = $smsId", Toast.LENGTH_LONG).show()

        // بستن اعلان
        if (notificationId != -1) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.cancel(notificationId)
        }

        if (smsId <= 0L) {
            Toast.makeText(context, "❌ smsId نامعتبر: $smsId", Toast.LENGTH_LONG).show()
            return
        }

        val appContext = context.applicationContext
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                android.util.Log.d("SmsActionReceiver", "Coroutine started for action=$action, smsId=$smsId")

                val database = AppDatabase.getDatabase(appContext)
                val repository = PendingBankSmsRepository(database.pendingBankSmsDao())

                val pending = repository.getById(smsId)
                if (pending == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(appContext, "❌ تراکنش پیدا نشد: $smsId", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }

                // ✅ Toast #3: پیدا شدن pending
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        appContext,
                        "✅ مرحله ۳: تراکنش پیدا شد\nمبلغ: ${pending.amount}",
                        Toast.LENGTH_LONG
                    ).show()
                }

                when (action) {
                    ACTION_CONFIRM -> {
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

                        val insertedId = database.transactionDao().insert(transaction)
                        repository.markAsConfirmed(smsId)

                        // ✅ Toast #4: موفقیت نهایی
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                appContext,
                                "🎉 ثبت شد! شناسه: $insertedId",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        android.util.Log.d("SmsActionReceiver", "Transaction inserted id=$insertedId")
                    }
                    ACTION_REJECT -> {
                        repository.markAsRejected(smsId)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(appContext, "🗑️ رد شد", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("SmsActionReceiver", "Error in coroutine", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        appContext,
                        "❌ خطا: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}