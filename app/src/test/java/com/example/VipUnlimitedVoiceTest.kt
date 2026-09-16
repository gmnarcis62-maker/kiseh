package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.billing.BillingManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VipUnlimitedVoiceTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        BillingManager.setProUserForTesting(context, true)
        BillingManager.resetVoiceUsageForTesting(context, count = 0)
    }

    @Test
    fun testVipHasUnlimitedVoiceRegistrations() {
        assertTrue(BillingManager.isProUser(context))
        assertEquals(Int.MAX_VALUE, BillingManager.getRemainingVoiceTrial(context))

        // کاربر VIP حتی پس از بیش از ۵ ثبت (مثلاً ۱۵ بار) همچنان بدون محدودیت دسترسی دارد
        for (i in 1..15) {
            assertTrue(BillingManager.canUseVoiceInput(context))
            assertEquals(Int.MAX_VALUE, BillingManager.getRemainingVoiceTrial(context))
        }

        assertTrue(BillingManager.canUseVoiceInput(context))
    }
}
