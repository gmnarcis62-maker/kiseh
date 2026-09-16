package com.example.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.example.billing.BillingManager
import com.example.billing.FeatureUsageManager
import com.example.data.AppDatabase
import com.example.data.PendingBankSms
import com.example.data.PendingBankSmsRepository
import com.example.data.Transaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * دریافت امن، فیلتر دقیق و ثبت هوشمند پیامک‌های ورودی بانکی سیستم‌عامل اندروید
 */
class BankSmsReceiver : BroadcastReceiver() {

    private val parser = BankSmsParser()

    override fun onReceive(context: Context, intent: Intent?) {
        android.util.Log.d("BANK_SMS_DEBUG", "SMS_RECEIVED_REACHED")

        if (intent == null) {
            android.util.Log.d("BANK_SMS_DEBUG", "RECEIVER_FINISHED - Intent is null")
            return
        }

        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION &&
            intent.action != "android.provider.Telephony.SMS_RECEIVED"
        ) {
            android.util.Log.d("BANK_SMS_DEBUG", "RECEIVER_FINISHED - Unhandled action: ${intent.action}")
            return
        }

        // ۱. بررسی دسترسی کاربر ویژه (VIP) یا سهمیه آزمایشی کاربر رایگان (Free Trial: 3 پیامک بانکی)
        if (!FeatureUsageManager.canProcessBankSms(context)) {
            android.util.Log.d("BANK_SMS_DEBUG", "RECEIVER_FINISHED - Quota limit reached")
            return
        }

        // ۲. بررسی مجوز پیامک جهت اطمینان و جلوگیری از هرگونه کرش یا رفتار ناخواسته
        if (!SmsPermissionManager.hasSmsPermission(context)) {
            android.util.Log.d("BANK_SMS_DEBUG", "RECEIVER_FINISHED - Missing SMS Permission")
            return
        }

        // ۳. بررسی فعال بودن قابلیت در ترجیحات کاربر (DataStore)
        val prefsManager = BankSmsPreferencesManager.getInstance(context)
        if (!prefsManager.isBankSmsEnabled()) {
            android.util.Log.d("BANK_SMS_DEBUG", "RECEIVER_FINISHED - Bank SMS feature disabled")
            return
        }

        val messages = try {
            Telephony.Sms.Intents.getMessagesFromIntent(intent)
        } catch (e: Exception) {
            null
        }

        if (messages.isNullOrEmpty()) {
            android.util.Log.d("BANK_SMS_DEBUG", "RECEIVER_FINISHED - No messages in intent")
            return
        }

        val sender = messages[0].originatingAddress ?: ""
        val fullBodyBuilder = StringBuilder()
        for (msg in messages) {
            msg.displayMessageBody?.let { fullBodyBuilder.append(it) }
        }
        val fullBody = fullBodyBuilder.toString()

        if (fullBody.isBlank()) {
            android.util.Log.d("BANK_SMS_DEBUG", "RECEIVER_FINISHED - Blank SMS body")
            return
        }

        android.util.Log.d("BANK_SMS_DEBUG", "SMS_RECEIVED_REACHED\nsender=$sender\nbody=$fullBody")

        // دریافت دسته‌بندی یاد گرفته شده کاربر در صورت فعال بودن
        val learner = UserCategoryLearner.getInstance(context)
        val userLearnedCategory = if (prefsManager.isUserLearningEnabled()) {
            learner.getLearnedCategory(fullBody)
        } else null

        // تحلیل پیامک با موتور هوشمند BankSmsParser
        android.util.Log.d("BANK_SMS_DEBUG", "PARSER_STARTED")
        val parsed = parser.parse(fullBody, sender, userLearnedCategory, context)
        if (parsed == null) {
            android.util.Log.d("BANK_SMS_DEBUG", "RECEIVER_FINISHED - Parser returned null")
            return
        }

        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (!FeatureUsageManager.canProcessBankSms(appContext)) {
                    android.util.Log.d("BANK_SMS_DEBUG", "RECEIVER_FINISHED - Quota limit reached inside async")
                    return@launch
                }
                val database = AppDatabase.getDatabase(appContext)
                val repository = PendingBankSmsRepository(database.pendingBankSmsDao())

                val merchant = parsed.description.substringAfter("خرید: ").substringAfter("واریز: ").substringBefore(" (")
                val categoryToUse = if (prefsManager.isSmartCategoryEnabled()) {
                    if (prefsManager.isUserLearningEnabled()) {
                        learner.getLearnedCategory(merchant) ?: parsed.suggestedCategory
                    } else {
                        parsed.suggestedCategory
                    }
                } else {
                    "سایر"
                }

                // ۳. بررسی و جلوگیری از ثبت تراکنش تکراری (Duplicate Protection)
                val isDuplicate = BankSmsDuplicateDetector.isDuplicate(
                    context = appContext,
                    bankName = parsed.bank.displayName,
                    amount = parsed.amount,
                    isIncome = parsed.isIncome,
                    cardLastDigits = parsed.cardNumber,
                    merchant = merchant,
                    timestamp = parsed.timestamp
                )

                if (isDuplicate) {
                    android.util.Log.d("BANK_SMS_DEBUG", "PENDING_INSERT_RESULT=skipped_duplicate")
                    return@launch
                }

                // ثبت در کش تکراری
                BankSmsDuplicateDetector.recordProcessed(
                    bankName = parsed.bank.displayName,
                    amount = parsed.amount,
                    isIncome = parsed.isIncome,
                    cardLastDigits = parsed.cardNumber,
                    merchant = merchant,
                    timestamp = parsed.timestamp
                )

                // ۴. تصمیم‌گیری بر اساس حالت Auto-Confirm یا تأیید قبل از ثبت
                val isAutoConfirm = prefsManager.isAutoConfirmEnabled()
                val isHighConfidence = parsed.confidence >= 0.85f

                if (isAutoConfirm && isHighConfidence) {
                    // ثبت خودکار مستقیم به عنوان تراکنش قطعی در برنامه
                    val canonicalCategory = SmartCategoryMatcher.toCanonicalKisehCategory(categoryToUse)
                    val description = buildString {
                        append(parsed.bank.displayName)
                        if (merchant.isNotBlank()) {
                            append(" - ")
                            append(merchant)
                        }
                        if (!parsed.cardNumber.isNullOrBlank()) {
                            append(" (کارت *")
                            append(parsed.cardNumber)
                            append(")")
                        }
                    }

                    val transaction = Transaction(
                        amount = parsed.amount,
                        category = canonicalCategory,
                        description = description,
                        isIncome = parsed.isIncome,
                        date = parsed.timestamp
                    )
                    database.transactionDao().insert(transaction)
                    android.util.Log.d("BANK_SMS_DEBUG", "PENDING_INSERT_RESULT=auto_confirmed_transaction")

                    if (merchant.isNotBlank() && prefsManager.isUserLearningEnabled()) {
                        learner.learn(merchant, categoryToUse)
                    }

                    if (prefsManager.isNotificationEnabled()) {
                        SmsNotificationHelper.showAutoConfirmedNotification(
                            context = appContext,
                            bankName = parsed.bank.displayName,
                            amount = parsed.amount,
                            isIncome = parsed.isIncome,
                            merchant = merchant,
                            suggestedCategory = canonicalCategory
                        )
                    }
                } else {
                    // درج در صف معلق (Pending) جهت بازبینی و تایید کاربر
                    val pendingSms = PendingBankSms(
                        bankName = parsed.bank.displayName,
                        amount = parsed.amount,
                        isIncome = parsed.isIncome,
                        balance = parsed.balance,
                        cardLastDigits = parsed.cardNumber,
                        merchant = merchant,
                        suggestedCategory = categoryToUse,
                        createdAt = parsed.timestamp
                    )

                    val insertedId = repository.insert(pendingSms)
                    android.util.Log.d("BANK_SMS_DEBUG", "PENDING_INSERT_RESULT=success (id=$insertedId)")

                    if (insertedId > 0L && prefsManager.isNotificationEnabled()) {
                        val savedItem = pendingSms.copy(id = insertedId)
                        val enableQuickActions = prefsManager.isQuickActionEnabled()
                        SmsNotificationHelper.showPendingSmsNotification(
                            context = appContext,
                            sms = savedItem,
                            enableQuickActions = enableQuickActions
                        )
                    }
                }

                // افزایش سهمیه پردازش پیامک
                FeatureUsageManager.incrementBankSmsUsage(appContext)

                // ۵. پاکسازی دوره‌ای خودکار پیامک‌های قدیمی پردازش‌شده
                if (prefsManager.isAutoCleanupEnabled()) {
                    val thirtyDaysAgo = System.currentTimeMillis() - 30L * 24 * 3600 * 1000L
                    repository.cleanupOldProcessedSms(thirtyDaysAgo)
                }

            } catch (e: Exception) {
                android.util.Log.e("BANK_SMS_DEBUG", "Error processing bank SMS broadcast", e)
            } finally {
                android.util.Log.d("BANK_SMS_DEBUG", "RECEIVER_FINISHED")
                pendingResult.finish()
            }
        }
    }
}
