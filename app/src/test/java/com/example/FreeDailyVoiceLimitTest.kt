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
class FreeDailyVoiceLimitTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        BillingManager.setProUser(context, false)
        BillingManager.resetVoiceUsageForTesting(context, 0, "2026-09-12")
    }

    @After
    fun tearDown() {
        BillingManager.resetVoiceUsageForTesting(context, 0, "2026-09-12")
    }

    @Test
    fun testFreeDailyLimitAllowsExactlyFiveRecordings() {
        assertEquals(0, BillingManager.getVoiceUsageCount(context))
        assertEquals(5, BillingManager.getRemainingVoiceTrial(context))

        // Record 5 times successfully
        for (i in 1..5) {
            assertTrue(BillingManager.canUseVoiceInput(context))
            BillingManager.incrementVoiceUsage(context)
            assertEquals(i, BillingManager.getVoiceUsageCount(context))
            assertEquals(5 - i, BillingManager.getRemainingVoiceTrial(context))
        }

        // 6th attempt must be blocked
        assertFalse(BillingManager.canUseVoiceInput(context))
        assertEquals(0, BillingManager.getRemainingVoiceTrial(context))
    }

    @Test
    fun testDailyResetOnDateChange() {
        // Use all 5 on yesterday
        BillingManager.resetVoiceUsageForTesting(context, 5, "2026-09-11")

        // Next day (today: 2026-09-12), usage must automatically reset to 0
        val count = BillingManager.getVoiceUsageCount(context)
        assertEquals(0, count)
        assertTrue(BillingManager.canUseVoiceInput(context))
        assertEquals(5, BillingManager.getRemainingVoiceTrial(context))
    }
}
