package com.example

import com.example.billing.FreePlanLimits
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FreeLimitsDisplayTest {

    @Test
    fun testCentralizedFreePlanLimitsValues() {
        assertEquals(5, FreePlanLimits.VOICE_DAILY_LIMIT)
        assertEquals(3, FreePlanLimits.BANK_SMS_TRIAL_LIMIT)
        assertEquals(3, FreePlanLimits.INVOICE_SCAN_LIMIT)
        assertEquals(1, FreePlanLimits.MAX_SAVINGS_GOALS)
        assertEquals(1, FreePlanLimits.MAX_RECURRING_TRANSACTIONS)
        assertEquals(2, FreePlanLimits.MAX_CUSTOM_CATEGORIES)
    }

    @Test
    fun testFreePlanLimitsDisplayStrings() {
        assertTrue(FreePlanLimits.LIMIT_VOICE_LABEL.contains("5 ثبت در روز"))
        assertTrue(FreePlanLimits.LIMIT_BANK_SMS_LABEL.contains("3 تراکنش آزمایشی"))
        assertTrue(FreePlanLimits.LIMIT_INVOICE_LABEL.contains("3 استفاده رایگان"))
        assertTrue(FreePlanLimits.LIMIT_SAVINGS_LABEL.contains("1 هدف فعال"))
        assertTrue(FreePlanLimits.LIMIT_RECURRING_LABEL.contains("1 مورد فعال"))
        assertEquals("پشتیبان‌گیری: محدود", FreePlanLimits.LIMIT_BACKUP_LABEL)
    }
}
