package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.billing.BillingManager
import com.example.billing.FreePlanLimits
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
class FreePlanReminderTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        BillingManager.setVipUser(context, false)
    }

    @After
    fun tearDown() {
        BillingManager.setVipUser(context, false)
    }

    @Test
    fun testFreeUserReminderContentAndState() {
        // Confirm user is Free
        assertFalse("User must be in Free plan mode", BillingManager.isVipUser(context))
        assertFalse("User must not be Pro user", BillingManager.isProUser(context))

        // Verify title & desc content
        assertEquals("شما در حال استفاده از نسخه رایگان کیسه هستید", FreePlanLimits.TITLE_FREE_PLAN)
        assertTrue(FreePlanLimits.DESC_FREE_PLAN.contains("تمام قابلیت‌های برنامه برای آشنایی شما فعال هستند"))
        assertTrue(FreePlanLimits.DESC_FREE_PLAN.contains("نسخه VIP را فعال کنید"))
    }
}
