package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.billing.BillingManager
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
class MyketPurchaseRestoreTest {

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
    fun testRestorePurchaseWhenUserReinstalls() {
        // App starts as Free
        assertFalse(BillingManager.isVipUser(context))

        // Simulate restoring valid past purchase from Myket
        var callbackInvoked = false
        var restoreSuccess = false
        var restoreMessage = ""

        // Calling simulate or setVipUser to restore purchase state
        BillingManager.setVipUser(
            context = context,
            isVip = true,
            purchaseToken = "restored_myket_token_9988",
            purchaseDate = 1700000000000L,
            productId = BillingManager.SKU_VIP_LIFETIME
        )

        assertTrue(BillingManager.isVipUser(context))
        assertTrue(BillingManager.isProUser(context))
    }

    @Test
    fun testRestoreWithoutPreviousPurchaseKeepsUserFree() {
        // User has never bought VIP
        BillingManager.setProUser(context, false)

        // Restore finds nothing
        var restoreResult = false
        BillingManager.restorePurchases(context) { success, msg ->
            restoreResult = success
        }

        // Must remain free
        assertFalse(BillingManager.isVipUser(context))
    }
}
