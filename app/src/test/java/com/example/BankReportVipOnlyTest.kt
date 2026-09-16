package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.billing.BillingManager
import com.example.data.Transaction
import com.example.sms.BankIntelligenceReport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BankReportVipOnlyTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testBankReportBlockedForFreeUser() {
        BillingManager.setProUserForTesting(context, false)
        assertFalse(BillingManager.isProUser(context))
        assertFalse(BankIntelligenceReport.isAccessible(context))

        val dummyTransactions = listOf(
            Transaction(amount = 50000, category = "خوراکی", description = "بانک ملت - افق کوروش (کارت *1234)", isIncome = false, date = 1000L),
            Transaction(amount = 2000000, category = "درآمد", description = "بانک ملی - واریز حقوق", isIncome = true, date = 2000L)
        )

        val report = BankIntelligenceReport.generateReport(context, dummyTransactions)
        assertNull("گزارش هوشمند برای کاربر رایگان باید مسدود باشد", report)
    }

    @Test
    fun testBankReportGeneratesForVipUser() {
        BillingManager.setProUserForTesting(context, true)
        assertTrue(BillingManager.isProUser(context))
        assertTrue(BankIntelligenceReport.isAccessible(context))

        val dummyTransactions = listOf(
            Transaction(amount = 50000, category = "خوراکی", description = "بانک ملت - افق کوروش (کارت *1234)", isIncome = false, date = 1000L),
            Transaction(amount = 2000000, category = "درآمد", description = "بانک ملی - واریز حقوق", isIncome = true, date = 2000L)
        )

        val report = BankIntelligenceReport.generateReport(context, dummyTransactions)
        assertNotNull(report)
        assertEquals(50000L, report!!.totalExpenseFromSms)
        assertEquals(2000000L, report.totalIncomeFromSms)
        assertEquals(2, report.totalProcessedSms)
    }
}
