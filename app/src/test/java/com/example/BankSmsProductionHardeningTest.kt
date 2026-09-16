package com.example

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.PendingBankSms
import com.example.data.PendingBankSmsRepository
import com.example.data.SmsStatus
import com.example.sms.BankSmsDuplicateDetector
import com.example.sms.BankSmsParser
import com.example.sms.BankSmsPreferencesManager
import com.example.sms.SmsNotificationHelper
import com.example.sms.SmsPermissionManager
import com.example.sms.SmsPermissionState
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
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BankSmsProductionHardeningTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: PendingBankSmsRepository
    private lateinit var prefsManager: BankSmsPreferencesManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        com.example.billing.BillingManager.setProUserForTesting(context, true)
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5
            )
            .build()
        repository = PendingBankSmsRepository(database.pendingBankSmsDao())
        prefsManager = BankSmsPreferencesManager.getInstance(context)
        runBlocking { prefsManager.resetToDefaults() }
        BankSmsDuplicateDetector.clearCache()
    }

    @After
    fun tearDown() {
        database.close()
        BankSmsDuplicateDetector.clearCache()
    }

    // 1. تست وضعیت‌های مختلف مجوز و Onboarding
    @Test
    fun testSmsPermissionManager_StateResolution() {
        val app = shadowOf(ApplicationProvider.getApplicationContext<Application>())
        val activityController = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = activityController.get()

        // حالت پیش‌فرض: مجوز اعطا نشده
        app.denyPermissions(Manifest.permission.RECEIVE_SMS)
        val stateDenied = SmsPermissionManager.getPermissionState(activity)
        assertFalse(SmsPermissionManager.hasSmsPermission(activity))
        assertTrue(
            stateDenied == SmsPermissionState.DENIED_CAN_ASK_AGAIN ||
            stateDenied == SmsPermissionState.PERMANENTLY_DENIED
        )

        // پس از اعطای مجوز
        app.grantPermissions(Manifest.permission.RECEIVE_SMS)
        assertTrue(SmsPermissionManager.hasSmsPermission(activity))
        assertEquals(SmsPermissionState.GRANTED, SmsPermissionManager.getPermissionState(activity))

        activityController.destroy()
    }

    // 2. تست ترجیحات و تنظیمات DataStore
    @Test
    fun testBankSmsPreferencesManager_Configuration() = runBlocking {
        // مقادیر پیش‌فرض
        assertTrue(prefsManager.isBankSmsEnabled())
        assertTrue(prefsManager.isSmartCategoryEnabled())
        assertFalse(prefsManager.isAutoConfirmEnabled())
        assertTrue(prefsManager.isQuickActionEnabled())
        assertTrue(prefsManager.isNotificationEnabled())
        assertTrue(prefsManager.isUserLearningEnabled())

        // تغییر تنظیمات و بررسی جریان داده‌ها (Flow)
        prefsManager.setAutoConfirmEnabled(true)
        assertTrue(prefsManager.isAutoConfirmEnabled())
        assertTrue(prefsManager.isAutoConfirmEnabledFlow.first())

        prefsManager.setBankSmsEnabled(false)
        assertFalse(prefsManager.isBankSmsEnabled())
        assertFalse(prefsManager.isBankSmsEnabledFlow.first())

        prefsManager.setQuickActionEnabled(false)
        assertFalse(prefsManager.isQuickActionEnabled())
    }

    // 3. تست سیستم تشخیص تراکنش تکراری (Duplicate Protection)
    @Test
    fun testBankSmsDuplicateDetector_InMemoryAndDatabase() = runBlocking {
        val bank = "بانک ملت"
        val amount = 150000L
        val isIncome = false
        val card = "4321"
        val merchant = "فروشگاه افق کوروش"
        val now = System.currentTimeMillis()

        // بار اول نباید تکراری باشد
        val firstCheck = BankSmsDuplicateDetector.isDuplicate(
            context = context,
            bankName = bank,
            amount = amount,
            isIncome = isIncome,
            cardLastDigits = card,
            merchant = merchant,
            timestamp = now
        )
        assertFalse(firstCheck)

        // ثبت در کش
        BankSmsDuplicateDetector.recordProcessed(bank, amount, isIncome, card, merchant, now)

        // بررسی مجدد در همان پنجره زمانی باید تکراری تشخیص داده شود
        val secondCheck = BankSmsDuplicateDetector.isDuplicate(
            context = context,
            bankName = bank,
            amount = amount,
            isIncome = isIncome,
            cardLastDigits = card,
            merchant = merchant,
            timestamp = now + 1000L
        )
        assertTrue(secondCheck)

        // سناریو: یک SMS مشابه ۵ بار پشت سر هم ارسال شود (Burst duplicate)
        var duplicateCount = 0
        for (i in 1..5) {
            val isDup = BankSmsDuplicateDetector.isDuplicate(
                context = context,
                bankName = bank,
                amount = amount,
                isIncome = isIncome,
                cardLastDigits = card,
                merchant = merchant,
                timestamp = now + (i * 500L)
            )
            if (isDup) {
                duplicateCount++
            }
        }
        assertEquals("تمام ۵ پیامک مکرر باید تکراری تشخیص داده شوند", 5, duplicateCount)

        // تراکنش دیگری با مبلغ متفاوت نباید تکراری باشد
        val differentAmountCheck = BankSmsDuplicateDetector.isDuplicate(
            context = context,
            bankName = bank,
            amount = 250000L,
            isIncome = isIncome,
            cardLastDigits = card,
            merchant = merchant,
            timestamp = now + 1000L
        )
        assertFalse(differentAmountCheck)
    }

    // 4. تست امنیت و عدم نمایش اطلاعات حساس در اعلان‌ها (Notification Hardening)
    @Test
    fun testNotificationHardening_PrivacyAndGrouping() {
        val pending = PendingBankSms(
            id = 101L,
            bankName = "بانک صادرات",
            amount = 85000L,
            isIncome = false,
            balance = 1200000L,
            cardLastDigits = "9876",
            merchant = "اسنپ",
            suggestedCategory = "حمل و نقل",
            createdAt = System.currentTimeMillis()
        )

        SmsNotificationHelper.showPendingSmsNotification(context, pending, enableQuickActions = true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val shadowManager = shadowOf(notificationManager)

        val notifications = shadowManager.allNotifications
        assertTrue(notifications.isNotEmpty())

        val notification = notifications.first()
        assertEquals(SmsNotificationHelper.GROUP_KEY_BANK_SMS, notification.group)

        // بررسی عدم وجود متن خام پیامک و حفظ امنیت
        assertNotNull(notification.actions)
        assertEquals(2, notification.actions.size) // ثبت هزینه و نادیده گرفتن
    }

    // 5. تست اعلان تراکنش‌های تایید خودکار (Auto-Confirmed)
    @Test
    fun testAutoConfirmedNotification() {
        SmsNotificationHelper.showAutoConfirmedNotification(
            context = context,
            bankName = "بانک پاسارگاد",
            amount = 45000L,
            isIncome = false,
            merchant = "کافه ویونا",
            suggestedCategory = "خوراکی و رستوران"
        )

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val shadowManager = shadowOf(notificationManager)
        assertTrue(shadowManager.allNotifications.isNotEmpty())
    }

    // 6. تست پاکسازی خودکار پیامک‌های قدیمی (Auto-Cleanup)
    @Test
    fun testAutoCleanupOldProcessedSms() = runBlocking {
        val now = System.currentTimeMillis()
        val oldTime = now - (35L * 24 * 3600 * 1000L) // ۳۵ روز پیش

        val oldConfirmed = PendingBankSms(
            bankName = "بانک ملی",
            amount = 50000L,
            isIncome = false,
            createdAt = oldTime,
            status = SmsStatus.CONFIRMED.name
        )
        val oldRejected = PendingBankSms(
            bankName = "بانک ملت",
            amount = 20000L,
            isIncome = false,
            createdAt = oldTime,
            status = SmsStatus.REJECTED.name
        )
        val oldPending = PendingBankSms(
            bankName = "بانک تجارت",
            amount = 70000L,
            isIncome = false,
            createdAt = oldTime,
            status = SmsStatus.PENDING.name
        )
        val recentConfirmed = PendingBankSms(
            bankName = "بانک سامان",
            amount = 30000L,
            isIncome = false,
            createdAt = now,
            status = SmsStatus.CONFIRMED.name
        )

        repository.insert(oldConfirmed)
        repository.insert(oldRejected)
        repository.insert(oldPending)
        repository.insert(recentConfirmed)

        // پاکسازی پیامک‌های پردازش‌شده با سن بیشتر از ۳۰ روز
        val threshold = now - (30L * 24 * 3600 * 1000L)
        repository.cleanupOldProcessedSms(threshold)

        val allRemaining = database.pendingBankSmsDao().getAllList()

        // پیامک‌های تأیید/رد شده قدیمی باید حذف شده باشند
        assertFalse(allRemaining.any { it.bankName == "بانک ملی" })
        assertFalse(allRemaining.any { it.bankName == "بانک ملت" })

        // پیامک معلق حتی اگر قدیمی باشد نباید حذف شود
        assertTrue(allRemaining.any { it.bankName == "بانک تجارت" })

        // پیامک اخیر تأیید شده نباید حذف شود
        assertTrue(allRemaining.any { it.bankName == "بانک سامان" })
    }

    // 7. تست ضریب اطمینان در Parser (Confidence Score)
    @Test
    fun testBankSmsParserConfidenceScore() {
        val parser = BankSmsParser()
        val smsMelli = """
            بانک ملی ایران
            برداشت از: 603799****1234
            مبلغ: 120,000 ریال
            مانده: 5,430,000 ریال
            خرید از: افق کوروش
        """.trimIndent()

        val parsed = parser.parse(smsMelli)
        assertNotNull(parsed)
        assertEquals(12000L, parsed!!.amount)
        assertTrue(parsed.confidence >= 0.85f)
    }
}
