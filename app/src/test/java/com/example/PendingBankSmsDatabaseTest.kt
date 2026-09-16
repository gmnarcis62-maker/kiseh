package com.example

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.PendingBankSms
import com.example.data.PendingBankSmsRepository
import com.example.data.SmsStatus
import com.example.data.Transaction
import com.example.data.TransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
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
class PendingBankSmsDatabaseTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: PendingBankSmsRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
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
    // ۱. ایجاد پیامک بانکی، ذخیره در دیتابیس و دریافت از لیست Pending
    // =========================================================================
    @Test
    fun testInsertAndGetPendingSms() = runBlocking {
        val sms = PendingBankSms(
            bankName = "بانک ملت",
            amount = 120000L,
            isIncome = false,
            balance = 4500000L,
            cardLastDigits = "4589",
            merchant = "اسنپ",
            suggestedCategory = "حمل و نقل"
        )

        val insertedId = repository.insert(sms)
        assertTrue("شناسه ثبت شده باید مثبت باشد", insertedId > 0)

        val pendingList = repository.allPendingSms.first()
        assertEquals(1, pendingList.size)
        val retrieved = pendingList[0]
        assertEquals("بانک ملت", retrieved.bankName)
        assertEquals(120000L, retrieved.amount)
        assertEquals(false, retrieved.isIncome)
        assertEquals(4500000L, retrieved.balance)
        assertEquals("4589", retrieved.cardLastDigits)
        assertEquals("اسنپ", retrieved.merchant)
        assertEquals("حمل و نقل", retrieved.suggestedCategory)
        assertEquals(SmsStatus.PENDING, retrieved.getSmsStatus())
    }

    // =========================================================================
    // ۲. تغییر وضعیت پیامک به CONFIRMED (خروج خودکار از لیست Pending)
    // =========================================================================
    @Test
    fun testConfirmPendingSms() = runBlocking {
        val sms = PendingBankSms(
            bankName = "بانک ملی",
            amount = 250000L,
            isIncome = false,
            merchant = "سوپر مارکت",
            suggestedCategory = "خوراک"
        )

        val id = repository.insert(sms)
        assertEquals(1, repository.getAllPendingList().size)

        // تایید پیامک
        repository.markAsConfirmed(id)

        // پس از تایید نباید در لیست PENDING باشد
        val pendingList = repository.getAllPendingList()
        assertEquals(0, pendingList.size)

        // بررسی مستقیم رکورد در دیتابیس
        val updated = repository.getById(id)
        assertNotNull(updated)
        assertEquals(SmsStatus.CONFIRMED, updated!!.getSmsStatus())
    }

    // =========================================================================
    // ۳. تغییر وضعیت به REJECTED و حذف پیامک رد شده
    // =========================================================================
    @Test
    fun testRejectAndDeletePendingSms() = runBlocking {
        val sms = PendingBankSms(
            bankName = "بانک سامان",
            amount = 50000L,
            isIncome = false,
            merchant = "تبلیغاتی",
            suggestedCategory = "سایر"
        )

        val id = repository.insert(sms)
        repository.markAsRejected(id)

        val rejected = repository.getById(id)
        assertNotNull(rejected)
        assertEquals(SmsStatus.REJECTED, rejected!!.getSmsStatus())

        // حذف پیامک
        repository.delete(rejected)
        assertNull(repository.getById(id))
    }

    // =========================================================================
    // ۴. بررسی Migration نسخه ۴ به ۵ و حفظ کامل اطلاعات قدیمی
    // =========================================================================
    @Test
    fun testMigration4to5_preservesAllExistingData() {
        val testDbName = "migration_4_to_5_test_db"
        context.deleteDatabase(testDbName)

        // ایجاد دیتابیس نسخه ۴ به صورت مستقیم با تمام جداول قبلی
        val helperFactory = FrameworkSQLiteOpenHelperFactory()
        val config = androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(testDbName)
            .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(4) {
                override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    // جدول تراکنش‌ها
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `transactions` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `amount` INTEGER NOT NULL,
                            `category` TEXT NOT NULL,
                            `date` INTEGER NOT NULL,
                            `description` TEXT NOT NULL,
                            `isIncome` INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )
                    // جدول اهداف پس‌انداز
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `savings_goals` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `title` TEXT NOT NULL,
                            `targetAmount` INTEGER NOT NULL,
                            `savedAmount` INTEGER NOT NULL,
                            `targetDate` INTEGER,
                            `note` TEXT NOT NULL,
                            `iconName` TEXT NOT NULL,
                            `colorHex` TEXT NOT NULL,
                            `createdAt` INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )
                    // جدول تراکنش‌های دوره‌ای
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `recurring_transactions` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `title` TEXT NOT NULL,
                            `amount` INTEGER NOT NULL,
                            `isIncome` INTEGER NOT NULL,
                            `category` TEXT NOT NULL,
                            `period` TEXT NOT NULL,
                            `startDate` INTEGER NOT NULL,
                            `endDate` INTEGER,
                            `nextExecutionDate` INTEGER NOT NULL,
                            `lastExecutedDate` INTEGER,
                            `isActive` INTEGER NOT NULL,
                            `note` TEXT NOT NULL,
                            `createdAt` INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )
                    // جدول دسته‌بندی‌های سفارشی
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `custom_categories` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `name` TEXT NOT NULL,
                            `iconName` TEXT NOT NULL,
                            `colorHex` TEXT NOT NULL,
                            `isIncome` INTEGER NOT NULL,
                            `isDefault` INTEGER NOT NULL,
                            `createdAt` INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )
                }

                override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val helper = helperFactory.create(config)
        val writableDb = helper.writableDatabase

        // درج رکورد تراکنش در نسخه ۴
        writableDb.execSQL("INSERT INTO transactions (id, amount, category, date, description, isIncome) VALUES (101, 850000, 'پوشاک', 1700000000000, 'خرید پیراهن', 0)")
        writableDb.close()
        helper.close()

        // باز کردن دیتابیس با نسخه ۵ و اعمال MIGRATION_4_5
        val upgradedDb = Room.databaseBuilder(context, AppDatabase::class.java, testDbName)
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5
            )
            .allowMainThreadQueries()
            .build()

        runBlocking {
            // ۱. بررسی دست‌نخورده ماندن تراکنش‌های قدیمی
            val transactions = upgradedDb.transactionDao().getAllTransactionsList()
            assertEquals(1, transactions.size)
            assertEquals("خرید پیراهن", transactions[0].description)
            assertEquals(850000L, transactions[0].amount)

            // ۲. بررسی وجود و سلامت جدول جدید pending_bank_sms
            val pendingDao = upgradedDb.pendingBankSmsDao()
            val initialPendingList = pendingDao.getAllList()
            assertTrue("جدول جدید باید در ابتدا خالی باشد", initialPendingList.isEmpty())

            // ۳. تست درج و واکشی در جدول جدید مایگریشن شده
            val newSmsId = pendingDao.insert(
                PendingBankSms(
                    bankName = "بانک پاسارگاد",
                    amount = 300000L,
                    isIncome = false,
                    merchant = "کافه کارما"
                )
            )
            assertTrue(newSmsId > 0)
            assertEquals(1, pendingDao.getAllPendingList().size)
        }

        upgradedDb.close()
        context.deleteDatabase(testDbName)
    }
}
