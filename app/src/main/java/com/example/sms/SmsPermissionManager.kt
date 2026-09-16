package com.example.sms

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * وضعیت‌های مجوز دریافت پیامک
 */
enum class SmsPermissionState {
    GRANTED,               // مجوز داده شده است
    DENIED_CAN_ASK_AGAIN,  // مجوز رد شده اما می‌توان دوباره درخواست داد
    PERMANENTLY_DENIED     // مجوز دائماً رد شده و نیازمند ورود به تنظیمات است
}

/**
 * مدیریت حرفه‌ای و امن مجوز دریافت پیامک بانکی (RECEIVE_SMS)
 *
 * بدون درخواست هیچ‌گونه مجوز اضافی نظیر READ_SMS یا SEND_SMS
 */
object SmsPermissionManager {

    const val SMS_PERMISSION = Manifest.permission.RECEIVE_SMS

    /**
     * بررسی سریع اعطای مجوز دریافت پیامک
     */
    fun hasSmsPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            SMS_PERMISSION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * تشخیص دقیق وضعیت ۳گانه مجوز در Activity
     */
    fun getPermissionState(activity: Activity): SmsPermissionState {
        return when {
            hasSmsPermission(activity) -> SmsPermissionState.GRANTED
            ActivityCompat.shouldShowRequestPermissionRationale(activity, SMS_PERMISSION) -> {
                SmsPermissionState.DENIED_CAN_ASK_AGAIN
            }
            else -> {
                // اگر قبلاً درخواست داده شده و کاربر "Don't ask again" را زده باشد
                SmsPermissionState.PERMANENTLY_DENIED
            }
        }
    }

    /**
     * هدایت امن کاربر به صفحه تنظیمات برنامه در سیستم‌عامل
     */
    fun openAppSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // جلوگیری از کرش در صورت عدم پشتیبانی رام خاص
        }
    }
}
