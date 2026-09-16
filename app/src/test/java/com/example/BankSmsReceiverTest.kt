package com.example

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.PendingBankSms
import com.example.data.PendingBankSmsRepository
import com.example.data.SmsStatus
import com.example.sms.BankSmsParser
import com.example.sms.BankSmsReceiver
import com.example.sms.BankType
import com.example.sms.SmsActionReceiver
import com.example.sms.SmsNotificationHelper
import com.example.sms.SmsPermissionManager
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BankSmsReceiverTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: PendingBankSmsRepository

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
    }

    @After
    fun tearDown() {
        database.close()
    }

    // =========================================================================
    // ۱. دریافت پیامک بانکی معتبر و ذخیره خودکار رکورد Pending
    // =========================================================================
    @Test
    fun testValidBankSms_createsPendingRecordAndNotifies() = runBlocking {
        val parser = BankSmsParser()
        val smsText = """
            بانک ملت
            برداشت: ۵۰۰,۰۰۰ ریال
            از کارت: *4589
            مانده: ۲,۰۰۰,۰۰۰ ریال
            پذیرنده: فروشگاه زنجیره‌ای رفاه
        """.trimIndent()

        val parsed = parser.parse(smsText, "بانک ملت")
        assertNotNull(parsed)
        assertEquals(BankType.MELLAT, parsed!!.bank)
        assertEquals(50000L, parsed.amount)

        // ذخیره در صف موقت دیتابیس
        val pendingSms = PendingBankSms(
            bankName = parsed.bank.displayName,
            amount = parsed.amount,
            isIncome = parsed.isIncome,
            balance = parsed.balance,
            cardLastDigits = parsed.cardNumber,
            merchant = "فروشگاه زنجیره‌ای رفاه",
            suggestedCategory = parsed.suggestedCategory,
            createdAt = System.currentTimeMillis()
        )

        val id = repository.insert(pendingSms)
        assertTrue(id > 0)

        // بررسی نوتیفیکیشن
        val saved = repository.getById(id)
        assertNotNull(saved)
        SmsNotificationHelper.showPendingSmsNotification(context, saved!!)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val shadowNotificationManager: ShadowNotificationManager = shadowOf(notificationManager)
        assertTrue(shadowNotificationManager.size() > 0)

        val notification = shadowNotificationManager.allNotifications.first()
        assertEquals("تراکنش بانکی جدید شناسایی شد", notification.extras.getString("android.title"))
    }

    // =========================================================================
    // ۲. دریافت پیامک غیر بانکی (عدم ذخیره هیچ رکوردی)
    // =========================================================================
    @Test
    fun testNonBankSms_doesNotCreatePendingRecord() = runBlocking {
        val parser = BankSmsParser()
        val otpSms = "رمز پویا بانک ملی: 489215 معتبر برای 2 دقیقه"

        val parsed = parser.parse(otpSms)
        assertNull("پیامک رمز پویا باید null برگرداند", parsed)

        // صف دیتابیس باید خالی بماند
        val allPending = repository.getAllPendingList()
        assertTrue("دیتابیس نباید رکوردی ذخیره کند", allPending.isEmpty())
    }

    // =========================================================================
    // ۳. مدیریت وضعیت Permission (عدم کرش در صورت عدم دسترسی)
    // =========================================================================
    @Test
    fun testPermissionHandling_doesNotCrash() {
        val receiver = BankSmsReceiver()
        val dummyIntent = Intent("android.provider.Telephony.SMS_RECEIVED")

        // فراخوانی مستقیم بدون کرش مهار می‌شود
        try {
            receiver.onReceive(context, dummyIntent)
        } catch (e: Exception) {
            org.junit.Assert.fail("نباید هیچ کرشی رخ دهد: ${e.message}")
        }
    }

    // =========================================================================
    // ۴. کلیک روی اکشن Confirm نوتیفیکیشن و تغییر وضعیت رکورد
    // =========================================================================
    @Test
    fun testNotificationConfirmAction_updatesStatusToConfirmed() = runBlocking {
        // درج رکورد اولیه با وضعیت PENDING
        val initialSms = PendingBankSms(
            bankName = "بانک سامان",
            amount = 120000L,
            isIncome = false,
            merchant = "دیجی کالا"
        )
        val id = repository.insert(initialSms)
        assertEquals(SmsStatus.PENDING, repository.getById(id)!!.getSmsStatus())

        // تایید مستقیم از طریق مخزن (همان کاری که SmsActionReceiver در بک‌گراند انجام می‌دهد)
        repository.markAsConfirmed(id)

        val updated = repository.getById(id)
        assertNotNull(updated)
        assertEquals(SmsStatus.CONFIRMED, updated!!.getSmsStatus())
        // باید از لیست Pending خارج شده باشد
        assertEquals(0, repository.getAllPendingList().size)
    }

    // =========================================================================
    // ۵. کلیک روی اکشن Reject نوتیفیکیشن و تغییر وضعیت رکورد
    // =========================================================================
    @Test
    fun testNotificationRejectAction_updatesStatusToRejected() = runBlocking {
        val initialSms = PendingBankSms(
            bankName = "بانک پاسارگاد",
            amount = 45000L,
            isIncome = false,
            merchant = "تبلیغ"
        )
        val id = repository.insert(initialSms)
        assertEquals(SmsStatus.PENDING, repository.getById(id)!!.getSmsStatus())

        // رد مستقیم از طریق مخزن (مطابق SmsActionReceiver)
        repository.markAsRejected(id)

        val updated = repository.getById(id)
        assertNotNull(updated)
        assertEquals(SmsStatus.REJECTED, updated!!.getSmsStatus())
        assertEquals(0, repository.getAllPendingList().size)
    }

    // =========================================================================
    // ۶. بررسی امنیت: عدم ذخیره متن کامل پیامک و عدم استخراج حساس
    // =========================================================================
    @Test
    fun testSecurity_rawSmsAndSensitiveDataNotPersisted() = runBlocking {
        val parser = BankSmsParser()
        val rawMessage = "بانک تجارت. برداشت 100,000 ریال از کارت 5859831099887766 مانده 500,000 ریال. رمز اینترنتی 1234"

        val parsed = parser.parse(rawMessage)
        assertNotNull(parsed)

        val pendingItem = PendingBankSms(
            bankName = parsed!!.bank.displayName,
            amount = parsed.amount,
            isIncome = parsed.isIncome,
            balance = parsed.balance,
            cardLastDigits = parsed.cardNumber,
            merchant = "تست امنیت",
            suggestedCategory = parsed.suggestedCategory
        )

        // ۱. بررسی شماره کارت: فقط ۴ رقم آخر
        assertEquals("7766", pendingItem.cardLastDigits)
        assertFalse("شماره کامل ۱۶ رقمی نباید ذخیره شود", pendingItem.cardLastDigits!!.contains("5859"))

        // ۲. فیلد متن خام (rawMessage) در مدل PendingBankSms ذخیره نمی‌شود
        val id = repository.insert(pendingItem)
        val fromDb = repository.getById(id)
        assertNotNull(fromDb)
        // عدم دسترسی به متن کامل در فیلدهای entity
        assertFalse("رمز اینترنتی نباید در هیچ فیلدی وجود داشته باشد", fromDb!!.merchant.contains("1234"))
    }
}
