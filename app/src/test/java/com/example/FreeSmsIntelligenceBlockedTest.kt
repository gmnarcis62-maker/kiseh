package com.example

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.room.Room
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.billing.BillingManager
import com.example.data.AppDatabase
import com.example.data.PendingBankSms
import com.example.data.PendingBankSmsRepository
import com.example.data.TransactionRepository
import com.example.sms.BankSmsParser
import com.example.sms.BankSmsReceiver
import com.example.sms.MatchSource
import com.example.sms.SmartCategoryMatcher
import com.example.sms.SmsActionReceiver
import com.example.sms.SmsNotificationHelper
import com.example.sms.UserCategoryLearner
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNotificationManager

/**
 * تست‌های جامع مسدود بودن کامل قابلیت‌های هوش پیامکی برای کاربران نسخه رایگان (Free)
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FreeSmsIntelligenceBlockedTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var pendingRepository: PendingBankSmsRepository
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var receiver: BankSmsReceiver
    private val parser = BankSmsParser()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        BillingManager.setProUserForTesting(context, false)
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .openHelperFactory(FrameworkSQLiteOpenHelperFactory())
            .allowMainThreadQueries()
            .build()
        pendingRepository = PendingBankSmsRepository(db.pendingBankSmsDao())
        transactionRepository = TransactionRepository(db.transactionDao())
        receiver = BankSmsReceiver()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testFreeUser_isProStateIsFalse() {
        assertFalse(BillingManager.isProUser(context))
        assertFalse(BillingManager.isVipUser(context))
    }

    @Test
    fun testFreeUser_canParseBankSmsWithinQuota() {
        val sms = "بانک ملت\nبرداشت از حساب: 500000 ریال\nپذیرنده: فروشگاه کوروش"
        val parsed = parser.parse(context = context, smsText = sms, sender = "بانک ملت")
        assertNotNull("کاربر رایگان باید بتواند در سهمیه آزمایشی پیامک بانکی را تحلیل کند", parsed)
    }

    @Test
    fun testFreeUser_backgroundSmsBlockedFromDatabase() = runBlocking {
        val intent = Intent("android.provider.Telephony.SMS_RECEIVED")
        receiver.onReceive(context, intent)

        val pendingList = db.pendingBankSmsDao().getAllPending().first()
        assertEquals(0, pendingList.size)

        val transactions = db.transactionDao().getAllTransactions().first()
        assertEquals(0, transactions.size)
    }

    @Test
    fun testFreeUser_notificationHelperPostsNotificationWhenInvoked() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val shadowNotificationManager: ShadowNotificationManager = shadowOf(notificationManager)

        val dummySms = PendingBankSms(
            id = 101L,
            bankName = "بانک ملت",
            amount = 50000L,
            isIncome = false,
            merchant = "تست"
        )

        SmsNotificationHelper.showPendingSmsNotification(context, dummySms)
        assertTrue("نوتیفیکیشن هنگام پردازش پیامک سهمیه رایگان باید ارسال شود", shadowNotificationManager.size() > 0)
    }

    @Test
    fun testFreeUser_smsActionReceiverDoesNotInsertTransaction() = runBlocking {
        val actionReceiver = SmsActionReceiver()
        val confirmIntent = Intent(SmsActionReceiver.ACTION_CONFIRM).apply {
            putExtra(SmsActionReceiver.EXTRA_SMS_ID, 999L)
        }
        actionReceiver.onReceive(context, confirmIntent)

        val transactions = db.transactionDao().getAllTransactions().first()
        assertEquals(0, transactions.size)
    }

    @Test
    fun testFreeUser_userCategoryLearnerBlocked() {
        val learner = UserCategoryLearner.getInstance(context)
        val vipOnlyResult = learner.learnVipOnly("دیجی کالا", "خرید آنلاین")
        assertFalse("کاربر رایگان نباید بتواند با learnVipOnly دسته‌بندی ذخیره کند", vipOnlyResult)
        assertNull("کاربر رایگان نباید بتواند دسته‌بندی یاد گرفته شده با learnVipOnly بازیابی کند", learner.getLearnedCategory("دیجی کالا"))
    }

    @Test
    fun testFreeUser_advancedCategoryMatcherReturnsFallback() {
        val result = SmartCategoryMatcher.matchCategoryAdvanced(
            context = context,
            text = "برداشت 450000 ریال از کارت فروشگاه رفاه",
            merchant = "رفاه",
            isIncome = false
        )
        assertEquals(MatchSource.FALLBACK_DEFAULT, result.source)
        assertEquals(0.50f, result.confidence, 0.01f)
    }

    @Test
    fun testFreeUser_viewModelBlocksSmsConfirmOperations() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = MainViewModel(app)

        val pending = PendingBankSms(
            id = 555L,
            bankName = "بانک سپه",
            amount = 100000L,
            isIncome = false,
            merchant = "فروشگاه"
        )

        viewModel.confirmPendingSms(pending)
        viewModel.editAndConfirmPendingSms(pending, 100000L, "خوراک", "خرید", false)

        val allTx = viewModel.allTransactions.first()
        assertEquals("هیچ تراکنشی نباید توسط کاربر رایگان از طریق هوش پیامکی ثبت شود", 0, allTx.size)
    }

    @Test
    fun testFreeUser_pendingSmsTransactionsFlowIsEmpty() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = MainViewModel(app)

        val pendingList = viewModel.pendingSmsTransactions.first()
        assertTrue("جریان پیامک‌های معلق برای کاربر غیر VIP باید خالی باشد", pendingList.isEmpty())
    }
}
