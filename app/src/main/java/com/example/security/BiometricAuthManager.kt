package com.example.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * مدیریت احراز هویت با اثر انگشت و بیومتریک سازگار با اندروید ۱۰ تا ۱۵
 */
object BiometricAuthManager {

    enum class BiometricStatus {
        AVAILABLE,
        NO_HARDWARE,
        HW_UNAVAILABLE,
        NONE_ENROLLED,
        UNKNOWN
    }

    /**
     * بررسی پشتیبانی دستگاه از حسگر بیومتریک
     */
    fun checkBiometricAvailability(context: Context): BiometricStatus {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BIOMETRIC_STRONG or BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.HW_UNAVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NONE_ENROLLED
            else -> BiometricStatus.UNKNOWN
        }
    }

    /**
     * آیا بیومتریک کاملاً آماده و فعال روی دستگاه است؟
     */
    fun isBiometricReady(context: Context): Boolean {
        return try {
            checkBiometricAvailability(context) == BiometricStatus.AVAILABLE
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * نمایش فرم استاندارد بیومتریک سیستم‌عامل اندروید به شکل کاملاً محافظت‌شده در برابر Crash
     */
    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String = "احراز هویت بیومتریک",
        subtitle: String = "برای ورود اثر انگشت خود را اسکن کنید",
        negativeButtonText: String = "استفاده از رمز عبور",
        onSuccess: () -> Unit,
        onError: (errorCode: Int, errorMessage: String) -> Unit,
        onFailed: () -> Unit = {}
    ) {
        // ۱. بررسی وضعیت دسترسی به سنسور قبل از هر اقدامی
        val status = try {
            checkBiometricAvailability(activity)
        } catch (t: Throwable) {
            onError(-1, "خطا در بررسی سنسور بیومتریک: ${t.localizedMessage ?: "نامشخص"}")
            return
        }

        when (status) {
            BiometricStatus.NO_HARDWARE -> {
                onError(BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE, "این دستگاه فاقد سنسور اثر انگشت یا بیومتریک است.")
                return
            }
            BiometricStatus.HW_UNAVAILABLE -> {
                onError(BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE, "حسگر اثر انگشت در حال حاضر در دسترس نیست.")
                return
            }
            BiometricStatus.NONE_ENROLLED -> {
                onError(BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED, "هیچ اثر انگشتی در تنظیمات دستگاه ثبت نشده است.")
                return
            }
            BiometricStatus.UNKNOWN -> {
                onError(-1, "احراز هویت بیومتریک در این دستگاه پشتیبانی نمی‌شود.")
                return
            }
            BiometricStatus.AVAILABLE -> {
                // آماده اجرا
            }
        }

        try {
            val executor = ContextCompat.getMainExecutor(activity)

            val callback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errorCode, errString.toString())
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onFailed()
                }
            }

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setNegativeButtonText(negativeButtonText)
                .setAllowedAuthenticators(BIOMETRIC_STRONG or BIOMETRIC_WEAK)
                .build()

            val biometricPrompt = BiometricPrompt(activity, executor, callback)
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Throwable) {
            // جلوگیری قطعی از هرگونه کرش برنامه در شبیه‌ساز یا ران‌تایم
            onError(-1, "عدم امکان اجرای سنسور در این محیط: ${e.message ?: "نامشخص"}")
        }
    }
}
