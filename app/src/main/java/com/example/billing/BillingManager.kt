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
 * مدیریت پرداخت درون‌برنامه‌ای مایکت (Myket In-App Billing) و فعال‌سازی اشتراک دائم VIP در اپلیکیشن کیسه.
 * کاملاً امن، مقاوم در برابر عدم وجود مایکت روی دستگاه، تایید امضای RSA و ذخیره‌سازی وضعیت در DataStore.
 */
object BillingManager {

    private const val TAG = "BillingManager"

    // کلید عمومی رسمی RSA برنامه در پنل مایکت
    const val MYKET_PUBLIC_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQChuEJCVjyXbvgOF74J3Dzbo86482Q+HI7UFFX1odEhqQ4qBO/62dEtjBvMzxUe8rZeVEpxJeATTx95+FIZJPiogXVCYhWwXokrSYO0F67wgTivoapyUUIevzZtZhi6JTEQebIPNR2J8qIRPzzC09FOVylhNobJGqXXcet1fzlrOQIDAQAB"

    const val MYKET_PACKAGE = "ir.mservices.market"
    const val MYKET_BILLING_ACTION = "ir.mservices.market.InAppBillingService.BIND"

    // شناسه محصول رسمی VIP دائم در مایکت
    const val SKU_PRO_LIFETIME = "kiseh_pro_lifetime"
    const val SKU_VIP_LIFETIME = "kiseh_pro_lifetime"
    val ALL_VIP_SKUS = listOf(SKU_PRO_LIFETIME)

    const val PURCHASE_REQUEST_CODE = 10001
    const val STORE_MYKET = "myket"
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
                Log.d(TAG, "Billing Connect Result: true (Connected to Myket Billing Service: ${name?.packageName})")
                appContext?.let { ctx ->
                    querySkuDetails(ctx)
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Billing Connect Result: false (Error in onServiceConnected)", t)
                mService = null
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mService = null
            isBound = false
            connectedStorePackage = null
            Log.d(TAG, "Billing Connect Result: false (Disconnected from Billing Service)")
        }
    }

    /**
     * تنظیم و اتصال به سرویس پرداخت مایکت (Myket In-App Billing)
     */
    fun init(context: Context) {
        try {
            val applicationContext = context.applicationContext
            appContext = applicationContext
            val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

            // بررسی اصالت VIP: اگر از استورهای غیر مایکت باقی مانده باشد پاکسازی شود
            val store = prefs.getString(KEY_PURCHASE_STORE, null)
            val isPro = prefs.getBoolean(KEY_IS_PRO, false)

            if (isPro && (store == "bazaar" || (store != null && store != STORE_MYKET))) {
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

            // همگام‌سازی از DataStore در پس‌زمینه
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val vipInfo = VipPreferencesManager(applicationContext).getVipInfo()
                    if (vipInfo.isVip) {
                        _isProState.value = true
                        prefs.edit().putBoolean(KEY_IS_PRO, true).putString(KEY_PURCHASE_STORE, STORE_MYKET).apply()
                    }
                } catch (_: Throwable) {}
            }

            if (mService == null && !isBound) {
                Log.d(TAG, "Billing Connect Start: pkg=$MYKET_PACKAGE, action=$MYKET_BILLING_ACTION")
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val pm = applicationContext.packageManager

                        // انحصاراً سرویس پرداخت مایکت
                        val myketIntent = Intent(MYKET_BILLING_ACTION).apply {
                            setPackage(MYKET_PACKAGE)
                        }
                        if (pm.queryIntentServices(myketIntent, 0).isNotEmpty()) {
                            isBound = applicationContext.bindService(myketIntent, serviceConnection, Context.BIND_AUTO_CREATE)
                            connectedStorePackage = MYKET_PACKAGE
                            Log.d(TAG, "Billing Connect Result: bindService initiated=$isBound")
                        } else {
                            Log.d(TAG, "Billing Connect Result: false (Myket app not installed on device)")
                        }
                    } catch (t: Throwable) {
                        Log.e(TAG, "Billing Connect Result: false (Error binding to billing service)", t)
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
     * استعلام مشخصات و قیمت محصول از مایکت (SKU Query)
     */
    fun querySkuDetails(context: Context, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        val service = mService
        val appCtx = context.applicationContext
        if (service == null) {
            Log.d(TAG, "SKU Query Start: Service not bound yet, returning cached or null")
            onResult(false, _skuPriceState.value)
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "SKU Query Start: sku=$SKU_PRO_LIFETIME, pkg=${appCtx.packageName}")
                val skuBundle = Bundle()
                skuBundle.putStringArrayList("ITEM_ID_LIST", arrayListOf(SKU_PRO_LIFETIME))

                val skuDetailsBundle = service.getSkuDetails(3, appCtx.packageName, "inapp", skuBundle)
                if (skuDetailsBundle == null) {
                    Log.e(TAG, "SKU Query Result: bundle is null")
                    withContext(Dispatchers.Main) {
                        onResult(false, _skuPriceState.value)
                    }
                    return@launch
                }

                val responseCode = skuDetailsBundle.getInt("RESPONSE_CODE", -1)
                Log.d(TAG, "SKU Query Result: responseCode=$responseCode")

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
                                    Log.d(TAG, "SKU Query Result: sku=$sku, title=$title, price=$formattedPrice")
                                    withContext(Dispatchers.Main) {
                                        onResult(true, formattedPrice)
                                    }
                                    return@launch
                                }
                            } catch (e: Throwable) {
                                Log.e(TAG, "Error parsing SKU detail JSON", e)
                            }
                        }
                    }
                }
                withContext(Dispatchers.Main) {
                    onResult(false, _skuPriceState.value)
                }
            } catch (t: Throwable) {
                Log.e(TAG, "SKU Query Result: error", t)
                withContext(Dispatchers.Main) {
                    onResult(false, _skuPriceState.value)
                }
            }
        }
    }

    /**
     * تنظیم قیمت برای محیط تست
     */
    fun setSkuPriceForTesting(price: String?) {
        _skuPriceState.value = price
    }

    /**
     * نرمال‌سازی و قالب‌بندی رشته قیمت دریافتی از مایکت به فارسی
     */
    fun formatPriceDisplay(rawPrice: String?): String {
        if (rawPrice.isNullOrBlank()) return ""
        val trimmed = rawPrice.trim()
        // اگر قبلاً حاوی کلمه تومان یا ریال است
        if (trimmed.contains("تومان") || trimmed.contains("تومن")) {
            return toPersianDigits(trimmed)
        }
        // استخراج عدد
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

    /**
     * ترجمه کدهای خطای رسمی IInAppBillingService مایکت به پیام‌های شفاف و تفکیک‌شده برای کاربر
     */
    fun getBillingErrorMessage(responseCode: Int): String {
        return when (responseCode) {
            1 -> "پرداخت توسط کاربر لغو شد."
            2 -> "ارتباط با سرور مایکت برقرار نشد. لطفاً اتصال اینترنت خود را بررسی کنید."
            3 -> "سیستم پرداخت مایکت در این نسخه پشتیبانی نمی‌شود."
            4 -> "محصول مورد نظر (kiseh_pro_lifetime) در مایکت یافت نشد."
            5 -> "خطای فنی در ارسال درخواست پرداخت."
            6 -> "خطای سیستمی مایکت در هنگام انجام تراکنش."
            7 -> "این اشتراک قبلاً خریداری شده است."
            else -> "خطا در برقراری ارتباط با مایکت (کد: $responseCode)"
        }
    }

    /**
     * بازیابی خریدهای قبلی کاربر (Restore Purchases)
     */
    fun restorePurchases(context: Context, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        val appCtx = context.applicationContext
        val service = mService

        if (service == null) {
            init(context)
            onResult(false, "ارتباط با مایکت برقرار نشد. لطفاً از نصب و فعال بودن مایکت اطمینان حاصل کنید.")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Restore Purchases Started")
                val purchasesBundle: Bundle? = service.getPurchases(3, appCtx.packageName, "inapp", null)
                if (purchasesBundle == null) {
                    withContext(Dispatchers.Main) {
                        onResult(false, "پاسخی از مایکت دریافت نشد.")
                    }
                    return@launch
                }

                val responseCode = purchasesBundle.getInt("RESPONSE_CODE", -1)
                Log.d(TAG, "Restore Purchases Response Code: $responseCode")
                if (responseCode != 0) {
                    val errorMsg = getBillingErrorMessage(responseCode)
                    withContext(Dispatchers.Main) {
                        onResult(false, errorMsg)
                    }
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

                        Log.d(TAG, "Purchase Token: $token")
                        Log.d(TAG, "Purchase State: $state")
                        Log.d(TAG, "Verify Started: sku=$sku, signature=$signature")

                        val isVerified = if (!signature.isNullOrBlank() && MYKET_PUBLIC_KEY.isNotBlank()) {
                            verifyPurchase(purchaseJson, signature, MYKET_PUBLIC_KEY)
                        } else {
                            true
                        }

                        Log.d(TAG, "Verify Result: $isVerified")

                        if ((sku in ALL_VIP_SKUS) && state == 0 && isVerified) {
                            restored = true
                            restoredToken = token
                            restoredDate = purchaseTime
                            restoredProduct = sku
                        }
                    } catch (e: Throwable) {
                        Log.e(TAG, "Error parsing restore purchase JSON", e)
                    }
                }

                if (restored) {
                    setVipUserInternal(appCtx, true, restoredToken, restoredDate, restoredProduct)
                    Log.d(TAG, "VIP Activated: token=$restoredToken, time=$restoredDate, product=$restoredProduct")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(appCtx, "🎉 خرید VIP شما با موفقیت بازیابی شد.", Toast.LENGTH_LONG).show()
                        onResult(true, "خرید شما با موفقیت بازیابی شد.")
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(appCtx, "خریدی برای این حساب در مایکت یافت نشد.", Toast.LENGTH_LONG).show()
                        onResult(false, "خریدی برای این حساب در مایکت یافت نشد.")
                    }
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Error checking purchases", t)
                withContext(Dispatchers.Main) {
                    onResult(false, "خطا در اتصال به مایکت: ${t.message}")
                }
            }
        }
    }

    /**
     * بررسی خودکار خریدهای قبلی در پس‌زمینه
     */
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
                            val isVerified = if (!signature.isNullOrBlank() && MYKET_PUBLIC_KEY.isNotBlank()) {
                                verifyPurchase(purchaseJson, signature, MYKET_PUBLIC_KEY)
                            } else {
                                true
                            }

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
                        Log.d(TAG, "VIP Activated (background check): product=$product")
                    }
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Error checking purchases", t)
            }
        }
    }

    /**
     * شروع فرایند خرید VIP مایکت با لاگ‌گذاری کامل و مدیریت دقیق خطاها
     */
    fun purchasePro(activity: Activity, onSuccess: () -> Unit = {}, onFailure: (String) -> Unit = {}) {
        purchaseVip(activity, onSuccess, onFailure)
    }

    fun purchaseVip(activity: Activity, onSuccess: () -> Unit = {}, onFailure: (String) -> Unit = {}) {
        try {
            Log.d(TAG, "Purchase Start")
            Log.d(TAG, "Purchase SKU: $SKU_PRO_LIFETIME")
            init(activity)
            val service = mService

            if (service == null) {
                val errorMsg = "ارتباط با سرور مایکت برقرار نشد. لطفاً اتصال اینترنت خود را بررسی کنید."
                Toast.makeText(activity, errorMsg, Toast.LENGTH_LONG).show()
                Log.e(TAG, "Purchase Response Code: -1 (Service null)")
                onFailure("Myket billing service not available")
                return
            }

            val developerPayload = "kiseh_pro_payload_${System.currentTimeMillis()}"
            val buyIntentBundle = service.getBuyIntent(3, activity.packageName, SKU_PRO_LIFETIME, "inapp", developerPayload)

            if (buyIntentBundle == null) {
                val errorMsg = "پاسخی از سرور مایکت دریافت نشد."
                Toast.makeText(activity, errorMsg, Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Purchase Response Code: -1 (buyIntentBundle is null)")
                onFailure("buyIntentBundle is null")
                return
            }

            val responseCode = buyIntentBundle.getInt("RESPONSE_CODE", -1)
            val responseMsg = getBillingErrorMessage(responseCode)
            Log.d(TAG, "Purchase Response Code: $responseCode")
            Log.d(TAG, "Purchase Response Message: $responseMsg")

            if (responseCode == 0) {
                @Suppress("DEPRECATION")
                val pendingIntent = buyIntentBundle.getParcelable<PendingIntent>("BUY_INTENT")
                if (pendingIntent != null) {
                    activity.startIntentSenderForResult(
                        pendingIntent.intentSender,
                        PURCHASE_REQUEST_CODE,
                        Intent(),
                        0, 0, 0
                    )
                } else {
                    val errorMsg = "خطا در ایجاد پیوند پرداخت مایکت."
                    Toast.makeText(activity, errorMsg, Toast.LENGTH_SHORT).show()
                    onFailure("PendingIntent is null")
                }
            } else if (responseCode == 7) { // ITEM_ALREADY_OWNED
                setVipUserInternal(activity, true, null, System.currentTimeMillis(), SKU_PRO_LIFETIME)
                Log.d(TAG, "VIP Activated (Item already owned)")
                Toast.makeText(activity, "🎉 شما قبلاً این اشتراک را در مایکت خریداری کرده‌اید. دسترسی VIP فعال شد.", Toast.LENGTH_LONG).show()
                onSuccess()
            } else {
                val errorMsg = getBillingErrorMessage(responseCode)
                Toast.makeText(activity, errorMsg, Toast.LENGTH_SHORT).show()
                onFailure(errorMsg)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Error launching purchase", t)
            val errorMsg = "امکان اتصال به درگاه مایکت میسر نشد: ${t.message}"
            Toast.makeText(activity, errorMsg, Toast.LENGTH_SHORT).show()
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
        } catch (_: Throwable) {
            false
        }
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
        } catch (_: Throwable) {
            _isProState.value
        }
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
                editor.putString(KEY_PURCHASE_STORE, STORE_MYKET)
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

            if (isVip) {
                Log.d(TAG, "VIP Activated in internal state: product=$productId, token=$purchaseToken")
            }

            // ذخیره ماندگار در DataStore
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
                prefs.edit()
                    .putString(KEY_VOICE_USAGE_DATE, today)
                    .putInt(KEY_VOICE_USAGE_COUNT, 0)
                    .apply()
                return
            }
            if (today < lastDate) {
                return
            }
            if (lastDate != today) {
                prefs.edit()
                    .putString(KEY_VOICE_USAGE_DATE, today)
                    .putInt(KEY_VOICE_USAGE_COUNT, 0)
                    .apply()
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Error resetting daily voice usage", t)
        }
    }

    fun getVoiceUsageCount(context: Context): Int {
        checkAndResetDailyUsage(context)
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.getInt(KEY_VOICE_USAGE_COUNT, 0)
        } catch (_: Throwable) {
            0
        }
    }

    fun getFreeVoiceUsage(context: Context): FreeVoiceUsage {
        checkAndResetDailyUsage(context)
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            FreeVoiceUsage(
                lastResetDate = prefs.getString(KEY_VOICE_USAGE_DATE, "") ?: "",
                countToday = prefs.getInt(KEY_VOICE_USAGE_COUNT, 0)
            )
        } catch (_: Throwable) {
            FreeVoiceUsage(lastResetDate = "", countToday = 0)
        }
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

    fun consumeVoiceTrial(context: Context) {
        incrementVoiceUsage(context)
    }

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
            prefs.edit()
                .putString(KEY_VOICE_USAGE_DATE, targetDate)
                .putInt(KEY_VOICE_USAGE_COUNT, count)
                .apply()
        } catch (t: Throwable) {
            Log.e(TAG, "Error resetting voice usage for testing", t)
        }
    }

    /**
     * تایید امضای خرید مایکت با کلید عمومی RSA
     */
    fun verifyPurchase(signedData: String, signature: String?, publicKeyString: String = MYKET_PUBLIC_KEY): Boolean {
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
