package com.example.sms

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.billing.BillingManager
import com.example.billing.FeatureUsageManager
import com.example.billing.VipPreferencesManager
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VipUnlimitedSmsTest {

    private lateinit var context: Context
    private lateinit var parser: BankSmsParser

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        parser = BankSmsParser()
        runBlocking {
            BillingManager.setVipUser(context, true)
            BankSmsPreferencesManager.getInstance(context).setBankSmsEnabled(true)
        }
    }

    @After
    fun tearDown() {
        runBlocking {
            BillingManager.setVipUser(context, false)
            VipPreferencesManager(context).clearVip()
            FeatureUsageManager.resetAllForTesting(context)
        }
    }

    @Test
    fun testVipUser_hasUnlimitedSmsProcessing() {
        assertTrue("VIP user can process SMS without limit", FeatureUsageManager.canProcessBankSms(context))
        assertEquals(Int.MAX_VALUE, FeatureUsageManager.getRemainingBankSmsTrial(context))
    }

    @Test
    fun testVipUser_incrementBankSmsUsageDoesNotLimitVip() {
        repeat(10) {
            FeatureUsageManager.incrementBankSmsUsage(context)
        }

        assertTrue("VIP user remains active after 10 SMS", FeatureUsageManager.canProcessBankSms(context))
        assertEquals(Int.MAX_VALUE, FeatureUsageManager.getRemainingBankSmsTrial(context))
    }

    @Test
    fun testVipUser_parseMultipleBankSmsWithoutGate() {
        val sms1 = "برداشت 500,000 ریال از حساب: 1234 بانک ملت"
        val sms2 = "خرید: 150,000 تومان از کارت 5678 بلوبانک"
        val sms3 = "واریز 2,000,000 ریال به حساب 9999 بانک سامان"

        val p1 = parser.parse(smsText = sms1, sender = "MELLAT", context = context)
        val p2 = parser.parse(smsText = sms2, sender = "BLU", context = context)
        val p3 = parser.parse(smsText = sms3, sender = "SAMAN", context = context)

        assertNotNull(p1)
        assertNotNull(p2)
        assertNotNull(p3)

        assertEquals(BankType.MELLAT, p1?.bank)
        assertEquals(BankType.BLUBANK, p2?.bank)
        assertEquals(BankType.SAMAN, p3?.bank)
    }
}
