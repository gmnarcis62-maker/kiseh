package com.example.notification

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.R
import com.example.billing.BillingManager
import com.example.voice.QuickVoiceActivity

/**
 * اعلان واحد، حرفه‌ای و جمع‌وجور برای کیسه.
 * یک اعلان باریک با کمترین فضای اشغال‌شده:
 *  - برای کاربر عادی: فقط یک خط متن "کیسه هوشمند - در حال دریافت پیامک"
 *  - برای کاربر VIP: همان خط + یک دکمه ثبت صوتی
 */
object QuickVoiceNotificationHelper {

    /** کانال و شناسه یکسان با SmsBackgroundService تا اعلان‌ها ادغام شوند */
    const val CHANNEL_ID = "KisehBackgroundChannel"
    const val NOTIFICATION_ID = 1001

    /**
     * اعلان پایه و بسیار مختصر (کاربر عادی)
     */
    fun buildSimpleForegroundNotification(context: Context): Notification {
        createNotificationChannel(context)
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_mic_notification)
            .setContentTitle("کیسه هوشمند")
            .setContentText("در حال دریافت پیامک بانکی")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .build()
    }

    /**
     * اعلان مختصر با دکمه ثبت صوتی (کاربر VIP)
     */
    fun buildQuickVoiceForegroundNotification(context: Context): Notification {
        createNotificationChannel(context)

        val voiceIntent = Intent(context, QuickVoiceActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val voicePendingIntent = PendingIntent.getActivity(
            context,
            0,
            voiceIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_mic_notification)
            .setContentTitle("کیسه هوشمند")
            .setContentText("فعال • برای ثبت صوتی ضربه بزنید")
            .setContentIntent(voicePendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .addAction(
                android.R.drawable.ic_btn_speak_now,
                "ثبت صوتی",
                voicePendingIntent
            )
            .build()
    }

    /**
     * بررسی واجد شرایط بودن کاربر برای ثبت صوتی سریع (VIP)
     */
    fun isQuickVoiceEnabled(context: Context): Boolean {
        return try {
            BillingManager.isProUser(context) && BillingManager.isQuickVoiceNotificationEnabled(context)
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * به‌روزرسانی اعلان سرویس پس‌زمینه بر اساس وضعیت VIP
     */
    fun showQuickVoiceNotification(context: Context) {
        try {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            ) {
                return
            }
            val notification = if (isQuickVoiceEnabled(context)) {
                buildQuickVoiceForegroundNotification(context)
            } else {
                buildSimpleForegroundNotification(context)
            }
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: Throwable) {}
    }

    fun cancelQuickVoiceNotification(context: Context) {
        try {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        } catch (_: Exception) {}
    }

    /**
     * کانال اعلان با کمترین اهمیت ممکن تا فضای کمتری اشغال کند.
     * IMPORTANCE_MIN باعث می‌شود اعلان در بالای صفحه نمایش داده نشود و
     * فقط به صورت یک آیکون کوچک در نوار وضعیت باقی بماند.
     */
    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "کیسه هوشمند"
            val descriptionText = "اعلان پایدار کیسه (دریافت پیامک بانکی + ثبت صوتی سریع)"
            val importance = NotificationManager.IMPORTANCE_MIN
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
                setSound(null, null)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}