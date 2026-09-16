package com.example

import android.content.Context
import android.content.Intent
import androidx.room.Room
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.billing.BillingManager
import com.example.data.AppDatabase
import com.example.sms.BankSmsReceiver
import com.example.sms.MatchSource
import com.example.sms.SmartCategoryMatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FreeBankSmsBlockedTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var receiver: BankSmsReceiver

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        BillingManager.setProUserForTesting(context, false)
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .openHelperFactory(FrameworkSQLiteOpenHelperFactory())
            .allowMainThreadQueries()
            .build()
        receiver = BankSmsReceiver()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testFreeUserHasNoBackgroundSmsProcessing() = runBlocking {
        assertFalse(BillingManager.isProUser(context))

        // شبیه‌سازی دریافت پیامک بانکی در نسخه رایگان
        val intent = Intent("android.provider.Telephony.SMS_RECEIVED")
        receiver.onReceive(context, intent)

        // اطمینان از اینکه هیچ رکوردی در دیتابیس درج نشده است
        val pendingList = db.pendingBankSmsDao().getAllPending().first()
        assertEquals(0, pendingList.size)
    }

    @Test
    fun testFreeUserBlockedFromAdvancedCategoryMatching() {
        assertFalse(BillingManager.isProUser(context))

        // نسخه رایگان نباید تطبیق پیشرفته انجام دهد و حالت پیش‌فرض برمی‌گرداند
        val result = SmartCategoryMatcher.matchCategoryAdvanced(
            context = context,
            text = "خرید از فروشگاه زنجیره‌ای افق کوروش مبلغ 150000 تومان",
            merchant = "افق کوروش",
            isIncome = false
        )

        assertEquals(0.50f, result.confidence, 0.01f)
        assertEquals(MatchSource.FALLBACK_DEFAULT, result.source)
    }
}
