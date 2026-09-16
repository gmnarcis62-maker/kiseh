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
class DailyVoiceResetTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        BillingManager.setProUserForTesting(context, false)
    }

    @Test
    fun testAutomaticDailyUsageReset() {
        // شبیه‌سازی اتمام سهمیه در روز گذشته
        val yesterday = "2026-09-09"
        BillingManager.resetVoiceUsageForTesting(context, count = 5, date = yesterday)

        // بررسی اینکه در روز گذشته ۵ بار مصرف شده بود
        val prefs = context.getSharedPreferences("kiseh_billing_prefs", Context.MODE_PRIVATE)
        assertEquals(5, prefs.getInt("voice_usage_count", 0))
        assertEquals(yesterday, prefs.getString("voice_usage_date", ""))

        // فراخوانی در روز جاری باعث ریست خودکار بر اساس تاریخ محلی دستگاه می‌شود
        val currentCount = BillingManager.getVoiceUsageCount(context)
        assertEquals(0, currentCount)
        assertEquals(5, BillingManager.getRemainingVoiceTrial(context))
        assertTrue(BillingManager.canUseVoiceInput(context))

        val usage = BillingManager.getFreeVoiceUsage(context)
        assertEquals(0, usage.countToday)
    }
}
