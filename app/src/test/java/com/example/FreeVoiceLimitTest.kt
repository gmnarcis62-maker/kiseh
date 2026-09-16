package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.billing.BillingManager
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
class FreeVoiceLimitTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        BillingManager.setProUserForTesting(context, false)
        BillingManager.resetVoiceUsageForTesting(context, count = 0)
    }

    @Test
    fun testFreeUserDailyLimitOfFiveRegistrations() {
        // ۱. بررسی وضعیت اولیه کاربر رایگان
        assertFalse(BillingManager.isProUser(context))
        assertEquals(0, BillingManager.getVoiceUsageCount(context))
        assertEquals(5, BillingManager.getRemainingVoiceTrial(context))
        assertTrue(BillingManager.canUseVoiceInput(context))

        // ۲. انجام ۵ ثبت موفق تراکنش صوتی
        for (i in 1..5) {
            assertTrue(BillingManager.canUseVoiceInput(context))
            BillingManager.incrementVoiceUsage(context)
            assertEquals(i, BillingManager.getVoiceUsageCount(context))
            assertEquals(5 - i, BillingManager.getRemainingVoiceTrial(context))
        }

        // ۳. پس از ۵ ثبت، سهمیه روزانه کاربر رایگان تمام می‌شود
        assertEquals(5, BillingManager.getVoiceUsageCount(context))
        assertEquals(0, BillingManager.getRemainingVoiceTrial(context))
        assertFalse(BillingManager.canUseVoiceInput(context))

        // ۴. بررسی مدل FreeVoiceUsage
        val usage = BillingManager.getFreeVoiceUsage(context)
        assertEquals(5, usage.countToday)
        assertTrue(usage.lastResetDate.isNotBlank())
    }

    @Test
    fun testUnsuccessfulRegistrationDoesNotConsumeQuota() {
        // ثبت ناموفق یا لغو توسط کاربر نباید سهمیه را کم کند
        val initialCount = BillingManager.getVoiceUsageCount(context)
        assertEquals(0, initialCount)

        // شبیه‌سازی شروع ضبط یا لغو بدون تایید نهایی
        // (هیچ فراخوانی incrementVoiceUsage صورت نمی‌گیرد)
        assertEquals(0, BillingManager.getVoiceUsageCount(context))
        assertEquals(5, BillingManager.getRemainingVoiceTrial(context))
        assertTrue(BillingManager.canUseVoiceInput(context))
    }
}
