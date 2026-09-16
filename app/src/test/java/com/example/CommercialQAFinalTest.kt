package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.billing.BillingManager
import com.example.sms.BankSmsReceiver
import com.example.sms.SmartCategoryMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CommercialQAFinalTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        BillingManager.setProUserForTesting(context, false)
        BillingManager.resetVoiceUsageForTesting(context, 0)
    }

    @Test
    fun testFreeVoiceQuotaExactlyFive() {
        // ۱. کاربر رایگان ۵ بار سهمیه ثبت موفق دارد
        assertTrue(BillingManager.canUseVoiceInput(context))
        assertEquals(5, BillingManager.getRemainingVoiceTrial(context))

        // ثبت موفق ۱ تا ۴
        for (i in 1..4) {
            BillingManager.incrementVoiceUsage(context)
            assertTrue(BillingManager.canUseVoiceInput(context))
            assertEquals(5 - i, BillingManager.getRemainingVoiceTrial(context))
        }

        // ثبت موفق ۵
        BillingManager.incrementVoiceUsage(context)
        assertEquals(5, BillingManager.getVoiceUsageCount(context))
        assertEquals(0, BillingManager.getRemainingVoiceTrial(context))

        // پس از ۵ ثبت، باید دسترسی قطع شود
        assertFalse("پس از ۵ ثبت موفق باید دسترسی ثبت صوتی در حالت رایگان مسدود شود", BillingManager.canUseVoiceInput(context))
    }

    @Test
    fun testClockManipulationDoesNotBypassLimit() {
        // کاربر ۵ ثبت انجام داده و سهمیه‌اش تمام شده است
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        BillingManager.resetVoiceUsageForTesting(context, count = 5, date = today)
        assertFalse(BillingManager.canUseVoiceInput(context))

        // شبیه‌سازی دستکاری ساعت دستگاه به گذشته (مثلاً کاربر تاریخ را یک ماه به عقب برمی‌گرداند)
        val pastDate = "2020-01-01"
        val prefs = context.getSharedPreferences("kiseh_billing_prefs", Context.MODE_PRIVATE)
        // فرض کنیم تاریخ ذخیره شده امروز است، اگر کاربر تاریخ سیستم را عقب ببرد (today < lastDate):
        // در متد checkAndResetDailyUsage، ریست نباید صورت گیرد
        BillingManager.resetVoiceUsageForTesting(context, count = 5, date = "2099-12-31") // تاریخ آینده در حافظه ثبت شده
        assertFalse("اگر ساعت به عقب برگردانده شود نباید سهمیه ریست شود", BillingManager.canUseVoiceInput(context))
    }

    @Test
    fun testAllPremiumFeaturesLockedForFreeUser() {
        // هوش پیامکی برای کاربر رایگان مسدود است
        assertFalse(BillingManager.isProUser(context))

        // Smart Category Matcher برای کاربر رایگان باید FALLBACK_DEFAULT باشد
        val matchResult = SmartCategoryMatcher.matchCategoryAdvanced(
            context = context,
            text = "خرید از سوپرمارکت کوروش",
            merchant = "افق کوروش",
            isIncome = false
        )
        assertEquals(com.example.sms.MatchSource.FALLBACK_DEFAULT, matchResult.source)
        assertEquals(0.50f, matchResult.confidence, 0.01f)
    }

    @Test
    fun testVipUserHasUnlimitedVoiceAndAllFeatures() {
        // فعال‌سازی VIP
        BillingManager.setProUserForTesting(context, true)
        assertTrue(BillingManager.isProUser(context))

        // کاربر VIP حتی با بیش از ۲۰ بار ثبت، همچنان نامحدود است
        for (i in 1..20) {
            BillingManager.incrementVoiceUsage(context)
            assertTrue("کاربر VIP باید سهمیه نامحدود داشته باشد", BillingManager.canUseVoiceInput(context))
        }

        val vipMatchResult = SmartCategoryMatcher.matchCategoryAdvanced(
            context = context,
            text = "خرید از سوپرمارکت کوروش",
            merchant = "افق کوروش",
            isIncome = false
        )
        assertTrue(vipMatchResult.confidence > 0.8f)
    }
}
