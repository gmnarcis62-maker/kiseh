package com.example.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.R
import com.example.billing.BillingManager
import com.example.voice.QuickVoiceActivity

object QuickVoiceNotificationHelper {

    private const val CHANNEL_ID = "quick_voice_channel"
    private const val NOTIFICATION_ID = 2002

    fun showQuickVoiceNotification(context: Context) {
        try {
            // وقتی گزینه ویجت خاموش است یا کاربر پرو VIP نیست: هیچ Notification Widget نمایش داده نشود
            if (!BillingManager.isProUser(context) || !BillingManager.isQuickVoiceNotificationEnabled(context)) {
                cancelQuickVoiceNotification(context)
                return
            }

            createNotificationChannel(context)

            val intent = Intent(context, QuickVoiceActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // ایجاد ساختار RemoteViews با چیدمان کاملاً راست‌به‌چپ (RTL) سازگار با اندروید ۱۲ تا ۱۵
            val remoteViews = RemoteViews(context.packageName, R.layout.widget_quick_voice_notification).apply {
                setOnClickPendingIntent(R.id.notification_root, pendingIntent)
                setOnClickPendingIntent(R.id.btn_record_action, pendingIntent)
                setOnClickPendingIntent(R.id.iv_mic_icon, pendingIntent)
                // اعمال layoutDirection = View.LAYOUT_DIRECTION_RTL
                setInt(R.id.notification_root, "setLayoutDirection", View.LAYOUT_DIRECTION_RTL)
                setInt(R.id.layout_text_container, "setLayoutDirection", View.LAYOUT_DIRECTION_RTL)
            }

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_mic_notification)
                .setCustomContentView(remoteViews)
                .setCustomBigContentView(remoteViews)
                .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                .setContentTitle("کیسه - دستیار ثبت سریع صوتی 🎙️")
                .setContentText("برای ثبت سریع خرج یا درآمد، روی آیکون میکروفون بزنید")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .build()

            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
            ) {
                NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            }
        } catch (t: Throwable) {
            // Ignore any notification failure on startup
        }
    }

    fun cancelQuickVoiceNotification(context: Context) {
        try {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "ثبت سریع صوتی کیسه"
            val descriptionText = "اعلان میانبر نوار اعلانات برای ثبت صوتی بدون باز کردن برنامه"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
