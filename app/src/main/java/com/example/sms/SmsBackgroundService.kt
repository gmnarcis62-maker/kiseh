package com.example.sms

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.example.notification.QuickVoiceNotificationHelper

class SmsBackgroundService : Service() {

    companion object {
        const val CHANNEL_ID = "KisehBackgroundChannel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP_SERVICE = "com.example.kiseh.STOP_SERVICE"
    }

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // شروع سرویس در حالت Foreground با اعلان واحد و مختصر
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
    }

    /**
     * اعلان واحد و مختصر:
     * - اگر کاربر VIP باشد: دکمه ثبت صوتی نمایش داده می‌شود
     * - در غیر این صورت: فقط یک خط متن ساده
     */
    private fun createNotification(): Notification {
        return if (QuickVoiceNotificationHelper.isQuickVoiceEnabled(this)) {
            QuickVoiceNotificationHelper.buildQuickVoiceForegroundNotification(this)
        } else {
            QuickVoiceNotificationHelper.buildSimpleForegroundNotification(this)
        }
    }
}