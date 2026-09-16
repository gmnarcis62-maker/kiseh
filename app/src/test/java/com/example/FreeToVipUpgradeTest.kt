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
class FreeToVipUpgradeTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        BillingManager.setProUser(context, false)
        BillingManager.resetVoiceUsageForTesting(context, 0)
    }

    @After
    fun tearDown() {
        BillingManager.setProUser(context, false)
        BillingManager.resetVoiceUsageForTesting(context, 0)
    }

    @Test
    fun testFreeUserReachingLimitUpgradesToVipAndBecomesUnlimited() {
        // Free user uses up all 5 voice recordings
        for (i in 1..5) {
            BillingManager.incrementVoiceUsage(context)
        }
        assertEquals(5, BillingManager.getVoiceUsageCount(context))
        assertFalse(BillingManager.canUseVoiceInput(context))

        // User purchases VIP in Myket
        BillingManager.setVipUser(
            context = context,
            isVip = true,
            purchaseToken = "upgraded_vip_token",
            productId = BillingManager.SKU_VIP_LIFETIME
        )

        // VIP status verified
        assertTrue(BillingManager.isVipUser(context))
        // Voice recordings are now unlimited
        assertTrue(BillingManager.canUseVoiceInput(context))
        assertEquals(Int.MAX_VALUE, BillingManager.getRemainingVoiceTrial(context))
    }
}
