package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.backup.AppPreferencesBackup
import com.example.backup.BackupCryptoManager
import com.example.backup.BackupMetadata
import com.example.backup.BackupRestoreRepository
import com.example.backup.GoogleDriveServiceHelper
import com.example.backup.KisehBackupPayload
import com.example.data.AppDatabase
import com.example.data.CustomCategory
import com.example.data.CustomCategoryDao
import com.example.data.RecurrencePeriod
import com.example.data.RecurringTransaction
import com.example.data.RecurringTransactionDao
import com.example.data.SavingsGoal
import com.example.data.SavingsGoalDao
import com.example.data.Transaction
import com.example.data.TransactionDao
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * تست‌های جامع و اعتبارسنجی معماری جریان Backup و Restore ابری گوگل درایو
 * مطابق با الزامات مرحله ۹.۱
 */
@RunWith(RobolectricTestRunner::class)
class GoogleDriveBackupRestoreFlowTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var transactionDao: TransactionDao
    private lateinit var savingsGoalDao: SavingsGoalDao
    private lateinit var recurringDao: RecurringTransactionDao
    private lateinit var customCategoryDao: CustomCategoryDao
    private lateinit var driveHelper: GoogleDriveServiceHelper
    private lateinit var repository: BackupRestoreRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        transactionDao = db.transactionDao()
        savingsGoalDao = db.savingsGoalDao()
        recurringDao = db.recurringTransactionDao()
        customCategoryDao = db.customCategoryDao()
        driveHelper = GoogleDriveServiceHelper(context)

        // ثبت حساب آزمایشی برای مجاز بودن دسترسی گوگل
        driveHelper.saveAccount("test.kiseh.user@gmail.com", "کاربر تستی")

        repository = BackupRestoreRepository(
            database = db,
            transactionDao = transactionDao,
            savingsGoalDao = savingsGoalDao,
            recurringTransactionDao = recurringDao,
            customCategoryDao = customCategoryDao,
            driveHelper = driveHelper,
            context = context
        )
    }

    @After
    fun tearDown() {
        db.close()
        driveHelper.signOut()
    }

    private suspend fun populateSampleData() {
        val tx1 = Transaction(id = 101, amount = 750000, category = "خرید سوپرمارکت", description = "خرید پروتئین", isIncome = false, date = 1700000000000L)
        val tx2 = Transaction(id = 102, amount = 12000000, category = "حقوق و دستمزد", description = "واریز حقوق", isIncome = true, date = 1700000100000L)
        transactionDao.insertAll(listOf(tx1, tx2))

        val goal1 = SavingsGoal(id = 201, title = "تعویض خودرو", targetAmount = 300000000, savedAmount = 80000000, targetDate = 1790000000000L)
        savingsGoalDao.insertAll(listOf(goal1))

        val rec1 = RecurringTransaction(
            id = 301,
            title = "بیمه تکمیلی",
            amount = 950000,
            category = "درمان",
            period = RecurrencePeriod.MONTHLY.name,
            startDate = 1700000000000L,
            nextExecutionDate = 1702592000000L,
            isActive = true,
            isIncome = false,
            note = "کسر خودکار"
        )
        recurringDao.insertAll(listOf(rec1))

        val cat1 = CustomCategory(id = 401, name = "ورزش و تندرستی", iconName = "fitness_center", colorHex = "#4CAF50", isIncome = false)
        customCategoryDao.insertAll(listOf(cat1))
    }

    // ۱. GoogleDriveBackupCreateTest: تست ایجاد فایل بکاپ و متادیتا با فرمت استاندارد
    @Test
    fun testGoogleDriveBackupCreateTest() = runBlocking {
        populateSampleData()

        val result = repository.createAndUploadBackup(
            budgetLimit = 15000000L,
            isCurrencyInRial = false,
            selectedPeriod = "ماهانه",
            isVip = true
        )

        assertTrue("عملیات ایجاد بکاپ باید با موفقیت خاتمه یابد", result.isSuccess)
        val metadata = result.getOrThrow()
        assertNotNull(metadata)
        assertTrue("نام فایل باید با پیشوند Backup_ و پسوند .hpb باشد", metadata.fileName.startsWith("Backup_") && metadata.fileName.endsWith(".hpb"))
        assertEquals("تعداد تراکنش‌ها باید ۲ باشد", 2, metadata.transactionCount)
        assertEquals("تعداد اهداف باید ۱ باشد", 1, metadata.goalsCount)
        assertTrue("حجم بایت‌های بکاپ باید بزرگتر از صفر باشد", metadata.sizeBytes > 0)
        assertEquals("1.6.0", metadata.appVersion)
    }

    // ۲. GoogleDriveBackupListTest: تست دریافت و لیست کردن نسخه‌های پشتیبان با تمام فیلدها
    @Test
    fun testGoogleDriveBackupListTest() = runBlocking {
        populateSampleData()

        // ایجاد نسخه پشتیبان
        val createResult = repository.createAndUploadBackup(
            budgetLimit = 10000000L,
            isCurrencyInRial = false,
            selectedPeriod = "ماهانه",
            isVip = true
        )
        assertTrue(createResult.isSuccess)

        // دریافت لیست بکاپ‌ها
        val listResult = repository.listAvailableBackups()
        assertTrue("استعلام لیست بکاپ‌ها باید موفق باشد", listResult.isSuccess)
        val backups = listResult.getOrThrow()
        assertFalse("لیست بکاپ‌های موجود نباید خالی باشد", backups.isEmpty())

        val firstBackup = backups.first()
        assertTrue("نام فایل باید صحیح باشد", firstBackup.fileName.startsWith("Backup_"))
        assertTrue("حجم فایل باید معتبر باشد", firstBackup.sizeBytes > 0)
        assertEquals("نسخه اپلیکیشن باید ۱.۶.۰ باشد", "1.6.0", firstBackup.appVersion)
        assertTrue("تاریخ فایل باید معتبر باشد", firstBackup.timestamp > 0L)
    }

    // ۳. GoogleDriveRestoreTest: تست کامل بازیابی بر اساس انتخاب فایل
    @Test
    fun testGoogleDriveRestoreTest() = runBlocking {
        populateSampleData()

        // ایجاد بکاپ
        val createResult = repository.createAndUploadBackup(
            budgetLimit = 20000000L,
            isCurrencyInRial = true,
            selectedPeriod = "سالانه",
            isVip = true
        )
        assertTrue(createResult.isSuccess)
        val backupMeta = createResult.getOrThrow()

        // شبیه‌سازی خالی شدن پایگاه داده در دستگاه جدید
        db.clearAllTables()
        assertEquals(0, transactionDao.getAllTransactionsList().size)
        assertEquals(0, savingsGoalDao.getAllGoalsList().size)

        // بازیابی نسخه انتخاب شده بر اساس File ID
        val restoreResult = repository.restoreBackup(backupMeta.fileId)
        assertTrue("بازیابی بکاپ با شناسه مشخص باید موفق باشد", restoreResult.isSuccess)

        val restoredPayload = restoreResult.getOrThrow()
        assertEquals(2, restoredPayload.transactions.size)
        assertEquals(1, restoredPayload.savingsGoals.size)
        assertEquals(20000000L, restoredPayload.appPreferences.budgetLimit)
        assertTrue(restoredPayload.appPreferences.isCurrencyInRial)
        assertEquals("سالانه", restoredPayload.appPreferences.selectedPeriod)

        // بررسی اینکه جداول Room با موفقیت پر شدند
        assertEquals(2, transactionDao.getAllTransactionsList().size)
        assertEquals(1, savingsGoalDao.getAllGoalsList().size)
        assertEquals(1, recurringDao.getAllList().size)
        assertEquals(1, customCategoryDao.getAllCategoriesList().size)
    }

    // ۴. RestorePermissionTest: تست اعتبارسنجی دسترسی و عدم سکوت در صورت فقدان پرمیشن
    @Test
    fun testRestorePermissionTest() = runBlocking {
        // خروج از حساب برای شبیه‌سازی عدم اتصال
        driveHelper.signOut()

        val listResult = repository.listAvailableBackups()
        assertTrue("در صورت عدم اتصال کاربر به حساب، عملیات باید با شکست مواجه شود", listResult.isFailure)

        val exception = listResult.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(
            "پیام خطای پرمیشن باید صریح و حاوی توضیح دسترسی Google Drive باشد",
            exception?.message?.contains("دسترسی Google Drive کافی نیست") == true
        )
    }

    // ۵. BackupDecryptTest: تست رمزگشایی و مدیریت خطای هدر نامعتبر
    @Test
    fun testBackupDecryptTest() {
        val samplePayload = KisehBackupPayload(
            timestamp = System.currentTimeMillis(),
            transactions = emptyList(),
            savingsGoals = emptyList(),
            recurringTransactions = emptyList(),
            customCategories = emptyList(),
            appPreferences = AppPreferencesBackup(5000000, false, "ماهانه", false)
        )

        val encryptedBytes = BackupCryptoManager.encryptBackup(samplePayload)
        assertTrue(encryptedBytes.size > 20)

        // تست رمزگشایی موفق
        val decrypted = BackupCryptoManager.decryptBackup(encryptedBytes)
        assertEquals(samplePayload.timestamp, decrypted.timestamp)

        // تست مدیریت بایت‌های نامعتبر یا جعلی (Invalid Header)
        val invalidBytes = "NotAValidBackupFileHeaderData".toByteArray(Charsets.UTF_8)
        try {
            BackupCryptoManager.decryptBackup(invalidBytes)
            fail("فایل با هدر نامعتبر باید اکسپشن پرتاب کند")
        } catch (e: Exception) {
            assertTrue(
                "پیام خطا باید مشخصاً به خطا یا هدر نامعتبر اشاره کند: ${e.message}",
                e.message?.contains("هدر") == true ||
                e.message?.contains("خراب است") == true ||
                e.message?.contains("نامعتبر") == true
            )
        }
    }

    // ۶. BackupDatabaseRecoveryTest: تست بازنشانی اتمیک داده‌های مالی و عدم تخریب پایگاه داده
    @Test
    fun testBackupDatabaseRecoveryTest() = runBlocking {
        populateSampleData()

        val initialTx = transactionDao.getAllTransactionsList()
        assertEquals(2, initialTx.size)

        // بکاپ اول
        val backupResult = repository.createAndUploadBackup(
            budgetLimit = 18000000L,
            isCurrencyInRial = false,
            selectedPeriod = "ماهانه",
            isVip = true
        )
        assertTrue(backupResult.isSuccess)

        // درج تراکنش جدید
        val newTx = Transaction(id = 103, amount = 450000, category = "تفریح", description = "سینما", isIncome = false, date = 1700000200000L)
        transactionDao.insertAll(listOf(newTx))
        assertEquals(3, transactionDao.getAllTransactionsList().size)

        // بازنشانی بکاپ قبلی
        val restoreResult = repository.restoreBackup()
        assertTrue(restoreResult.isSuccess)

        // بررسی اینکه تراکنش‌های قدیمی با موفقیت merge و update شدند و دیتابیس پایدار است
        val finalTxList = transactionDao.getAllTransactionsList()
        assertTrue(finalTxList.any { it.id == 101L })
        assertTrue(finalTxList.any { it.id == 102L })
    }
}
