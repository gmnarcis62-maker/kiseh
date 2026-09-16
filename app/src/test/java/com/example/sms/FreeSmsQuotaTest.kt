package com.example.sms

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.billing.BillingManager
import com.example.billing.FeatureUsageManager
import com.example.billing.VipPreferencesManager
import com.example.data.AppDatabase
import com.example.data.PendingBankSms
import com.example.data.PendingBankSmsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FreeSmsQuotaTest {

    private lateinit var context: Context
    private lateinit var parser: BankSmsParser

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        parser = BankSmsParser()
        runBlocking {
            BillingManager.setVipUser(context, false)
            VipPreferencesManager(context).clearVip()
            FeatureUsageManager.resetAllForTesting(context)
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
    fun testFreeUser_canReceiveAndParseSmsWithinQuota() {
        assertTrue("Free user should be able to process SMS initially", FeatureUsageManager.canProcessBankSms(context))
        assertEquals(3, FeatureUsageManager.getRemainingBankSmsTrial(context))

        val smsText = """
            برداشت 200,000 ریال
            از حساب 1234
            پذیرنده: سوپرمارکت
            مانده: 1,000,000 ریال
            بانک ملت
        """.trimIndent()

        val parsed = parser.parse(smsText = smsText, sender = "MELLAT", context = context)
        assertNotNull("Free user can parse SMS", parsed)
        assertEquals(20000L, parsed?.amount)
    }

    @Test
    fun testFreeUser_quotaLimitEnforcedAtThirdSms() = runBlocking {
        assertEquals(0, FeatureUsageManager.getBankSmsUsageCount(context))

        // Process 3 SMS
        repeat(3) {
            assertTrue("Should allow SMS when count < 3", FeatureUsageManager.canProcessBankSms(context))
            FeatureUsageManager.incrementBankSmsUsage(context)
        }

        assertEquals(3, FeatureUsageManager.getBankSmsUsageCount(context))
        assertEquals(0, FeatureUsageManager.getRemainingBankSmsTrial(context))
        assertFalse("Free user quota limit reached at 3 SMS", FeatureUsageManager.canProcessBankSms(context))
    }

    @Test
    fun testFreeUser_pendingSmsSavedInDatabaseWithinQuota() = runBlocking {
        val appDb = AppDatabase.getDatabase(context)
        val repo = PendingBankSmsRepository(appDb.pendingBankSmsDao())

        val smsText = """
            واریز 1,000,000 ریال
            به حساب 5678
            شرح: واریز یارانه
            بانک ملی ایران
        """.trimIndent()

        val parsed = parser.parse(smsText = smsText, sender = "BMI", context = context)
        assertNotNull(parsed)

        if (FeatureUsageManager.canProcessBankSms(context)) {
            val pendingSms = PendingBankSms(
                bankName = parsed!!.bank.displayName,
                amount = parsed.amount,
                isIncome = parsed.isIncome,
                balance = parsed.balance,
                cardLastDigits = parsed.cardNumber,
                merchant = "واریز یارانه",
                suggestedCategory = parsed.suggestedCategory,
                createdAt = parsed.timestamp
            )
            val id = repo.insert(pendingSms)
            assertTrue(id > 0)
            FeatureUsageManager.incrementBankSmsUsage(context)
        }

        assertEquals(1, FeatureUsageManager.getBankSmsUsageCount(context))
        val allPending = repo.allPendingSms.first()
        assertTrue(allPending.any { it.amount == 100000L })
    }
}
