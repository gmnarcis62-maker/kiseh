package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.billing.BillingManager
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MyketSkuQueryTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        BillingManager.setSkuPriceForTesting(null)
    }

    @After
    fun tearDown() {
        BillingManager.setSkuPriceForTesting(null)
    }

    @Test
    fun testSkuIdentifierMatchesMyketPanel() {
        assertEquals("kiseh_pro_lifetime", BillingManager.SKU_PRO_LIFETIME)
        assertEquals("kiseh_pro_lifetime", BillingManager.SKU_VIP_LIFETIME)
        assertTrue(BillingManager.ALL_VIP_SKUS.contains("kiseh_pro_lifetime"))
        assertEquals(1, BillingManager.ALL_VIP_SKUS.size)
    }

    @Test
    fun testQuerySkuDetailsGracefulWhenUnbound() {
        var callbackInvoked = false
        var callbackSuccess = true
        var resultPrice: String? = "not_null"

        BillingManager.querySkuDetails(context) { success, price ->
            callbackInvoked = true
            callbackSuccess = success
            resultPrice = price
        }

        assertTrue(callbackInvoked)
        assertFalse(callbackSuccess)
        assertNull(resultPrice)
    }

    @Test
    fun testSkuDetailsJsonParsingLogic() {
        val sampleMyketJson = JSONObject().apply {
            put("productId", "kiseh_pro_lifetime")
            put("type", "inapp")
            put("price", "1000")
            put("title", "اشتراک مادام‌العمر کیسه VIP")
            put("description", "دسترسی نامحدود به تمام امکانات کیسه")
        }

        val parsedSku = sampleMyketJson.optString("productId")
        val rawPrice = sampleMyketJson.optString("price")
        val formattedPrice = BillingManager.formatPriceDisplay(rawPrice)

        assertEquals("kiseh_pro_lifetime", parsedSku)
        assertEquals("۱,۰۰۰ تومان", formattedPrice)
    }
}
