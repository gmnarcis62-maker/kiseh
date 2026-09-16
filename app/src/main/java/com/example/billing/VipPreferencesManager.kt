package com.example.billing

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.vipDataStore: DataStore<Preferences> by preferencesDataStore(name = "kiseh_vip_prefs")

/**
 * مدل اطلاعات خرید معتبر VIP جهت ذخیره در DataStore
 */
data class VipPurchaseInfo(
    val isVip: Boolean = false,
    val purchaseToken: String? = null,
    val purchaseDate: Long = 0L,
    val productId: String? = null
)

/**
 * مدیریت ماندگار وضعیت اشتراک VIP با Jetpack DataStore
 * منبع اصلی و معتبر فعال بودن VIP و خرید تایید شده مایکت
 */
class VipPreferencesManager(private val context: Context) {

    companion object {
        val KEY_IS_VIP = booleanPreferencesKey("is_vip_user")
        val KEY_PURCHASE_TOKEN = stringPreferencesKey("purchase_token")
        val KEY_PURCHASE_DATE = longPreferencesKey("purchase_date")
        val KEY_PRODUCT_ID = stringPreferencesKey("product_id")
    }

    private val safePrefsFlow: Flow<Preferences> = context.vipDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }

    val vipInfoFlow: Flow<VipPurchaseInfo> = safePrefsFlow.map { prefs ->
        VipPurchaseInfo(
            isVip = prefs[KEY_IS_VIP] ?: false,
            purchaseToken = prefs[KEY_PURCHASE_TOKEN],
            purchaseDate = prefs[KEY_PURCHASE_DATE] ?: 0L,
            productId = prefs[KEY_PRODUCT_ID]
        )
    }

    val isVipFlow: Flow<Boolean> = vipInfoFlow.map { it.isVip }

    suspend fun saveVipStatus(
        isVip: Boolean,
        purchaseToken: String? = null,
        purchaseDate: Long = System.currentTimeMillis(),
        productId: String? = null
    ) {
        context.vipDataStore.edit { prefs ->
            prefs[KEY_IS_VIP] = isVip
            if (purchaseToken != null) {
                prefs[KEY_PURCHASE_TOKEN] = purchaseToken
            } else if (!isVip) {
                prefs.remove(KEY_PURCHASE_TOKEN)
            }

            if (purchaseDate > 0L && isVip) {
                prefs[KEY_PURCHASE_DATE] = purchaseDate
            } else if (!isVip) {
                prefs.remove(KEY_PURCHASE_DATE)
            }

            if (productId != null) {
                prefs[KEY_PRODUCT_ID] = productId
            } else if (!isVip) {
                prefs.remove(KEY_PRODUCT_ID)
            }
        }
    }

    suspend fun getVipInfo(): VipPurchaseInfo {
        return try {
            vipInfoFlow.first()
        } catch (_: Throwable) {
            VipPurchaseInfo()
        }
    }

    suspend fun clearVip() {
        context.vipDataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
