package com.example.sms

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

class SmsBackgroundService : Service() {

    companion object {
        const val CHANNEL_ID = "KisehBackgroundChannel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP_SERVICE = "com.example.kiseh.STOP_SERVICE"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // شروع سرویس در حالت Foreground برای جلوگیری از کشته شدن توسط سیستم
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        // اینجا می‌توانیم منطق اضافی برای چک کردن وضعیت را قرار دهیم
        // اما عمدتا وجود این سرویس برای زنده نگه داشتن پروسه کافی است
        
        return START_STICKY // اگر سرویس کشته شد، سیستم سعی می‌کند دوباره آن را بسازد
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "سرویس هوشمند کیسه"
            val descriptionText = "برای ثبت خودکار پیامک‌ها و ویجت در پس‌زمینه فعال است"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                showBadge = false
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        // اینتنت برای باز کردن برنامه وقتی روی نوتیفیکیشن کلیک می‌شود
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // اینتنت برای بستن سرویس (اختیاری)
        val stopIntent = Intent(ACTION_STOP_SERVICE).apply {
            setPackage(packageName)
        }
        val stopPendingIntent: PendingIntent = PendingIntent.getBroadcast(
            this, 0, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("کیسه - هوشمند")
            .setContentText("سیستم ثبت خودکار پیامک و ویجت فعال است.")
            .setSmallIcon(R.drawable.ic_mic_notification) // آیکون میکروفون برای نوتیفیکیشن
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true) // بی صدا باشد
            .addAction(android.R.drawable.ic_media_pause, "توقف", stopPendingIntent)
            .build()
    }
}
