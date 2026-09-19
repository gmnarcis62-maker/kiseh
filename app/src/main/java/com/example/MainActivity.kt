package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
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

    // ⭐ Launcher خرید (روش جدید اندروید ۱۴)
    private val purchaseLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        android.util.Log.d("BillingManager", "Purchase result: resultCode=${result.resultCode}")

        val data = result.data
        if (result.resultCode == RESULT_OK && data != null) {
            handlePurchaseSuccess(data)
        } else {
            android.widget.Toast.makeText(this, "پرداخت لغو شد.", android.widget.Toast.LENGTH_SHORT).show()
        }

        try {
            BillingManager.checkPurchases(this)
        } catch (_: Throwable) {}
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        try {
            QuickVoiceNotificationHelper.showQuickVoiceNotification(this)
        } catch (_: Throwable) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // ⭐ اتصال Launcher به BillingManager قبل از init
        BillingManager.attachPurchaseLauncher(purchaseLauncher)

        try {
            BillingManager.init(this)
        } catch (_: Throwable) {}

        try {
            requestRequiredPermissions()
        } catch (_: Throwable) {}

        startBackgroundService()

        try {
            QuickVoiceNotificationHelper.showQuickVoiceNotification(this)
        } catch (_: Throwable) {}

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
                        SplashScreen(onSplashFinished = { showSplash = false })
                    } else if (isAppLockEnabled && isAppCurrentlyLocked) {
                        PinLockScreen(viewModel = viewModel, onUnlocked = { viewModel.unlockApp() })
                    } else {
                        KisehDashboardScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }

    /**
     * ⭐ پردازش نتیجه خرید از Launcher
     */
    private fun handlePurchaseSuccess(data: Intent) {
        val responseCode = data.getIntExtra("RESPONSE_CODE", 0)
        val purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA")
        val dataSignature = data.getStringExtra("INAPP_DATA_SIGNATURE")

        android.util.Log.d("BillingManager", "responseCode=$responseCode, hasData=${!purchaseData.isNullOrEmpty()}")

        if (responseCode == 0 && !purchaseData.isNullOrEmpty()) {
            try {
                val json = org.json.JSONObject(purchaseData)
                val productId = json.optString("productId")
                val purchaseState = json.optInt("purchaseState", -1)
                val token = json.optString("purchaseToken")
                val time = json.optLong("purchaseTime", System.currentTimeMillis())

                val isVerified = if (!dataSignature.isNullOrBlank() && BillingManager.MYKET_PUBLIC_KEY.isNotBlank()) {
                    BillingManager.verifyPurchase(purchaseData, dataSignature, BillingManager.MYKET_PUBLIC_KEY)
                } else true

                if (!isVerified) {
                    android.widget.Toast.makeText(this, "اعتبارسنجی امضا ناموفق بود.", android.widget.Toast.LENGTH_LONG).show()
                    return
                }

                if (productId !in BillingManager.ALL_VIP_SKUS) {
                    android.widget.Toast.makeText(this, "محصول یافت نشد.", android.widget.Toast.LENGTH_LONG).show()
                    return
                }

                if (purchaseState == 0) {
                    BillingManager.setVipUser(this, true, token, time, productId)
                    android.widget.Toast.makeText(this, "🎉 خرید موفق! VIP فعال شد.", android.widget.Toast.LENGTH_LONG).show()
                    try { QuickVoiceNotificationHelper.showQuickVoiceNotification(this) } catch (_: Throwable) {}
                }
            } catch (e: Throwable) {
                android.util.Log.e("BillingManager", "Error parsing purchase", e)
                android.widget.Toast.makeText(this, "خطا در پردازش خرید.", android.widget.Toast.LENGTH_SHORT).show()
            }
        } else if (responseCode == 7) {
            BillingManager.setVipUser(this, true, null, System.currentTimeMillis(), BillingManager.SKU_PRO_LIFETIME)
            android.widget.Toast.makeText(this, "🎉 قبلاً خریداری شده بود.", android.widget.Toast.LENGTH_LONG).show()
        } else if (responseCode != 0) {
            val errorMsg = BillingManager.getBillingErrorMessage(responseCode)
            android.widget.Toast.makeText(this, errorMsg, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        try { BillingManager.checkPurchases(this) } catch (_: Throwable) {}
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        try { BillingManager.checkPurchases(this) } catch (_: Throwable) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        BillingManager.release()
    }

    private fun startBackgroundService() {
        val intent = Intent(this, SmsBackgroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

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
            } catch (_: Throwable) {}
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
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true
    }
}