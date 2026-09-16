package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.billing.BillingManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MyketPurchaseSuccessTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        BillingManager.setProUser(context, false)
    }

    @After
    fun tearDown() {
        BillingManager.setProUser(context, false)
    }

    @Test
    fun testMyketPurchaseSuccessActivatesVip() {
        // Initial state is free
        assertEquals(false, BillingManager.isVipUser(context))

        // Simulate successful Myket purchase with official SKU
        val purchaseToken = "myket_token_abc_12345"
        val purchaseTime = System.currentTimeMillis()
        val productId = BillingManager.SKU_VIP_LIFETIME

        BillingManager.setVipUser(
            context = context,
            isVip = true,
            purchaseToken = purchaseToken,
            purchaseDate = purchaseTime,
            productId = productId
        )

        // Verify VIP status is now active
        assertTrue(BillingManager.isVipUser(context))
        assertTrue(BillingManager.isProUser(context))
        assertTrue(BillingManager.isProState.value)
    }

    @Test
    fun testMyketPurchaseWithLegacySkuAlsoActivatesVip() {
        // Support legacy SKU for backward compatibility
        BillingManager.setVipUser(
            context = context,
            isVip = true,
            purchaseToken = "legacy_token",
            purchaseDate = System.currentTimeMillis(),
            productId = BillingManager.SKU_PRO_LIFETIME
        )

        assertTrue(BillingManager.isVipUser(context))
    }
}
