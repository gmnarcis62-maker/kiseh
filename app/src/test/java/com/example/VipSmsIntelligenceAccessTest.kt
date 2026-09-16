package com.example

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.billing.BillingManager
import com.example.data.AppDatabase
import com.example.data.PendingBankSms
import com.example.data.PendingBankSmsRepository
import com.example.data.Transaction
import com.example.data.TransactionRepository
import com.example.sms.BankSmsParser
import com.example.sms.BankType
import com.example.sms.SmartCategoryMatcher
import com.example.sms.SmsNotificationHelper
import com.example.sms.UserCategoryLearner
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNotificationManager

/**
 * تست‌های جامع دسترسی کامل و نامحدود کاربران VIP به تمام قابلیت‌های هوش پیامکی بانکی
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VipSmsIntelligenceAccessTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repository: PendingBankSmsRepository
    private val parser = BankSmsParser()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
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
        Dispatchers.resetMain()
    }

    @Test
    fun testVipUser_isProStateIsTrue() {
        assertTrue(BillingManager.isProUser(context))
        assertTrue(BillingManager.isVipUser(context))
    }

    @Test
    fun testVipUser_parserWithContextSuccessfullyParses() {
        val sms = "بانک ملت\nبرداشت: ۲۵۰,۰۰۰ ریال\nاز کارت: *1234\nخرید: فروشگاه افق کوروش\nمانده: ۱۰,۰۰۰,۰۰۰ ریال"
        val parsed = parser.parse(context = context, smsText = sms, sender = "بانک ملت")

        assertNotNull(parsed)
        assertEquals(BankType.MELLAT, parsed!!.bank)
        assertEquals(25000L, parsed.amount)
        assertEquals(1000000L, parsed.balance)
        assertFalse(parsed.isIncome)
        assertEquals("خوراک", parsed.suggestedCategory)
    }

    @Test
    fun testVipUser_smsNotificationHelperPostsNotifications() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val shadowNotificationManager: ShadowNotificationManager = shadowOf(notificationManager)

        val sms = PendingBankSms(
            id = 202L,
            bankName = "بانک سامان",
            amount = 150000L,
            isIncome = false,
            merchant = "اسنپ",
            suggestedCategory = "حمل و نقل"
        )

        SmsNotificationHelper.showPendingSmsNotification(context, sms)
        assertTrue(shadowNotificationManager.size() > 0)

        val notification = shadowNotificationManager.allNotifications.first()
        assertEquals("تراکنش بانکی جدید شناسایی شد", notification.extras.getString("android.title"))
    }

    @Test
    fun testVipUser_userCategoryLearnerWorks() {
        val learner = UserCategoryLearner.getInstance(context)
        learner.learn("فروشگاه پروتئینی نمونه", "خوراکی")

        val retrieved = learner.getLearnedCategory("فروشگاه پروتئینی نمونه")
        assertEquals("خوراکی", retrieved)
        assertTrue(learner.getAllLearned().isNotEmpty())
    }

    @Test
    fun testVipUser_advancedCategoryMatcherHasHighConfidence() {
        val result = SmartCategoryMatcher.matchCategoryAdvanced(
            context = context,
            text = "برداشت 300000 ریال از کارت اسنپ فود",
            merchant = "اسنپ فود",
            isIncome = false
        )
        assertTrue("کاربر VIP باید به مدل تطبیق پیشرفته دسترسی داشته باشد", result.confidence >= 0.80f)
        assertEquals("رستوران و کافه", result.category)
    }

    @Test
    fun testVipUser_viewModelConfirmPendingSmsRegistersTransaction() = testScope.runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        BillingManager.setVipUser(app, true)
        BillingManager.setProUserForTesting(app, true)
        val appDb = AppDatabase.getDatabase(app)
        val viewModel = MainViewModel(app)
        val pendingRepo = PendingBankSmsRepository(appDb.pendingBankSmsDao())
        val transactionRepo = TransactionRepository(appDb.transactionDao())

        val pending = PendingBankSms(
            bankName = "بانک صادرات",
            amount = 80000L,
            isIncome = false,
            merchant = "کافه ویونا",
            suggestedCategory = "خوراک",
            createdAt = System.currentTimeMillis()
        )
        val id = pendingRepo.insert(pending)
        val inserted = pending.copy(id = id)

        viewModel.confirmPendingSms(inserted)
        advanceUntilIdle()
        runBlocking { kotlinx.coroutines.delay(300) }

        // بررسی اینکه تراکنش در ویومدل و دیتابیس درج شده است
        val transactions = runBlocking { appDb.transactionDao().getAllTransactionsList() }
        val tx = transactions.find { it.amount == 80000L }
        assertNotNull("کاربر VIP باید بتواند تراکنش پیامکی را تایید و ثبت کند", tx)

        // پاکسازی
        appDb.transactionDao().deleteAll()
        appDb.pendingBankSmsDao().delete(inserted)
    }

    @Test
    fun testVipUser_pendingSmsTransactionsFlowExposesItems() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = MainViewModel(app)

        // برای کاربر VIP اگر دیتابیس پیامک معلق داشته باشد، در Flow منعکس می‌شود
        val flowValue = viewModel.pendingSmsTransactions.first()
        assertNotNull(flowValue)
    }
}
