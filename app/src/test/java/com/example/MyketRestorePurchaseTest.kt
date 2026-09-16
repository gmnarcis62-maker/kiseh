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
class MyketRestorePurchaseTest {

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
    fun testRestorePurchaseOnReinstall() {
        // App starts as Free
        assertFalse(BillingManager.isVipUser(context))

        // Simulate restore with verified Myket token
        val token = "myket_restored_token_abc"
        val purchaseTime = 1710000000000L
        BillingManager.setVipUser(
            context = context,
            isVip = true,
            purchaseToken = token,
            purchaseDate = purchaseTime,
            productId = BillingManager.SKU_VIP_LIFETIME
        )

        assertTrue("Restoring valid purchase must activate VIP", BillingManager.isVipUser(context))
        assertTrue(BillingManager.isProState.value)
    }

    @Test
    fun testRestoreWhenNoPurchaseExistsKeepsFree() {
        BillingManager.setVipUser(context, false)

        var resultSuccess = false
        BillingManager.restorePurchases(context) { success, _ ->
            resultSuccess = success
        }

        assertFalse("Without Myket service connection, restore fails gracefully", resultSuccess)
        assertFalse(BillingManager.isVipUser(context))
        assertEquals(5, BillingManager.getRemainingVoiceTrial(context))
    }
}
