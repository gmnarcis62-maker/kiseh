package com.example.billing

import android.app.Activity
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import com.android.vending.billing.IInAppBillingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * مدل نگهداری وضعیت سهمیه صوتی رایگان روزانه
 */
data class FreeVoiceUsage(
    val lastResetDate: String,
    val countToday: Int
)

/**
 * مدیریت پرداخت درون‌برنامه‌ای کافهبازار (Bazaar In-App Billing) و فعال‌سازی اشتراک دائم VIP.
 */
object BillingManager {

    private const val TAG = "BillingManager"

    const val BAZAAR_PUBLIC_KEY = "MIHNMA0GCSqGSIb3DQEBAQUAA4G7ADCBtwKBrwCivzZfp5mTKGMtNtJVPF1Mobx5Y6vOJeEZzKo0EunleE8YCkFb1c7Mg3qP+ud7VbYrjdXp/Kh0/KNVNn2NdAfeyjnvAN+b3K4ZDznX9/mWeiLEFUvPO8WfHPNX499Y1WEEUeKqgZNtywBWbaJjU83tSlnx7wjZTTBcxsW4cIoU/yZH7pfVmxrDCG8RvUEMOK9UbWTr9cl9+EBU4rkfxkRm04jJe4ZtmHQrMfPjkrMCAwEAAQ=="

    const val BAZAAR_PACKAGE = "com.farsitel.bazaar"
    const val BAZAAR_BILLING_ACTION = "ir.cafebazaar.pardakht.InAppBillingService.BIND"

    const val SKU_PRO_LIFETIME = "kiseh_pro_lifetime"
    const val SKU_VIP_LIFETIME = "kiseh_pro_lifetime"
    val ALL_VIP_SKUS = listOf(SKU_PRO_LIFETIME)

    const val PURCHASE_REQUEST_CODE = 10001
    const val STORE_BAZAAR = "bazaar"
    private const val PREFS_NAME = "kiseh_billing_prefs"
    private const val KEY_IS_PRO = "is_pro_user"
    private const val KEY_PURCHASE_STORE = "purchase_store"
    private const val KEY_PURCHASE_TOKEN = "purchase_token"
    private const val KEY_PURCHASE_DATE = "purchase_date"
    private const val KEY_PRODUCT_ID = "product_id"
    private const val KEY_VOICE_USAGE_COUNT = "voice_usage_count"
    private const val KEY_VOICE_USAGE_DATE = "voice_usage_date"
    private const val KEY_QUICK_VOICE_ENABLED = "quick_voice_notification_enabled"
    const val MAX_FREE_VOICE_USAGE = 5

    private val _isProState = MutableStateFlow(false)
    val isProState: StateFlow<Boolean> = _isProState.asStateFlow()
    val isVipState: StateFlow<Boolean> = _isProState.asStateFlow()

    private val _skuPriceState = MutableStateFlow<String?>(null)
    val skuPriceState: StateFlow<String?> = _skuPriceState.asStateFlow()
    val vipProductPrice: StateFlow<String?> = _skuPriceState.asStateFlow()

    @Volatile
    private var mService: IInAppBillingService? = null
    @Volatile
    private var isBound = false
    private var appContext: Context? = null
    private var connectedStorePackage: String? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            try {
                mService = IInAppBillingService.Stub.asInterface(service)
                Log.d(TAG, "Billing Connect Result: true (Connected to Bazaar: ${name?.packageName})")
                appContext?.let { ctx ->
                    // تاخیر کوتاه تا سرویس کاملاً آماده شود
                    CoroutineScope(Dispatchers.IO).launch {
                        delay(800)
                        querySkuDetailsInternal(ctx, attempt = 1, maxAttempts = 5, onResult = { _, _ -> })
                    }
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Error in onServiceConnected", t)
                mService = null
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mService = null
            isBound = false
            connectedStorePackage = null
            Log.d(TAG, "Billing Disconnected")
        }
    }

    fun init(context: Context) {
        try {
            val applicationContext = context.applicationContext
            appContext = applicationContext
            val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

            val store = prefs.getString(KEY_PURCHASE_STORE, null)
            val isPro = prefs.getBoolean(KEY_IS_PRO, false)

            if (isPro && (store == "bazaar" || (store != null && store != STORE_BAZAAR))) {
                prefs.edit()
                    .putBoolean(KEY_IS_PRO, false)
                    .remove(KEY_PURCHASE_STORE)
                    .remove(KEY_PURCHASE_TOKEN)
                    .remove(KEY_PURCHASE_DATE)
                    .remove(KEY_PRODUCT_ID)
                    .apply()
                _isProState.value = false
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        VipPreferencesManager(applicationContext).clearVip()
                    } catch (_: Throwable) {}
                }
            } else {
                _isProState.value = isPro
            }

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val vipInfo = VipPreferencesManager(applicationContext).getVipInfo()
                    if (vipInfo.isVip) {
                        _isProState.value = true
                        prefs.edit().putBoolean(KEY_IS_PRO, true).putString(KEY_PURCHASE_STORE, STORE_BAZAAR).apply()
                    }
                } catch (_: Throwable) {}
            }

            if (mService == null && !isBound) {
                Log.d(TAG, "Billing Connect Start: pkg=$BAZAAR_PACKAGE")
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val pm = applicationContext.packageManager
                        val bazaarIntent = Intent(BAZAAR_BILLING_ACTION).apply {
                            setPackage(BAZAAR_PACKAGE)
                        }
                        if (pm.queryIntentServices(bazaarIntent, 0).isNotEmpty()) {
                            isBound = applicationContext.bindService(bazaarIntent, serviceConnection, Context.BIND_AUTO_CREATE)
                            connectedStorePackage = BAZAAR_PACKAGE
                            Log.d(TAG, "Billing Connect Result: bindService initiated=$isBound")
                        } else {
                            Log.e(TAG, "Billing Connect Result: false (Bazaar app not installed)")
                        }
                    } catch (t: Throwable) {
                        Log.e(TAG, "Billing Connect Result: false (error binding)", t)
                    }
                }
            } else if (mService != null) {
                querySkuDetails(applicationContext)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Error in BillingManager.init", t)
        }
    }

    /**
     * استعلام قیمت (با ۳ تلاش)
     */
    fun querySkuDetails(context: Context, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        val appCtx = context.applicationContext
        if (mService == null) {
            Log.w(TAG, "SKU Query: service null, returning cached")
            onResult(false, _skuPriceState.value)
            return
        }
        querySkuDetailsInternal(appCtx, attempt = 1, maxAttempts = 3, onResult = onResult)
    }

    /**
     * متد عمومی برای تلاش مجدد از سمت UI
     */
    fun retrySkuQuery(context: Context, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        val appCtx = context.applicationContext
        if (mService == null) {
            Log.d(TAG, "Retry: service not bound, calling init")
            init(appCtx)
            CoroutineScope(Dispatchers.Main).launch {
                delay(2000)
                querySkuDetails(appCtx, onResult)
            }
        } else {
            querySkuDetails(appCtx, onResult)
        }
    }

    private fun querySkuDetailsInternal(
        appCtx: Context,
        attempt: Int,
        maxAttempts: Int,
        onResult: (Boolean, String?) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val service = mService
                if (service == null) {
                    Log.w(TAG, "SKU Query internal: service null")
                    withContext(Dispatchers.Main) { onResult(false, _skuPriceState.value) }
                    return@launch
                }

                Log.d(TAG, "SKU Query Start (attempt $attempt/$maxAttempts): sku=$SKU_PRO_LIFETIME")
                val skuBundle = Bundle()
                skuBundle.putStringArrayList("ITEM_ID_LIST", arrayListOf(SKU_PRO_LIFETIME))

                val skuDetailsBundle = service.getSkuDetails(3, appCtx.packageName, "inapp", skuBundle)
                if (skuDetailsBundle == null) {
                    Log.e(TAG, "SKU Query: bundle null (attempt $attempt)")
                    if (attempt < maxAttempts) {
                        delay(2000L)
                        querySkuDetailsInternal(appCtx, attempt + 1, maxAttempts, onResult)
                    } else {
                        withContext(Dispatchers.Main) { onResult(false, null) }
                    }
                    return@launch
                }

                val responseCode = skuDetailsBundle.getInt("RESPONSE_CODE", -1)
                Log.d(TAG, "SKU Query Result: responseCode=$responseCode (attempt $attempt)")

                if (responseCode == 0) {
                    val detailsList = skuDetailsBundle.getStringArrayList("DETAILS_LIST")
                    if (!detailsList.isNullOrEmpty()) {
                        for (itemJson in detailsList) {
                            try {
                                val json = JSONObject(itemJson)
                                val sku = json.optString("productId")
                                if (sku == SKU_PRO_LIFETIME) {
                                    val rawPrice = json.optString("price")
                                    val title = json.optString("title")
                                    val formattedPrice = formatPriceDisplay(rawPrice)
                                    _skuPriceState.value = formattedPrice
                                    Log.d(TAG, "SKU SUCCESS: price=$formattedPrice, title=$title")
                                    withContext(Dispatchers.Main) { onResult(true, formattedPrice) }
                                    return@launch
                                }
                            } catch (e: Throwable) {
                                Log.e(TAG, "Error parsing SKU JSON", e)
                            }
                        }
                        Log.w(TAG, "SKU: details list present but no match for $SKU_PRO_LIFETIME")
                    } else {
                        Log.w(TAG, "SKU: responseCode=0 but DETAILS_LIST empty")
                    }
                }

                if (attempt < maxAttempts) {
                    delay(2000L)
                    querySkuDetailsInternal(appCtx, attempt + 1, maxAttempts, onResult)
                } else {
                    withContext(Dispatchers.Main) { onResult(false, _skuPriceState.value) }
                }
            } catch (t: Throwable) {
                Log.e(TAG, "SKU Query error (attempt $attempt)", t)
                if (attempt < maxAttempts) {
                    delay(2000L)
                    querySkuDetailsInternal(appCtx, attempt + 1, maxAttempts, onResult)
                } else {
                    withContext(Dispatchers.Main) { onResult(false, _skuPriceState.value) }
                }
            }
        }
    }

    fun setSkuPriceForTesting(price: String?) {
        _skuPriceState.value = price
    }

    fun formatPriceDisplay(rawPrice: String?): String {
        if (rawPrice.isNullOrBlank()) return ""
        val trimmed = rawPrice.trim()
        if (trimmed.contains("تومان") || trimmed.contains("تومن")) {
            return toPersianDigits(trimmed)
        }
        val digitsOnly = trimmed.replace(Regex("[^0-9]"), "")
        if (digitsOnly.isNotEmpty()) {
            try {
                val amountLong = digitsOnly.toLong()
                val formattedNumber = java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(amountLong)
                return "${toPersianDigits(formattedNumber)} تومان"
            } catch (_: Throwable) {}
        }
        return toPersianDigits(trimmed)
    }

    private fun toPersianDigits(text: String): String {
        val persianDigits = arrayOf("۰", "۱", "۲", "۳", "۴", "۵", "۶", "۷", "۸", "۹")
        var result = text
        for (i in 0..9) {
            result = result.replace(i.toString(), persianDigits[i])
        }
        return result
    }

    fun getBillingErrorMessage(responseCode: Int): String {
        return when (responseCode) {
            1 -> "پرداخت توسط کاربر لغو شد."
            2 -> "ارتباط با سرور کافهبازار برقرار نشد. لطفاً اتصال اینترنت خود را بررسی کنید."
            3 -> "سیستم پرداخت کافهبازار در این نسخه پشتیبانی نمی‌شود."
            4 -> "محصول مورد نظر (kiseh_pro_lifetime) در کافهبازار یافت نشد."
            5 -> "خطای فنی در ارسال درخواست پرداخت."
            6 -> "خطای سیستمی کافهبازار در هنگام انجام تراکنش."
            7 -> "این اشتراک قبلاً خریداری شده است."
            else -> "خطا در برقراری ارتباط با کافهبازار (کد: $responseCode)"
        }
    }

    fun restorePurchases(context: Context, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        val appCtx = context.applicationContext
        val service = mService

        if (service == null) {
            init(context)
            onResult(false, "ارتباط با کافهبازار برقرار نشد. لطفاً از نصب و فعال بودن کافهبازار اطمینان حاصل کنید.")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val purchasesBundle: Bundle? = service.getPurchases(3, appCtx.packageName, "inapp", null)
                if (purchasesBundle == null) {
                    withContext(Dispatchers.Main) { onResult(false, "پاسخی از کافهبازار دریافت نشد.") }
                    return@launch
                }

                val responseCode = purchasesBundle.getInt("RESPONSE_CODE", -1)
                if (responseCode != 0) {
                    withContext(Dispatchers.Main) { onResult(false, getBillingErrorMessage(responseCode)) }
                    return@launch
                }

                val purchaseDataList = purchasesBundle.getStringArrayList("INAPP_PURCHASE_DATA_LIST")
                val signatureList = purchasesBundle.getStringArrayList("INAPP_DATA_SIGNATURE_LIST")
                var restored = false
                var restoredToken: String? = null
                var restoredDate: Long = System.currentTimeMillis()
                var restoredProduct: String = SKU_VIP_LIFETIME

                purchaseDataList?.forEachIndexed { index, purchaseJson ->
                    try {
                        val json = JSONObject(purchaseJson)
                        val sku = json.optString("productId")
                        val state = json.optInt("purchaseState", -1)
                        val token = json.optString("purchaseToken")
                        val purchaseTime = json.optLong("purchaseTime", System.currentTimeMillis())
                        val signature = signatureList?.getOrNull(index)

                        val isVerified = if (!signature.isNullOrBlank() && BAZAAR_PUBLIC_KEY.isNotBlank()) {
                            verifyPurchase(purchaseJson, signature, BAZAAR_PUBLIC_KEY)
                        } else true

                        if ((sku in ALL_VIP_SKUS) && state == 0 && isVerified) {
                            restored = true
                            restoredToken = token
                            restoredDate = purchaseTime
                            restoredProduct = sku
                        }
                    } catch (e: Throwable) {
                        Log.e(TAG, "Error parsing restore JSON", e)
                    }
                }

                if (restored) {
                    setVipUserInternal(appCtx, true, restoredToken, restoredDate, restoredProduct)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(appCtx, "🎉 خرید VIP شما با موفقیت بازیابی شد.", Toast.LENGTH_LONG).show()
                        onResult(true, "خرید شما با موفقیت بازیابی شد.")
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(appCtx, "خریدی برای این حساب یافت نشد.", Toast.LENGTH_LONG).show()
                        onResult(false, "خریدی برای این حساب یافت نشد.")
                    }
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Error checking purchases", t)
                withContext(Dispatchers.Main) { onResult(false, "خطا: ${t.message}") }
            }
        }
    }

    fun checkPurchases(context: Context) {
        val service = mService ?: return
        val appCtx = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val purchasesBundle: Bundle? = service.getPurchases(3, appCtx.packageName, "inapp", null)
                if (purchasesBundle != null && purchasesBundle.getInt("RESPONSE_CODE", -1) == 0) {
                    val purchaseDataList = purchasesBundle.getStringArrayList("INAPP_PURCHASE_DATA_LIST")
                    val signatureList = purchasesBundle.getStringArrayList("INAPP_DATA_SIGNATURE_LIST")
                    var isVip = false
                    var token: String? = null
                    var date: Long = System.currentTimeMillis()
                    var product: String = SKU_VIP_LIFETIME

                    purchaseDataList?.forEachIndexed { index, purchaseJson ->
                        try {
                            val json = JSONObject(purchaseJson)
                            val sku = json.optString("productId")
                            val state = json.optInt("purchaseState", -1)
                            val signature = signatureList?.getOrNull(index)
                            val isVerified = if (!signature.isNullOrBlank() && BAZAAR_PUBLIC_KEY.isNotBlank()) {
                                verifyPurchase(purchaseJson, signature, BAZAAR_PUBLIC_KEY)
                            } else true

                            if ((sku in ALL_VIP_SKUS) && state == 0 && isVerified) {
                                isVip = true
                                token = json.optString("purchaseToken")
                                date = json.optLong("purchaseTime", System.currentTimeMillis())
                                product = sku
                            }
                        } catch (e: Throwable) {
                            Log.e(TAG, "Error parsing purchase JSON", e)
                        }
                    }
                    if (isVip) {
                        setVipUserInternal(appCtx, true, token, date, product)
                    }
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Error checking purchases", t)
            }
        }
    }

    fun purchasePro(activity: Activity, onSuccess: () -> Unit = {}, onFailure: (String) -> Unit = {}) {
        purchaseVip(activity, onSuccess, onFailure)
    }

    fun purchaseVip(activity: Activity, onSuccess: () -> Unit = {}, onFailure: (String) -> Unit = {}) {
        try {
            init(activity)
            val service = mService

            if (service == null) {
                val errorMsg = "ارتباط با سرور کافهبازار برقرار نشد. لطفاً اتصال اینترنت خود را بررسی کنید."
                Toast.makeText(activity, errorMsg, Toast.LENGTH_LONG).show()
                onFailure("Bazaar billing service not available")
                return
            }

            val developerPayload = "kiseh_pro_payload_${System.currentTimeMillis()}"
            val buyIntentBundle = service.getBuyIntent(3, activity.packageName, SKU_PRO_LIFETIME, "inapp", developerPayload)

            if (buyIntentBundle == null) {
                Toast.makeText(activity, "پاسخی از سرور کافهبازار دریافت نشد.", Toast.LENGTH_SHORT).show()
                onFailure("buyIntentBundle is null")
                return
            }

            val responseCode = buyIntentBundle.getInt("RESPONSE_CODE", -1)

            if (responseCode == 0) {
                @Suppress("DEPRECATION")
                val pendingIntent = buyIntentBundle.getParcelable<PendingIntent>("BUY_INTENT")
                if (pendingIntent != null) {
                    activity.startIntentSenderForResult(
                        pendingIntent.intentSender,
                        PURCHASE_REQUEST_CODE,
                        Intent(), 0, 0, 0
                    )
                } else {
                    onFailure("PendingIntent is null")
                }
            } else if (responseCode == 7) {
                setVipUserInternal(activity, true, null, System.currentTimeMillis(), SKU_PRO_LIFETIME)
                Toast.makeText(activity, "🎉 شما قبلاً این اشتراک را خریداری کرده‌اید.", Toast.LENGTH_LONG).show()
                onSuccess()
            } else {
                val errorMsg = getBillingErrorMessage(responseCode)
                Toast.makeText(activity, errorMsg, Toast.LENGTH_SHORT).show()
                onFailure(errorMsg)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Error launching purchase", t)
            Toast.makeText(activity, "امکان اتصال به درگاه کافهبازار میسر نشد: ${t.message}", Toast.LENGTH_SHORT).show()
            onFailure(t.message ?: "Error launching purchase")
        }
    }

    fun release() {
        if (isBound) {
            try {
                appContext?.unbindService(serviceConnection)
            } catch (t: Throwable) {
                Log.e(TAG, "Error unbinding service", t)
            }
            isBound = false
            mService = null
            connectedStorePackage = null
        }
    }

    fun isQuickVoiceNotificationEnabled(context: Context): Boolean {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.getBoolean(KEY_QUICK_VOICE_ENABLED, false)
        } catch (_: Throwable) { false }
    }

    fun setQuickVoiceNotificationEnabled(context: Context, enabled: Boolean) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_QUICK_VOICE_ENABLED, enabled).commit()
        } catch (t: Throwable) {
            Log.e(TAG, "Error saving notification pref", t)
        }
    }

    fun isProUser(context: Context): Boolean = isVipUser(context)

    fun isVipUser(context: Context): Boolean {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val isPro = prefs.getBoolean(KEY_IS_PRO, false)
            _isProState.value = isPro
            isPro
        } catch (_: Throwable) { _isProState.value }
    }

    fun setProUser(context: Context, isPro: Boolean) {
        setVipUserInternal(context, isPro, null, System.currentTimeMillis(), SKU_VIP_LIFETIME)
    }

    fun setVipUser(
        context: Context,
        isVip: Boolean,
        purchaseToken: String? = null,
        purchaseDate: Long = System.currentTimeMillis(),
        productId: String? = SKU_VIP_LIFETIME
    ) {
        setVipUserInternal(context, isVip, purchaseToken, purchaseDate, productId)
    }

    private fun setVipUserInternal(
        context: Context,
        isVip: Boolean,
        purchaseToken: String?,
        purchaseDate: Long,
        productId: String?
    ) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val editor = prefs.edit().putBoolean(KEY_IS_PRO, isVip)
            if (isVip) {
                editor.putString(KEY_PURCHASE_STORE, STORE_BAZAAR)
                purchaseToken?.let { editor.putString(KEY_PURCHASE_TOKEN, it) }
                editor.putLong(KEY_PURCHASE_DATE, purchaseDate)
                productId?.let { editor.putString(KEY_PRODUCT_ID, it) }
            } else {
                editor.remove(KEY_PURCHASE_STORE)
                editor.remove(KEY_PURCHASE_TOKEN)
                editor.remove(KEY_PURCHASE_DATE)
                editor.remove(KEY_PRODUCT_ID)
            }
            editor.commit()
            _isProState.value = isVip

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    VipPreferencesManager(context.applicationContext).saveVipStatus(
                        isVip = isVip,
                        purchaseToken = purchaseToken,
                        purchaseDate = purchaseDate,
                        productId = productId
                    )
                } catch (t: Throwable) {
                    Log.e(TAG, "Error saving VIP to DataStore", t)
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Error setting VIP user", t)
            _isProState.value = isVip
        }
    }

    fun setProUserForTesting(context: Context, isPro: Boolean) {
        setVipUserInternal(context, isPro, "test_token", System.currentTimeMillis(), SKU_VIP_LIFETIME)
    }

    private fun checkAndResetDailyUsage(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
            val lastDate = prefs.getString(KEY_VOICE_USAGE_DATE, "") ?: ""
            if (lastDate.isEmpty()) {
                prefs.edit().putString(KEY_VOICE_USAGE_DATE, today).putInt(KEY_VOICE_USAGE_COUNT, 0).apply()
                return
            }
            if (today < lastDate) return
            if (lastDate != today) {
                prefs.edit().putString(KEY_VOICE_USAGE_DATE, today).putInt(KEY_VOICE_USAGE_COUNT, 0).apply()
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Error resetting daily voice usage", t)
        }
    }

    fun getVoiceUsageCount(context: Context): Int {
        checkAndResetDailyUsage(context)
        return try {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getInt(KEY_VOICE_USAGE_COUNT, 0)
        } catch (_: Throwable) { 0 }
    }

    fun getFreeVoiceUsage(context: Context): FreeVoiceUsage {
        checkAndResetDailyUsage(context)
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            FreeVoiceUsage(
                lastResetDate = prefs.getString(KEY_VOICE_USAGE_DATE, "") ?: "",
                countToday = prefs.getInt(KEY_VOICE_USAGE_COUNT, 0)
            )
        } catch (_: Throwable) { FreeVoiceUsage("", 0) }
    }

    fun incrementVoiceUsage(context: Context) {
        checkAndResetDailyUsage(context)
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val current = prefs.getInt(KEY_VOICE_USAGE_COUNT, 0)
            prefs.edit().putInt(KEY_VOICE_USAGE_COUNT, current + 1).apply()
        } catch (t: Throwable) {
            Log.e(TAG, "Error incrementing voice usage", t)
        }
    }

    fun consumeVoiceTrial(context: Context) { incrementVoiceUsage(context) }

    fun canUseVoiceInput(context: Context): Boolean {
        if (isVipUser(context)) return true
        return getVoiceUsageCount(context) < MAX_FREE_VOICE_USAGE
    }

    fun getRemainingVoiceTrial(context: Context): Int {
        if (isVipUser(context)) return Int.MAX_VALUE
        val remaining = MAX_FREE_VOICE_USAGE - getVoiceUsageCount(context)
        return if (remaining < 0) 0 else remaining
    }

    fun resetVoiceUsageForTesting(context: Context, count: Int = 0, date: String? = null) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val targetDate = date ?: java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
            prefs.edit().putString(KEY_VOICE_USAGE_DATE, targetDate).putInt(KEY_VOICE_USAGE_COUNT, count).apply()
        } catch (t: Throwable) {
            Log.e(TAG, "Error resetting voice usage", t)
        }
    }

    fun verifyPurchase(signedData: String, signature: String?, publicKeyString: String = BAZAAR_PUBLIC_KEY): Boolean {
        if (publicKeyString.isBlank() || signature.isNullOrBlank()) return false
        return try {
            val decodedKey = android.util.Base64.decode(publicKeyString, android.util.Base64.DEFAULT)
            val keyFactory = java.security.KeyFactory.getInstance("RSA")
            val publicKey = keyFactory.generatePublic(java.security.spec.X509EncodedKeySpec(decodedKey))
            val sig = java.security.Signature.getInstance("SHA1withRSA")
            sig.initVerify(publicKey)
            sig.update(signedData.toByteArray(Charsets.UTF_8))
            sig.verify(android.util.Base64.decode(signature, android.util.Base64.DEFAULT))
        } catch (t: Throwable) {
            Log.e(TAG, "RSA signature verification failed", t)
            false
        }
    }
}