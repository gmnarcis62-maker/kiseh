package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.billing.BillingManager
import com.example.notification.QuickVoiceNotificationHelper
import com.example.sms.SmsBackgroundService
import com.example.ui.screens.KisehDashboardScreen
import com.example.ui.screens.PinLockScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.KisehTheme
import com.example.viewmodel.MainViewModel

class MainActivity : FragmentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val requiredPermissions by lazy {
        mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // بعد از دریافت مجوزها، اعلان را به‌روزرسانی کن
        try {
            QuickVoiceNotificationHelper.showQuickVoiceNotification(this)
        } catch (_: Throwable) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        try {
            BillingManager.init(this)
        } catch (_: Throwable) {}

        try {
            requestRequiredPermissions()
        } catch (_: Throwable) {}

        // شروع سرویس پس‌زمینه برای زنده نگه داشتن برنامه جهت دریافت پیامک و ویجت
        startBackgroundService()

        // به‌روزرسانی اعلان سرویس پس‌زمینه بر اساس وضعیت ثبت صوتی (VIP)
        try {
            QuickVoiceNotificationHelper.showQuickVoiceNotification(this)
        } catch (_: Throwable) {}

        // درخواست معافیت از بهینه‌سازی باتری برای پایدار ماندن سرویس
        requestIgnoreBatteryOptimization()

        setContent {
            KisehTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var showSplash by remember { mutableStateOf(true) }
                    val isAppCurrentlyLocked by viewModel.isAppCurrentlyLocked.collectAsState()
                    val isAppLockEnabled by viewModel.isAppLockEnabled.collectAsState()

                    if (showSplash) {
                        SplashScreen(
                            onSplashFinished = { showSplash = false }
                        )
                    } else if (isAppLockEnabled && isAppCurrentlyLocked) {
                        PinLockScreen(
                            viewModel = viewModel,
                            onUnlocked = { viewModel.unlockApp() }
                        )
                    } else {
                        KisehDashboardScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            com.example.billing.BillingManager.checkPurchases(this)
        } catch (_: Throwable) {}
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        try {
            com.example.billing.BillingManager.checkPurchases(this)
        } catch (_: Throwable) {}
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == com.example.billing.BillingManager.PURCHASE_REQUEST_CODE || requestCode == 10001) {
            android.util.Log.d("BillingManager", "Purchase Result: requestCode=$requestCode, resultCode=$resultCode")

            if (resultCode == RESULT_OK && data != null) {
                val responseCode = data.getIntExtra("RESPONSE_CODE", 0)
                val purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA")
                val dataSignature = data.getStringExtra("INAPP_DATA_SIGNATURE")
                android.util.Log.d("BillingManager", "Purchase Result: responseCode=$responseCode, hasPurchaseData=${!purchaseData.isNullOrEmpty()}, hasSignature=${!dataSignature.isNullOrEmpty()}")

                if ((responseCode == 0) && !purchaseData.isNullOrEmpty()) {
                    try {
                        val json = org.json.JSONObject(purchaseData)
                        val productId = json.optString("productId")
                        val purchaseState = json.optInt("purchaseState", -1)
                        val token = json.optString("purchaseToken")
                        val time = json.optLong("purchaseTime", System.currentTimeMillis())

                        android.util.Log.d("BillingManager", "Purchase Token: $token")
                        android.util.Log.d("BillingManager", "Purchase State: $purchaseState")
                        android.util.Log.d("BillingManager", "Verify Started: productId=$productId, signature=$dataSignature")

                        val isVerified = if (!dataSignature.isNullOrBlank() && com.example.billing.BillingManager.BAZAAR_PUBLIC_KEY.isNotBlank()) {
                            com.example.billing.BillingManager.verifyPurchase(purchaseData, dataSignature, com.example.billing.BillingManager.BAZAAR_PUBLIC_KEY)
                        } else {
                            true
                        }

                        android.util.Log.d("BillingManager", "Verify Result: $isVerified")

                        if (!isVerified) {
                            android.widget.Toast.makeText(this, "اعتبارسنجی امضای دیجیتال خرید ناموفق بود. تراکنش نامعتبر است.", android.widget.Toast.LENGTH_LONG).show()
                            return
                        }

                        if (productId !in com.example.billing.BillingManager.ALL_VIP_SKUS) {
                            android.widget.Toast.makeText(this, "محصول خریداری شده ($productId) در لیست VIP برنامه یافت نشد.", android.widget.Toast.LENGTH_LONG).show()
                            return
                        }

                        if (purchaseState == 0) {
                            com.example.billing.BillingManager.setVipUser(this, true, token, time, productId)
                            android.util.Log.d("BillingManager", "VIP Activated: token=$token, time=$time, product=$productId")
                            android.widget.Toast.makeText(this, "🎉 خرید با موفقیت تایید شد! دسترسی دائم VIP فعال گردید.", android.widget.Toast.LENGTH_LONG).show()
                            // بعد از فعال شدن VIP، اعلان سرویس را به‌روزرسانی کن
                            try {
                                QuickVoiceNotificationHelper.showQuickVoiceNotification(this)
                            } catch (_: Throwable) {}
                            return
                        } else {
                            android.widget.Toast.makeText(this, "وضعیت خرید معتبر نیست (کد وضعیت: $purchaseState).", android.widget.Toast.LENGTH_SHORT).show()
                            return
                        }
                    } catch (e: Throwable) {
                        android.util.Log.e("BillingManager", "Error parsing onActivityResult purchaseData", e)
                        android.widget.Toast.makeText(this, "خطا در پردازش اطلاعات خرید دریافتی از کافهبازار.", android.widget.Toast.LENGTH_SHORT).show()
                        return
                    }
                } else if (responseCode == 7) {
                    com.example.billing.BillingManager.setVipUser(this, true, null, System.currentTimeMillis(), com.example.billing.BillingManager.SKU_PRO_LIFETIME)
                    android.util.Log.d("BillingManager", "VIP Activated (Item already owned)")
                    android.widget.Toast.makeText(this, "🎉 این اشتراک قبلاً خریداری شده است و دسترسی VIP فعال گردید.", android.widget.Toast.LENGTH_LONG).show()
                    try {
                        QuickVoiceNotificationHelper.showQuickVoiceNotification(this)
                    } catch (_: Throwable) {}
                    return
                } else if (responseCode != 0) {
                    val errorMsg = com.example.billing.BillingManager.getBillingErrorMessage(responseCode)
                    android.widget.Toast.makeText(this, errorMsg, android.widget.Toast.LENGTH_SHORT).show()
                    return
                }
            } else if (resultCode == RESULT_CANCELED) {
                val responseCode = data?.getIntExtra("RESPONSE_CODE", 1) ?: 1
                val errorMsg = com.example.billing.BillingManager.getBillingErrorMessage(responseCode)
                android.widget.Toast.makeText(this, errorMsg, android.widget.Toast.LENGTH_SHORT).show()
                return
            }

            com.example.billing.BillingManager.checkPurchases(this)
            if (!com.example.billing.BillingManager.isProUser(this)) {
                android.widget.Toast.makeText(this, "پرداخت توسط کاربر لغو شد.", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        BillingManager.release()
    }

    private fun startBackgroundService() {
        // برای اندروید ۸ به بالا باید از startForegroundService استفاده کرد
        val intent = Intent(this, SmsBackgroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    /**
     * درخواست معافیت از بهینه‌سازی باتری برای پایدار ماندن سرویس پس‌زمینه.
     * این کار باعث می‌شود اندروید سرویس ما را در پس‌زمینه به قتل نرساند.
     */
    private fun requestIgnoreBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val pm = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
                if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                    val intent = Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = android.net.Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                }
            } catch (_: Throwable) {
                try {
                    startActivity(Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                } catch (_: Throwable) {}
            }
        }
    }

    private fun requestRequiredPermissions() {
        val permissionsToRequest = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}