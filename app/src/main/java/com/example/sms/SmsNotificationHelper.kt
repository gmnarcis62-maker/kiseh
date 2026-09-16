package com.example.sms

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.PendingBankSms
import java.text.NumberFormat
import java.util.Locale

/**
 * مدیریت ایجاد، گروه‌بندی و نمایش امن اعلان‌های تعاملی پیامک‌های بانکی جدید
 */
object SmsNotificationHelper {

    private const val CHANNEL_ID = "kiseh_bank_sms_channel"
    private const val CHANNEL_NAME = "پیامک‌های بانکی کیسه"
    private const val CHANNEL_DESC = "اطلاع‌رسانی خودکار تراکنش‌های شناسایی‌شده از پیامک‌های بانکی"

    const val GROUP_KEY_BANK_SMS = "com.example.sms.BANK_TRANSACTIONS"
    private const val SUMMARY_NOTIFICATION_ID = 999901

    /**
     * ثبت کانال نوتیفیکیشن
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    /**
     * نمایش نوتیفیکیشن تعاملی برای پیامک جدید با حفظ کامل حریم خصوصی و گروه‌بندی
     */
    fun showPendingSmsNotification(
        context: Context,
        sms: PendingBankSms,
        enableQuickActions: Boolean = true
    ) {
        createNotificationChannel(context)

        val notificationId = if (sms.id > 0L) {
            (sms.id and 0x7FFFFFFFL).toInt()
        } else {
            (System.currentTimeMillis() % 100000).toInt()
        }

        // اکشن کلیک روی بدنه نوتیفیکیشن (باز شدن برنامه)
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formattedAmount = formatCurrency(sms.amount)
        val typeTitle = if (sms.isIncome) "واریز وجه" else "برداشت / خرید"
        val merchantInfo = if (sms.merchant.isNotBlank()) "\nپذیرنده: ${sms.merchant}" else ""
        val cardInfo = if (!sms.cardLastDigits.isNullOrBlank()) "\nکارت: *${sms.cardLastDigits}" else ""
        val categoryInfo = "\nدسته پیشنهادی: ${sms.suggestedCategory}"

        val contentText = "$typeTitle: $formattedAmount تومان (${sms.bankName})"
        val bigText = "$contentText$merchantInfo$cardInfo$categoryInfo"

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("تراکنش بانکی جدید شناسایی شد")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .setGroup(GROUP_KEY_BANK_SMS)

        if (enableQuickActions) {
            // اکشن دکمه ۱: تایید و ثبت
            val confirmIntent = Intent(context, SmsActionReceiver::class.java).apply {
                action = SmsActionReceiver.ACTION_CONFIRM
                putExtra(SmsActionReceiver.EXTRA_SMS_ID, sms.id)
                putExtra(SmsActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            }
            val confirmPendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId * 10 + 1,
                confirmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // اکشن دکمه ۲: رد کردن / نادیده گرفتن
            val rejectIntent = Intent(context, SmsActionReceiver::class.java).apply {
                action = SmsActionReceiver.ACTION_REJECT
                putExtra(SmsActionReceiver.EXTRA_SMS_ID, sms.id)
                putExtra(SmsActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            }
            val rejectPendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId * 10 + 2,
                rejectIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val actionLabel = if (sms.isIncome) "ثبت درآمد" else "ثبت هزینه"
            builder.addAction(0, actionLabel, confirmPendingIntent)
            builder.addAction(0, "نادیده گرفتن", rejectPendingIntent)
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(notificationId, builder.build())
        try {
            android.util.Log.d("BANK_SMS_DEBUG", "NOTIFICATION_RESULT=success (id=$notificationId)")
        } catch (_: Throwable) {}
    }

    /**
     * نمایش اعلان برای تراکنش‌هایی که به صورت خودکار و با اطمینان بالا ثبت شدند
     */
    fun showAutoConfirmedNotification(
        context: Context,
        bankName: String,
        amount: Long,
        isIncome: Boolean,
        merchant: String,
        suggestedCategory: String
    ) {
        createNotificationChannel(context)
        val notificationId = (System.currentTimeMillis() % 100000).toInt()

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formattedAmount = formatCurrency(amount)
        val typeTitle = if (isIncome) "واریز وجه" else "خرید / برداشت"
        val contentText = "$typeTitle: $formattedAmount تومان با موفقیت ثبت شد"
        val merchantText = if (merchant.isNotBlank()) "پذیرنده: $merchant | " else ""
        val subText = "${merchantText}دسته: $suggestedCategory ($bankName)"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("تراکنش بانکی به صورت هوشمند ثبت شد")
            .setContentText(contentText)
            .setSubText(bankName)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$contentText\n$subText"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .setGroup(GROUP_KEY_BANK_SMS)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(notificationId, notification)

        showGroupSummaryNotification(context)
    }

    /**
     * ایجاد نوتیفیکیشن خلاصه (Summary) برای گروه‌بندی اعلانات پیامک در نوار وضعیت
     */
    private fun showGroupSummaryNotification(context: Context) {
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            SUMMARY_NOTIFICATION_ID,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val summaryNotification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("پیامک‌های بانکی کیسه")
            .setContentText("تراکنش‌های شناسایی‌شده از پیامک‌های بانکی")
            .setGroup(GROUP_KEY_BANK_SMS)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(SUMMARY_NOTIFICATION_ID, summaryNotification)
    }

    /**
     * بستن اعلان مشخص
     */
    fun cancelNotification(context: Context, notificationId: Int) {
        if (notificationId != -1) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.cancel(notificationId)
        }
    }

    private fun formatCurrency(amount: Long): String {
        return try {
            NumberFormat.getNumberInstance(Locale.US).format(amount)
        } catch (e: Exception) {
            amount.toString()
        }
    }
}
