package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.backup.BackupCryptoManager
import com.example.backup.BackupRestoreRepository
import com.example.backup.GoogleDriveServiceHelper
import com.example.data.AppDatabase
import com.example.data.CustomCategory
import com.example.data.CustomCategoryDao
import com.example.data.RecurrencePeriod
import com.example.data.RecurringTransaction
import com.example.data.RecurringTransactionDao
import com.example.data.RecurringTransactionRepository
import com.example.data.SavingsGoal
import com.example.data.SavingsGoalDao
import com.example.data.SavingsGoalRepository
import com.example.data.Transaction
import com.example.data.TransactionDao
import com.example.data.TransactionRepository
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

/**
 * آزمون‌های یکپارچه و ارزیابی کیفی سناریوی کاربر (User Journey QA)
 * برای ۴ قابلیت اصلی:
 * ۱. Savings Goals (اهداف پس‌انداز)
 * ۲. Recurring Transactions (تراکنش‌های دوره‌ای)
 * ۳. Security & Biometric (قفل امنیتی و بیومتریک)
 * ۴. Cloud Backup & Restore (پشتیبان‌گیری و بازیابی ابری)
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UserJourneyQATest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var transactionDao: TransactionDao
    private lateinit var savingsGoalDao: SavingsGoalDao
    private lateinit var recurringDao: RecurringTransactionDao
    private lateinit var customCategoryDao: CustomCategoryDao

    private lateinit var transactionRepo: TransactionRepository
    private lateinit var savingsGoalRepo: SavingsGoalRepository
    private lateinit var recurringRepo: RecurringTransactionRepository
    private lateinit var securityManager: SecurityPreferencesManager
    private lateinit var driveHelper: GoogleDriveServiceHelper
    private lateinit var backupRestoreRepo: BackupRestoreRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        transactionDao = db.transactionDao()
        savingsGoalDao = db.savingsGoalDao()
        recurringDao = db.recurringTransactionDao()
        customCategoryDao = db.customCategoryDao()

        transactionRepo = TransactionRepository(transactionDao)
        savingsGoalRepo = SavingsGoalRepository(savingsGoalDao)
        recurringRepo = RecurringTransactionRepository(recurringDao)
        securityManager = SecurityPreferencesManager(context)
        driveHelper = GoogleDriveServiceHelper(context)

        backupRestoreRepo = BackupRestoreRepository(
            database = db,
            transactionDao = transactionDao,
            savingsGoalDao = savingsGoalDao,
            recurringTransactionDao = recurringDao,
            customCategoryDao = customCategoryDao,
            driveHelper = driveHelper
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    // =========================================================================
    // ۱. سناریوی عملی کاربر: اهداف پس‌انداز (Savings Goals QA Scenario)
    // =========================================================================
    @Test
    fun qaScenario_savingsGoals_fullUserLifecycle() = runBlocking {
        // مرحله ۱: کاربر هدف جدیدی برای «خرید گوشی» با مبلغ ۵۰ میلیون تومان تعریف می‌کند
        val newGoal = SavingsGoal(
            title = "خرید گوشی هوشمند",
            targetAmount = 50000000L,
            savedAmount = 0L,
            targetDate = System.currentTimeMillis() + 90L * 86400000L, // ۳ ماه بعد
            note = "مدل پرچمدار برای کار",
            iconName = "Smartphone",
            colorHex = "#0D5C46"
        )
        val goalId = savingsGoalRepo.insert(newGoal)
        assertTrue("شناسه هدف پس‌انداز باید مثبت و معتبر باشد", goalId > 0)

        // مرحله ۲: بررسی نمایش در لیست اهداف و مقادیر اولیه
        val goalsList = savingsGoalRepo.allGoals.first()
        assertEquals("باید دقیقاً یک هدف در لیست باشد", 1, goalsList.size)
        val created = goalsList.first()
        assertEquals("خرید گوشی هوشمند", created.title)
        assertEquals(0L, created.savedAmount)
        assertEquals(0, created.progressPercent)
        assertEquals(50000000L, created.remainingAmount)
        assertFalse("هدف در ابتدا نباید تکمیل شده باشد", created.isCompleted)

        // مرحله ۳: واریز مرحله اول (۲۰ میلیون تومان)
        savingsGoalRepo.addDeposit(goalId, 20000000L)
        val afterDeposit1 = savingsGoalDao.getGoalById(goalId)
        assertNotNull(afterDeposit1)
        assertEquals(20000000L, afterDeposit1!!.savedAmount)
        assertEquals(40, afterDeposit1.progressPercent) // ۲۰ از ۵۰ = ۴۰ درصد
        assertEquals(30000000L, afterDeposit1.remainingAmount)
        assertFalse(afterDeposit1.isCompleted)

        // مرحله ۴: واریز مرحله دوم برای تکمیل هدف (۳۰ میلیون تومان)
        savingsGoalRepo.addDeposit(goalId, 30000000L)
        val afterDeposit2 = savingsGoalDao.getGoalById(goalId)
        assertNotNull(afterDeposit2)
        assertEquals(50000000L, afterDeposit2!!.savedAmount)
        assertEquals(100, afterDeposit2.progressPercent)
        assertEquals(0L, afterDeposit2.remainingAmount)
        assertTrue("هدف باید ۱۰۰٪ تکمیل شده باشد", afterDeposit2.isCompleted)

        // مرحله ۵: حذف هدف توسط کاربر
        savingsGoalRepo.delete(afterDeposit2)
        val finalGoals = savingsGoalRepo.allGoals.first()
        assertTrue("پس از حذف، لیست اهداف باید خالی باشد", finalGoals.isEmpty())
    }

    // =========================================================================
    // ۲. سناریوی عملی کاربر: تراکنش‌های دوره‌ای (Recurring Transactions QA Scenario)
    // =========================================================================
    @Test
    fun qaScenario_recurringTransactions_detectionAndAutoExecution() = runBlocking {
        val now = System.currentTimeMillis()

        // مرحله ۱: کاربر یک تراکنش دوره‌ای ماهانه برای «حقوق پرسنل» سررسید شده در زمان حال تعریف می‌کند
        val dueItem = RecurringTransaction(
            title = "حقوق پرسنل",
            amount = 18000000L,
            isIncome = false,
            category = "حقوق و دستمزد",
            period = RecurrencePeriod.MONTHLY.name,
            startDate = now - 100000L,
            nextExecutionDate = now - 5000L, // سررسید شده در گذشته نزدیک
            isActive = true,
            note = "پرداخت انتهای ماه"
        )
        val recId = recurringRepo.insert(dueItem)
        assertTrue(recId > 0)

        // مرحله ۲: بررسی تشخیص خودکار تراکنش‌های موعد رسیده
        val dueList = recurringRepo.getDueTransactions(now)
        assertEquals("باید یک تراکنش سررسید شده تشخیص داده شود", 1, dueList.size)
        val due = dueList.first()
        assertEquals("حقوق پرسنل", due.title)

        // مرحله ۳: ثبت عملیات به عنوان یک تراکنش واقعی در جدول transactions
        val actualTx = Transaction(
            amount = due.amount,
            category = due.category,
            description = "${due.title} (ثبت خودکار دوره‌ای)",
            isIncome = due.isIncome,
            date = now
        )
        val txId = transactionRepo.insert(actualTx)
        assertTrue("تراکنش واقعی باید با موفقیت درج شود", txId > 0)

        // مرحله ۴: بروزرسانی موعد سررسید بعدی به ماه آینده
        val nextDate = RecurringTransactionRepository.calculateNextDate(due.nextExecutionDate, due.recurrencePeriod)
        assertTrue("تاریخ سررسید بعدی باید بزرگتر از سررسید قبلی باشد", nextDate > due.nextExecutionDate)
        recurringRepo.updateExecutionDates(due.id, nextDate, now)

        // مرحله ۵: اطمینان از اینکه دیگر تراکنش سررسید شده‌ای وجود ندارد
        val dueAfter = recurringRepo.getDueTransactions(now)
        assertEquals("نباید دیگر تراکنش معوقه‌ای وجود داشته باشد", 0, dueAfter.size)

        // مرحله ۶: غیرفعال‌سازی موقت توسط کاربر
        recurringRepo.setActiveState(recId, false)
        val updatedRec = recurringRepo.allRecurringTransactions.first().first { it.id == recId }
        assertFalse("تراکنش دوره‌ای باید غیرفعال شده باشد", updatedRec.isActive)
    }

    // =========================================================================
    // ۳. سناریوی عملی کاربر: امنیت، هش و سالت رمز عبور (Security QA Scenario)
    // =========================================================================
    @Test
    fun qaScenario_securityAndPinLock_verificationAndPersistence() = runBlocking {
        // مرحله ۱: در ابتدا هیچ پینی تنظیم نشده است
        securityManager.clearSecurityData()

        // مرحله ۲: کاربر یک پین جدید ۴ رقمی «7391» ذخیره می‌کند
        val pin = "7391"
        securityManager.savePin(pin)

        // مرحله ۳: بررسی فعال شدن قفل و عدم ذخیره‌سازی متن خام پین
        val isEnabled = securityManager.isAppLockEnabled.first()
        assertTrue("قفل برنامه باید فعال شده باشد", isEnabled)
        val hasPin = securityManager.hasPinSet.first()
        assertTrue("پین باید تنظیم شده باشد", hasPin)

        // مرحله ۴: کاربر با پین نادرست «1234» تلاش می‌کند
        val wrongResult = securityManager.verifyPin("1234")
        assertFalse("پین اشتباه باید رد شود", wrongResult)

        // مرحله ۵: کاربر با پین صحیح وارد می‌شود
        val correctResult = securityManager.verifyPin("7391")
        assertTrue("پین صحیح باید تایید شود", correctResult)

        // مرحله ۶: بررسی مقاومت در برابر Rainbow Table (بررسی تولید سالت تصادفی)
        val salt1 = SecurityPreferencesManager.generateSalt()
        val salt2 = SecurityPreferencesManager.generateSalt()
        org.junit.Assert.assertNotEquals("سالت‌ها نباید یکسان باشند", salt1, salt2)

        // مرحله ۷: غیرفعال‌سازی قفل توسط کاربر
        securityManager.clearSecurityData()
        val isEnabledAfter = securityManager.isAppLockEnabled.first()
        assertFalse("پس از غیرفعال‌سازی، قفل باید خاموش باشد", isEnabledAfter)
    }

    // =========================================================================
    // ۴. سناریوی عملی کاربر: بکاپ و بازیابی ابری (Cloud Backup & Restore QA)
    // =========================================================================
    @Test
    fun qaScenario_cloudBackupAndRestore_endToEndFlow() = runBlocking {
        // مرحله ۱: ایجاد داده‌های مالی واقعی توسط کاربر
        transactionRepo.insert(
            Transaction(id = 101, amount = 450000L, category = "خرید", description = "خرید هفتگی", date = System.currentTimeMillis(), isIncome = false)
        )
        savingsGoalRepo.insert(
            SavingsGoal(id = 201, title = "تعمیرات خودرو", targetAmount = 15000000L, savedAmount = 5000000L)
        )
        recurringRepo.insert(
            RecurringTransaction(
                id = 301,
                title = "شارژ ساختمان",
                amount = 600000L,
                category = "مسکن",
                period = RecurrencePeriod.MONTHLY.name,
                startDate = System.currentTimeMillis(),
                nextExecutionDate = System.currentTimeMillis() + 86400000L,
                isActive = true
            )
        )
        customCategoryDao.insertCategory(
            CustomCategory(id = 401, name = "تفریح و سفر", iconName = "flight", colorHex = "#FF5722", isIncome = false)
        )

        // مرحله ۲: کاربر نسخه پشتیبان ابری تهیه می‌کند
        val backupResult = backupRestoreRepo.createAndUploadBackup(
            budgetLimit = 20000000L,
            isCurrencyInRial = false,
            selectedPeriod = "ماهانه",
            isVip = true
        )
        assertTrue("پشتیبان‌گیری ابری باید با موفقیت انجام شود", backupResult.isSuccess)
        val meta = backupResult.getOrThrow()
        assertEquals(1, meta.transactionCount)
        assertTrue(meta.sizeBytes > 0)

        // مرحله ۳: سناریوی بحرانی - حذف اتفاقی داده‌ها از دستگاه یا تعویض گوشی
        db.clearAllTables()

        assertEquals(0, transactionDao.getAllTransactionsList().size)
        assertEquals(0, savingsGoalDao.getAllGoalsList().size)
        assertEquals(0, recurringDao.getAllList().size)
        assertEquals(0, customCategoryDao.getAllCategoriesList().size)

        // مرحله ۴: کاربر نسخه پشتیبان ابری خود را بازیابی می‌کند
        val restoreResult = backupRestoreRepo.restoreBackup()
        assertTrue("بازیابی ابری باید با موفقیت کامل انجام شود", restoreResult.isSuccess)
        val restoredPayload = restoreResult.getOrThrow()

        // مرحله ۵: اعتبارسنجی دقیق داده‌های بازیابی‌شده در پایگاه‌داده
        val restoredTransactions = transactionDao.getAllTransactionsList()
        assertEquals("تراکنش باید به درستی بازگردانده شده باشد", 1, restoredTransactions.size)
        assertEquals("خرید هفتگی", restoredTransactions[0].description)
        assertEquals(450000L, restoredTransactions[0].amount)

        val restoredGoals = savingsGoalDao.getAllGoalsList()
        assertEquals("هدف پس‌انداز باید بازگردانده شده باشد", 1, restoredGoals.size)
        assertEquals("تعمیرات خودرو", restoredGoals[0].title)
        assertEquals(15000000L, restoredGoals[0].targetAmount)
        assertEquals(5000000L, restoredGoals[0].savedAmount)

        val restoredRecurring = recurringDao.getAllList()
        assertEquals("تراکنش دوره‌ای باید بازگردانده شده باشد", 1, restoredRecurring.size)
        assertEquals("شارژ ساختمان", restoredRecurring[0].title)

        val restoredCategories = customCategoryDao.getAllCategoriesList()
        assertEquals("دسته‌بندی اختصاصی باید بازگردانده شده باشد", 1, restoredCategories.size)
        assertEquals("تفریح و سفر", restoredCategories[0].name)

        assertEquals(20000000L, restoredPayload.appPreferences.budgetLimit)
        assertTrue(restoredPayload.appPreferences.isVip)
    }
}
