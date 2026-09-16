package com.example

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.analyzer.ExpenseAnalyzer
import com.example.backup.BackupRestoreRepository
import com.example.backup.GoogleDriveServiceHelper
import com.example.data.AppDatabase
import com.example.data.CategoryRepository
import com.example.data.RecurrencePeriod
import com.example.data.RecurringTransaction
import com.example.data.RecurringTransactionRepository
import com.example.data.SavingsGoal
import com.example.data.SavingsGoalRepository
import com.example.data.Transaction
import com.example.data.TransactionRepository
import com.example.report.CsvGenerator
import com.example.security.SecurityPreferencesManager
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
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PreReleaseCheckTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5)
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    // =========================================================================
    // ۱. تست پایداری و صحت قابلیت‌های قبلی (Legacy Features Regression Check)
    // =========================================================================
    @Test
    fun testLegacyFeatures_transactionsAnalysisAndCsv() = runBlocking {
        val txDao = db.transactionDao()
        val txRepo = TransactionRepository(txDao)
        val analyzer = ExpenseAnalyzer()

        // ۱. ثبت تراکنش‌های درآمد و هزینه
        val now = System.currentTimeMillis()
        val tx1 = Transaction(id = 1, amount = 25000000L, category = "حقوق", description = "حقوق شهریور", isIncome = true, date = now - 1000)
        val tx2 = Transaction(id = 2, amount = 4000000L, category = "خوراک", description = "خرید ماهانه", isIncome = false, date = now)
        val tx3 = Transaction(id = 3, amount = 1000000L, category = "حمل و نقل", description = "بنزین و اسنپ", isIncome = false, date = now)

        txRepo.insert(tx1)
        txRepo.insert(tx2)
        txRepo.insert(tx3)

        val allTx = txRepo.allTransactions.first()
        assertEquals(3, allTx.size)

        // ۲. تست موتور تحلیل پردازش گفتار/متن صوتی (ExpenseAnalyzer)
        val voiceResult1 = analyzer.analyze("پنجاه هزار تومن بنزین زدم")
        assertEquals(50000L, voiceResult1.amount)
        assertEquals("حمل‌ونقل", voiceResult1.category)
        assertFalse(voiceResult1.isIncome)

        val voiceResult2 = analyzer.analyze("واریزی حقوق 30 میلیون تومان")
        assertEquals(30000000L, voiceResult2.amount)
        assertEquals("درآمد", voiceResult2.category)
        assertTrue(voiceResult2.isIncome)

        // ۳. تست تولید خروجی گزارش CSV
        val csvFile: File? = CsvGenerator.createExpenseReportCsv(context, allTx)
        assertNotNull("فایل CSV باید با موفقیت ایجاد شود", csvFile)
        assertTrue(csvFile!!.exists())
        val csvContent = csvFile.readText()
        assertTrue("فایل CSV باید دارای هدر تراکنش‌ها باشد", csvContent.contains("شناسه") && csvContent.contains("مبلغ"))
        assertTrue("فایل CSV باید شامل اطلاعات تراکنش‌ها باشد", csvContent.contains("حقوق شهریور"))
        assertTrue("فایل CSV باید شامل هزینه خوراک باشد", csvContent.contains("خرید ماهانه"))
    }

    // =========================================================================
    // ۲. تست مهاجرت دیتابیس بدون از دست رفتن داده‌های کاربر (Database Migration Check)
    // =========================================================================
    @Test
    fun testRoomDatabaseMigrations_preservesExistingData() {
        val testDbName = "migration_test_db"
        context.deleteDatabase(testDbName)

        // ۱. ایجاد دیتابیس نسخه ۱ با جدول اصلی transactions به صورت خام
        val helperFactory = FrameworkSQLiteOpenHelperFactory()
        val config = androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(testDbName)
            .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
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
                }

                override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val helper = helperFactory.create(config)
        val writableDb = helper.writableDatabase

        // درج داده در نسخه ۱
        writableDb.execSQL("INSERT INTO transactions (id, amount, category, date, description, isIncome) VALUES (1, 150000, 'غذا', 1700000000000, 'ناهار کاری', 0)")
        writableDb.close()
        helper.close()

        // ۲. باز کردن دیتابیس با AppDatabase نسخه ۵ و اعمال اتوماتیک Migration ها (1->2->3->4->5)
        val upgradedDb = Room.databaseBuilder(context, AppDatabase::class.java, testDbName)
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5)
            .allowMainThreadQueries()
            .build()

        // ۳. بررسی دست‌نخورده ماندن داده‌ها
        runBlocking {
            val list = upgradedDb.transactionDao().getAllTransactionsList()
            assertEquals("تراکنش‌های کاربر نباید پس از مایگریشن حذف شوند", 1, list.size)
            assertEquals("ناهار کاری", list[0].description)
            assertEquals(150000L, list[0].amount)

            // ۴. بررسی وجود و سلامت جداول جدید مایگریشن شده
            val goals = upgradedDb.savingsGoalDao().getAllGoalsList()
            assertTrue(goals.isEmpty())

            val recurring = upgradedDb.recurringTransactionDao().getAllList()
            assertTrue(recurring.isEmpty())

            val categories = upgradedDb.customCategoryDao().getAllCategoriesList()
            assertTrue(categories.isEmpty())
        }

        upgradedDb.close()
        context.deleteDatabase(testDbName)
    }

    // =========================================================================
    // ۳. تست سفر کامل کاربر (End-to-End User Journey)
    // =========================================================================
    @Test
    fun testCompleteUserJourney_fromInstallToRestore() = runBlocking {
        val txRepo = TransactionRepository(db.transactionDao())
        val savingsRepo = SavingsGoalRepository(db.savingsGoalDao())
        val recurringRepo = RecurringTransactionRepository(db.recurringTransactionDao())
        val catRepo = CategoryRepository(db.customCategoryDao())
        val security = SecurityPreferencesManager(context)
        val driveHelper = GoogleDriveServiceHelper(context)
        val backupRepo = BackupRestoreRepository(
            database = db,
            transactionDao = db.transactionDao(),
            savingsGoalDao = db.savingsGoalDao(),
            recurringTransactionDao = db.recurringTransactionDao(),
            customCategoryDao = db.customCategoryDao(),
            driveHelper = driveHelper
        )

        // ۱. مرحله اول: نصب و شروع برنامه (تنظیمات اولیه امنیتی)
        security.clearSecurityData()
        security.savePin("4455")
        assertTrue("قفل باید فعال باشد", security.isAppLockEnabled.first())
        assertTrue("پین صحیح باید تایید شود", security.verifyPin("4455"))

        // ۲. مرحله دوم: ثبت تراکنش‌ها
        val txId = txRepo.insert(
            Transaction(id = 10, amount = 3200000L, category = "پوشاک", description = "خرید کفش", date = System.currentTimeMillis(), isIncome = false)
        )
        assertTrue(txId > 0)

        // ۳. مرحله سوم: تعریف هدف پس‌انداز و واریز
        val goalId = savingsRepo.insert(
            SavingsGoal(id = 20, title = "سفر یزد", targetAmount = 8000000L, savedAmount = 2000000L)
        )
        savingsRepo.addDeposit(goalId, 6000000L)
        val completedGoal = db.savingsGoalDao().getGoalById(goalId)
        assertNotNull(completedGoal)
        assertTrue("هدف باید ۱۰۰٪ تکمیل شده باشد", completedGoal!!.isCompleted)

        // ۴. مرحله چهارم: ثبت تراکنش دوره‌ای
        val recId = recurringRepo.insert(
            RecurringTransaction(
                id = 30,
                title = "اینترنت ثابت",
                amount = 250000L,
                category = "قبوض",
                period = RecurrencePeriod.MONTHLY.name,
                startDate = System.currentTimeMillis(),
                nextExecutionDate = System.currentTimeMillis() + 86400000L,
                isActive = true
            )
        )
        assertTrue(recId > 0)

        // ۵. مرحله پنجم: تهیه پشتیبان ابری
        val backupResult = backupRepo.createAndUploadBackup(
            budgetLimit = 15000000L,
            isCurrencyInRial = false,
            selectedPeriod = "ماهانه",
            isVip = true
        )
        assertTrue("بکاپ ابری باید موفق باشد", backupResult.isSuccess)

        // ۶. مرحله ششم: شبیه‌سازی پاک شدن داده‌ها (Re-install / New Device)
        db.clearAllTables()
        assertEquals(0, db.transactionDao().getAllTransactionsList().size)
        assertEquals(0, db.savingsGoalDao().getAllGoalsList().size)

        // ۷. مرحله هفتم: بازیابی کامل اطلاعات از کلود
        val restoreResult = backupRepo.restoreBackup()
        assertTrue("بازیابی باید موفق باشد", restoreResult.isSuccess)

        val restoredTx = db.transactionDao().getAllTransactionsList()
        assertEquals(1, restoredTx.size)
        assertEquals("خرید کفش", restoredTx[0].description)

        val restoredGoals = db.savingsGoalDao().getAllGoalsList()
        assertEquals(1, restoredGoals.size)
        assertEquals("سفر یزد", restoredGoals[0].title)
        assertEquals(8000000L, restoredGoals[0].savedAmount)

        val restoredRec = db.recurringTransactionDao().getAllList()
        assertEquals(1, restoredRec.size)
        assertEquals("اینترنت ثابت", restoredRec[0].title)
    }
}
