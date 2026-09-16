package com.example

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.billing.BillingManager
import com.example.data.AppDatabase
import com.example.data.PendingBankSms
import com.example.data.PendingBankSmsRepository
import com.example.sms.BankSmsParser
import com.example.sms.BankSmsPreferencesManager
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
class VipBankSmsAccessTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repository: PendingBankSmsRepository
    private val parser = BankSmsParser()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        BillingManager.setProUserForTesting(context, true)
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .openHelperFactory(FrameworkSQLiteOpenHelperFactory())
            .allowMainThreadQueries()
            .build()
        repository = PendingBankSmsRepository(db.pendingBankSmsDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testVipHasFullAccessToBankSmsIntelligence() = runBlocking {
        assertTrue(BillingManager.isProUser(context))

        val smsText = "بانک ملت\nبرداشت از حساب: 250000 ریال\nکارت: 1234\nخرید از: فروشگاه افق کوروش\nمانده: 10000000"
        val parsed = parser.parse(smsText, "Melat")

        assertNotNull(parsed)
        val pending = PendingBankSms(
            bankName = parsed!!.bank.displayName,
            amount = parsed.amount,
            isIncome = parsed.isIncome,
            balance = parsed.balance,
            cardLastDigits = parsed.cardNumber,
            merchant = "افق کوروش",
            suggestedCategory = parsed.suggestedCategory,
            createdAt = parsed.timestamp,
            status = "PENDING"
        )

        val id = repository.insert(pending)
        assertTrue(id > 0)

        val allPending = repository.allPendingSms.first()
        assertEquals(1, allPending.size)
        assertEquals("افق کوروش", allPending[0].merchant)
        assertEquals(25000L, allPending[0].amount)
    }
}
