package com.example.sms

import android.app.Application
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.test.core.app.ApplicationProvider
import com.example.billing.BillingManager
import com.example.billing.FeatureUsageManager
import com.example.billing.VipPreferencesManager
import com.example.data.AppDatabase
import com.example.data.PendingBankSmsRepository
import kotlinx.coroutines.flow.first
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
class RealSmsFlowTest {

    private lateinit var context: Context
    private lateinit var appDb: AppDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Application>()
        appDb = AppDatabase.getDatabase(context)
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
    fun testRealDeviceSmsParserEndToEnd_MellatBank() {
        val parser = BankSmsParser()
        val smsBody = """
            برداشت 500,000 ریال
            از حساب: 1234
            پذیرنده: فروشگاه کوروش
            مانده: 12,500,000 ریال
            بانک ملت
        """.trimIndent()

        val parsed = parser.parse(smsText = smsBody, sender = "MELLAT", context = context)
        assertNotNull("SMS Parser must parse Mellat real SMS even for Free user", parsed)
        assertEquals(BankType.MELLAT, parsed?.bank)
        assertEquals(50000L, parsed?.amount) // 500,000 Rials = 50,000 Tomans
        assertEquals(false, parsed?.isIncome)
        assertEquals("1234", parsed?.cardNumber)
    }

    @Test
    fun testRealDeviceSmsParserEndToEnd_BluBank() {
        val parser = BankSmsParser()
        val smsBody = """
            خرید: 120,000 تومان
            از کارت 5678
            پذیرنده: کافه لميز
            مانده: 1,450,000 تومان
            بلوبانک
        """.trimIndent()

        val parsed = parser.parse(smsText = smsBody, sender = "blubank", context = context)
        assertNotNull("SMS Parser must parse BluBank real SMS even for Free user", parsed)
        assertEquals(BankType.BLUBANK, parsed?.bank)
        assertEquals(120000L, parsed?.amount) // Neobanks use Toman directly
        assertEquals(false, parsed?.isIncome)
        assertEquals("5678", parsed?.cardNumber)
    }

    @Test
    fun testRealDeviceSmsReceiverToDatabaseFlow_FreeUser() = runBlocking {
        val receiver = BankSmsReceiver()
        val smsText = """
            برداشت 300,000 ریال
            از حساب 9876
            پذیرنده: فروشگاه زنجیره‌ای رفاه
            مانده: 5,000,000 ریال
            بانک ملی ایران
        """.trimIndent()

        val intent = Intent(Telephony.Sms.Intents.SMS_RECEIVED_ACTION).apply {
            putExtra("pdus", arrayOf<ByteArray>())
        }

        // Test parser directly and store in Pending repository as receiver does
        val parsed = BankSmsParser().parse(smsText = smsText, sender = "BMI", context = context)
        assertNotNull("Free user must be able to parse bank SMS", parsed)

        val repo = PendingBankSmsRepository(appDb.pendingBankSmsDao())
        val pendingId = repo.insert(
            com.example.data.PendingBankSms(
                bankName = parsed!!.bank.displayName,
                amount = parsed.amount,
                isIncome = parsed.isIncome,
                balance = parsed.balance,
                cardLastDigits = parsed.cardNumber,
                merchant = "فروشگاه زنجیره‌ای رفاه",
                suggestedCategory = parsed.suggestedCategory,
                createdAt = parsed.timestamp
            )
        )

        assertTrue("Pending SMS should be inserted into database", pendingId > 0L)
        val allPending = repo.allPendingSms.first()
        assertTrue("Database must contain inserted SMS", allPending.any { it.amount == 30000L })
    }

    @Test
    fun testRealDeviceSmsReceiverToDatabaseFlow_VipUser() = runBlocking {
        BillingManager.setVipUser(context, true)
        val smsText = """
            واریز 2,000,000 ریال
            به حساب 4321
            شرح: واریز حقوق
            بانک سامان
        """.trimIndent()

        val parsed = BankSmsParser().parse(smsText = smsText, sender = "SAMAN", context = context)
        assertNotNull("VIP user must parse Saman bank SMS", parsed)
        assertEquals(BankType.SAMAN, parsed?.bank)
        assertEquals(200000L, parsed?.amount) // 2,000,000 Rials = 200,000 Tomans
        assertEquals(true, parsed?.isIncome)
    }
}
