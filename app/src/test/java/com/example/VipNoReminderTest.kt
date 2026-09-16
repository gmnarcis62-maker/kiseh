package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.billing.BillingManager
import com.example.billing.FeatureUsageManager
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
class VipNoReminderTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        BillingManager.setVipUser(context, true)
    }

    @After
    fun tearDown() {
        BillingManager.setVipUser(context, false)
    }

    @Test
    fun testVipUserCleanExperienceNoLimits() {
        // VIP User must be identified
        assertTrue("VIP user state must be true", BillingManager.isVipUser(context))
        assertTrue("Pro user state must be true for VIP", BillingManager.isProUser(context))

        // VIP must have unlimited access across all features
        assertTrue(FeatureUsageManager.canUseVoiceInput(context))
        assertEquals(Int.MAX_VALUE, FeatureUsageManager.getRemainingVoiceTrial(context))

        assertTrue(FeatureUsageManager.canProcessBankSms(context))
        assertEquals(Int.MAX_VALUE, FeatureUsageManager.getRemainingBankSmsTrial(context))

        assertTrue(FeatureUsageManager.canScanInvoice(context))
        assertEquals(Int.MAX_VALUE, FeatureUsageManager.getRemainingInvoiceScans(context))

        assertTrue(FeatureUsageManager.canAddSavingsGoal(context, 100))
        assertTrue(FeatureUsageManager.canAddRecurringTransaction(context, 100))
    }
}
