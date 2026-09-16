package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.billing.BillingManager
import com.example.billing.VipPreferencesManager
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MyketPurchaseTest {

    private lateinit var context: Context

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        BillingManager.setVipUser(context, false)
        VipPreferencesManager(context).clearVip()
    }

    @After
    fun tearDown() = runBlocking {
        BillingManager.setVipUser(context, false)
        VipPreferencesManager(context).clearVip()
    }

    @Test
    fun testMyketSkuConfiguration() {
        assertEquals("kiseh_pro_lifetime", BillingManager.SKU_VIP_LIFETIME)
        assertTrue(BillingManager.ALL_VIP_SKUS.contains(BillingManager.SKU_VIP_LIFETIME))
    }

    @Test
    fun testSuccessfulMyketPurchaseActivatesVip() {
        assertFalse(BillingManager.isVipUser(context))
        assertEquals(5, BillingManager.getRemainingVoiceTrial(context))

        val token = "myket_purchase_token_valid_7788"
        val time = System.currentTimeMillis()

        BillingManager.setVipUser(
            context = context,
            isVip = true,
            purchaseToken = token,
            purchaseDate = time,
            productId = BillingManager.SKU_VIP_LIFETIME
        )

        assertTrue(BillingManager.isVipUser(context))
        assertTrue(BillingManager.isProUser(context))
        assertTrue(BillingManager.isProState.value)
        assertTrue(BillingManager.isVipState.value)
        assertEquals(Int.MAX_VALUE, BillingManager.getRemainingVoiceTrial(context))
        assertTrue(BillingManager.canUseVoiceInput(context))
    }
}
