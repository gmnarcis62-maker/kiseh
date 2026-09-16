package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.billing.BillingManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VipStatusDisplayTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        BillingManager.setVipUser(context, false)
    }

    @Test
    fun testFreeStatusDisplay() {
        BillingManager.setVipUser(context, false)
        val statusText = if (BillingManager.isProUser(context)) "نسخه فعلی: VIP ✓" else "نسخه فعلی: رایگان"
        assertEquals("نسخه فعلی: رایگان", statusText)
    }

    @Test
    fun testVipStatusDisplay() {
        BillingManager.setVipUser(context, true)
        val statusText = if (BillingManager.isProUser(context)) "نسخه فعلی: VIP ✓" else "نسخه فعلی: رایگان"
        val subtitleText = if (BillingManager.isProUser(context))
            "تمام امکانات بدون محدودیت فعال هستند."
        else
            "تمام قابلیت‌ها جهت تست فعال می‌باشند اما دارای محدودیت تعداد استفاده هستند."

        assertEquals("نسخه فعلی: VIP ✓", statusText)
        assertEquals("تمام امکانات بدون محدودیت فعال هستند.", subtitleText)
    }
}
