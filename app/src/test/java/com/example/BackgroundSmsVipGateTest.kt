package com.example

import android.content.Context
import android.content.Intent
import androidx.room.Room
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.billing.BillingManager
import com.example.data.AppDatabase
import com.example.data.PendingBankSms
import com.example.data.PendingBankSmsRepository
import com.example.sms.BankSmsPreferencesManager
import com.example.sms.BankSmsReceiver
import com.example.sms.SmsActionReceiver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * تست‌های اختصاصی راستی‌آزمایی گیت VIP در پردازش مستقیم پیامک‌های پس‌زمینه (Background SMS VIP Gate)
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackgroundSmsVipGateTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var pendingRepository: PendingBankSmsRepository
    private lateinit var receiver: BankSmsReceiver
    private lateinit var actionReceiver: SmsActionReceiver

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .openHelperFactory(FrameworkSQLiteOpenHelperFactory())
            .allowMainThreadQueries()
            .build()
        pendingRepository = PendingBankSmsRepository(db.pendingBankSmsDao())
        receiver = BankSmsReceiver()
        actionReceiver = SmsActionReceiver()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testBackgroundSms_whenUserIsFree_dropsImmediatelyWithoutAnyProcessing() = runBlocking {
        // ۱. کاربر در وضعیت Free تنظیم می‌شود
        BillingManager.setProUserForTesting(context, false)
        assertFalse(BillingManager.isProUser(context))

        // ۲. ارسال پیامک مستقیم در پس‌زمینه
        val intent = Intent("android.provider.Telephony.SMS_RECEIVED")
        receiver.onReceive(context, intent)

        // ۳. اطمینان ۱۰۰٪ از اینکه هیچگونه رکوردی در جدول پیامک‌های معلق یا تراکنش‌ها ثبت نشده است
        val pendingList = db.pendingBankSmsDao().getAllPending().first()
        assertEquals("هیچ پیامک معلقی برای کاربر رایگان در پس‌زمینه نباید ثبت شود", 0, pendingList.size)

        val transactions = db.transactionDao().getAllTransactions().first()
        assertEquals("هیچ تراکنشی برای کاربر رایگان در پس‌زمینه نباید درج شود", 0, transactions.size)
    }

    @Test
    fun testBackgroundSms_whenTelephonyActionTriggeredForFreeUser_abortsSilently() = runBlocking {
        BillingManager.setProUserForTesting(context, false)

        val telephonyIntent = Intent("android.provider.Telephony.SMS_RECEIVED")
        receiver.onReceive(context, telephonyIntent)

        val allPending = pendingRepository.getAllPendingList()
        assertTrue("لیست پیامک‌های معلق باید کاملاً خالی باشد", allPending.isEmpty())
    }

    @Test
    fun testBackgroundActionReceiver_confirmFromNotification_blockedForFreeUser() = runBlocking {
        BillingManager.setProUserForTesting(context, false)

        // درج دستی یک پیامک در دیتابیس (مثلاً از قبل باقی مانده)
        val initialSms = PendingBankSms(
            bankName = "بانک سپه",
            amount = 50000L,
            isIncome = false,
            merchant = "نانوایی"
        )
        val insertedId = pendingRepository.insert(initialSms)

        // ارسال مستقیم Intent تایید نوتیفیکیشن
        val confirmIntent = Intent(SmsActionReceiver.ACTION_CONFIRM).apply {
            putExtra(SmsActionReceiver.EXTRA_SMS_ID, insertedId)
            putExtra(SmsActionReceiver.EXTRA_NOTIFICATION_ID, 123)
        }
        actionReceiver.onReceive(context, confirmIntent)

        // اطمینان از اینکه هیچ تراکنشی درج نشده است
        val transactions = db.transactionDao().getAllTransactions().first()
        assertEquals("کاربر رایگان نباید بتواند با نوتیفیکیشن تراکنش ثبت کند", 0, transactions.size)
    }

    @Test
    fun testBackgroundSms_upgradeToVipUnlocksGate() {
        // ۱. ابتدا کاربر Free است
        BillingManager.setProUserForTesting(context, false)
        assertFalse(BillingManager.isProUser(context))

        // ۲. ارتقا به VIP
        BillingManager.setProUserForTesting(context, true)
        assertTrue(BillingManager.isProUser(context))
        assertTrue(BillingManager.isVipUser(context))
    }
}
