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
class BazaarBillingReferenceTest {

    private lateinit var context: Context

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        BillingManager.setProUser(context, false)
        VipPreferencesManager(context).clearVip()
    }

    @After
    fun tearDown() = runBlocking {
        BillingManager.setProUser(context, false)
        VipPreferencesManager(context).clearVip()
    }

    @Test
    fun testOnlyMyketPackageConfigured() {
        assertEquals("ir.mservices.market", BillingManager.MYKET_PACKAGE)
        assertEquals("ir.mservices.market.InAppBillingService.BIND", BillingManager.MYKET_BILLING_ACTION)
        assertEquals("kiseh_pro_lifetime", BillingManager.SKU_VIP_LIFETIME)
    }

    @Test
    fun testLegacyBazaarVipIsPurgedAndDenied() = runBlocking {
        // Simulate an old user having legacy Bazaar VIP stored in SharedPreferences
        val prefs = context.getSharedPreferences("kiseh_billing_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("is_pro_user", true)
            .putString("purchase_store", "bazaar")
            .putString("purchase_token", "old_bazaar_token_123")
            .apply()

        // Call init() to simulate app launch
        BillingManager.init(context)

        // Verify that legacy Bazaar VIP status is purged and not accepted
        assertFalse("Legacy Bazaar VIP must be revoked and not recognized as VIP", BillingManager.isVipUser(context))
        assertFalse("StateFlow must emit false for legacy Bazaar user", BillingManager.isProState.value)
    }

    @Test
    fun testMyketVerificationIsValid() = runBlocking {
        // Legit Myket purchase activates and is accepted
        BillingManager.setVipUser(
            context = context,
            isVip = true,
            purchaseToken = "myket_valid_token_xyz",
            purchaseDate = System.currentTimeMillis(),
            productId = BillingManager.SKU_VIP_LIFETIME
        )

        assertTrue("Legitimate Myket VIP must be active", BillingManager.isVipUser(context))
        assertTrue(BillingManager.isProState.value)
    }
}
