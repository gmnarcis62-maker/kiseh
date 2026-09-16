package com.example.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * پس از روشن شدن گوشی، سرویس پس‌زمینه را دوباره راه‌اندازی می‌کند
 * تا حتی اگر کاربر برنامه را باز نکرده باشد، پیامک‌های بانکی ثبت شوند.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            try {
                val serviceIntent = Intent(context, SmsBackgroundService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
                android.util.Log.d("BootReceiver", "SmsBackgroundService started after boot")
            } catch (e: Exception) {
                android.util.Log.e("BootReceiver", "Failed to start service on boot", e)
            }
        }
    }
}