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
class FreeLimitTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        BillingManager.setVipUser(context, false)
        BillingManager.resetVoiceUsageForTesting(context, 0)
    }

    @After
    fun tearDown() {
        BillingManager.setVipUser(context, false)
        BillingManager.resetVoiceUsageForTesting(context, 0)
    }

    @Test
    fun testFreeDailyLimitEnforcement() {
        // Free user starts with 5 voice recordings
        assertEquals(5, BillingManager.getRemainingVoiceTrial(context))
        assertTrue(BillingManager.canUseVoiceInput(context))

        // Consume 5 credits
        for (i in 1..5) {
            assertTrue("Credit $i should allow recording", BillingManager.canUseVoiceInput(context))
            BillingManager.consumeVoiceTrial(context)
            assertEquals(5 - i, BillingManager.getRemainingVoiceTrial(context))
        }

        // 6th attempt should be blocked
        assertFalse("Free user with 0 credits must be blocked", BillingManager.canUseVoiceInput(context))
        assertEquals(0, BillingManager.getRemainingVoiceTrial(context))

        // Upgrade to VIP
        BillingManager.setVipUser(context, true)
        assertTrue("VIP user must be allowed voice input unconditionally", BillingManager.canUseVoiceInput(context))
        assertEquals(Int.MAX_VALUE, BillingManager.getRemainingVoiceTrial(context))
    }
}
