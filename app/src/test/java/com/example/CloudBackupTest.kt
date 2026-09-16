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
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CloudBackupTest {

    private lateinit var db: AppDatabase
    private lateinit var transactionDao: TransactionDao
    private lateinit var savingsGoalDao: SavingsGoalDao
    private lateinit var recurringDao: RecurringTransactionDao
    private lateinit var customCategoryDao: CustomCategoryDao
    private lateinit var driveHelper: GoogleDriveServiceHelper
    private lateinit var repository: BackupRestoreRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        transactionDao = db.transactionDao()
        savingsGoalDao = db.savingsGoalDao()
        recurringDao = db.recurringTransactionDao()
        customCategoryDao = db.customCategoryDao()
        driveHelper = GoogleDriveServiceHelper(context)

        repository = BackupRestoreRepository(
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

    private fun createSamplePayload(): KisehBackupPayload {
        val transactions = listOf(
            Transaction(id = 1, amount = 250000, category = "خرید روزمره", description = "سوپرمارکت", isIncome = false, date = 1700000000000L),
            Transaction(id = 2, amount = 8500000, category = "حقوق", description = "واریز حقوق اسفند", isIncome = true, date = 1700000010000L)
        )
        val goals = listOf(
            SavingsGoal(id = 1, title = "خرید لپتاپ", targetAmount = 50000000, savedAmount = 15000000, targetDate = 1750000000000L)
        )
        val recurring = listOf(
            RecurringTransaction(
                id = 1,
                title = "اشتراک فیبر نوری",
                amount = 1200000,
                category = "اینترنت",
                period = RecurrencePeriod.MONTHLY.name,
                startDate = 1700000000000L,
                nextExecutionDate = 1702592000000L,
                isActive = true,
                isIncome = false,
                note = "پرداخت آنلاین"
            )
        )
        val categories = listOf(
            CustomCategory(id = 1, name = "سرمایه‌گذاری", iconName = "trending_up", colorHex = "#00897B", isIncome = false)
        )
        val prefs = AppPreferencesBackup(budgetLimit = 8000000, isCurrencyInRial = false, selectedPeriod = "ماهانه", isVip = true)

        return KisehBackupPayload(
            timestamp = 1700000050000L,
            transactions = transactions,
            savingsGoals = goals,
            recurringTransactions = recurring,
            customCategories = categories,
            appPreferences = prefs
        )
    }

    // ۱. تست رمزنگاری (Encryption)
    @Test
    fun testBackupEncryption_producesSecureCipherBytes() {
        val payload = createSamplePayload()
        val cipherBytes = BackupCryptoManager.encryptBackup(payload)

        // خروجی نباید خالی باشد
        assertTrue(cipherBytes.isNotEmpty())
        // خروجی نباید حاوی متون ساده مالی به عنوان رشته خام باشد
        val asString = String(cipherBytes, Charsets.ISO_8859_1)
        assertFalse(asString.contains("سوپرمارکت"))
        assertFalse(asString.contains("واریز حقوق"))

        // تست اینکه دو بار رمزنگاری یک پی‌لود به دلیل IV تصادفی خروجی‌های متفاوتی تولید می‌کند (Semantically Secure)
        val cipherBytes2 = BackupCryptoManager.encryptBackup(payload)
        assertFalse(cipherBytes.contentEquals(cipherBytes2))
    }

    // ۲. تست رمزگشایی (Decryption)
    @Test
    fun testBackupDecryption_restoresOriginalPayloadAccurately() {
        val originalPayload = createSamplePayload()
        val encrypted = BackupCryptoManager.encryptBackup(originalPayload)

        val decryptedPayload = BackupCryptoManager.decryptBackup(encrypted)

        assertEquals(originalPayload.transactions.size, decryptedPayload.transactions.size)
        assertEquals(originalPayload.transactions[0].amount, decryptedPayload.transactions[0].amount)
        assertEquals(originalPayload.transactions[0].description, decryptedPayload.transactions[0].description)
        assertEquals(originalPayload.savingsGoals.size, decryptedPayload.savingsGoals.size)
        assertEquals(originalPayload.savingsGoals[0].title, decryptedPayload.savingsGoals[0].title)
        assertEquals(originalPayload.recurringTransactions.size, decryptedPayload.recurringTransactions.size)
        assertEquals(originalPayload.customCategories.size, decryptedPayload.customCategories.size)
        assertEquals(originalPayload.appPreferences.budgetLimit, decryptedPayload.appPreferences.budgetLimit)
    }

    // ۳. تست صحت فایل بکاپ و اعتبارسنجی Checksum (Integrity & Tamper-Proof)
    @Test
    fun testBackupIntegrity_failsOnTamperedData() {
        val originalPayload = createSamplePayload()
        val encrypted = BackupCryptoManager.encryptBackup(originalPayload)

        // دستکاری یک بایت تصادفی در بدنه فایل
        val tampered = encrypted.copyOf()
        tampered[tampered.size - 5] = (tampered[tampered.size - 5] + 1).toByte()

        try {
            BackupCryptoManager.decryptBackup(tampered)
            fail("فایل دستکاری شده نباید رمزگشایی شود!")
        } catch (e: Exception) {
            // انتظار خطا به دلیل نقض اصالت GCM یا Checksum
            assertTrue(e is Exception)
        }
    }

    @Test
    fun testChecksumCalculation_isDeterministicAndAccurate() {
        val input = "kiseh_financial_checksum_test_data"
        val checksum1 = BackupCryptoManager.calculateChecksum(input)
        val checksum2 = BackupCryptoManager.calculateChecksum(input)

        assertEquals(64, checksum1.length) // SHA-256 خروجی ۶۴ کاراکتر هگز دارد
        assertEquals(checksum1, checksum2)
    }

    // ۴. تست بازیابی اطلاعات در پایگاه داده (Data Restore)
    @Test
    fun testDatabaseDataRestore_insertsAllDataAtomically() = runBlocking {
        // ابتدا بررسی دیتابیس خالی
        assertEquals(0, transactionDao.getAllTransactionsList().size)
        assertEquals(0, savingsGoalDao.getAllGoalsList().size)

        // ایجاد و آپلود بکاپ در مخزن محلی کلود
        val uploadResult = repository.createAndUploadBackup(
            budgetLimit = 12000000,
            isCurrencyInRial = false,
            selectedPeriod = "ماهانه",
            isVip = true
        )
        assertTrue(uploadResult.isSuccess)

        // درج داده‌های نمونه در دیتابیس
        val sampleTransactions = listOf(
            Transaction(id = 10, amount = 300000, category = "حمل و نقل", description = "اسنپ", isIncome = false, date = 1700000000000L)
        )
        transactionDao.insertAll(sampleTransactions)

        // ایجاد بکاپ جدید با یک تراکنش
        val backupResult = repository.createAndUploadBackup(
            budgetLimit = 15000000,
            isCurrencyInRial = false,
            selectedPeriod = "ماهانه",
            isVip = true
        )
        assertTrue(backupResult.isSuccess)

        // بازیابی بکاپ
        val restoreResult = repository.restoreBackup()
        assertTrue(restoreResult.isSuccess)

        val restoredPayload = restoreResult.getOrThrow()
        assertEquals(1, restoredPayload.transactions.size)
        assertEquals("اسنپ", restoredPayload.transactions[0].description)

        // تایید حضور داده‌ها در دیتابیس Room
        val inDbTransactions = transactionDao.getAllTransactionsList()
        assertEquals(1, inDbTransactions.size)
        assertEquals(300000L, inDbTransactions[0].amount)
    }

    // ۵. تست عدم نشت اطلاعات امنیتی در ساختار بکاپ (No Security Credentials Leak)
    @Test
    fun testSecurityPolicy_noPinInBackupPayload() {
        val payload = createSamplePayload()
        val jsonAdapter = com.squareup.moshi.Moshi.Builder()
            .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
            .build()
            .adapter(KisehBackupPayload::class.java)

        val jsonString = jsonAdapter.toJson(payload)

        // بررسی اینکه هیچ فیلدی به نام pin یا password در ساختار وجود ندارد
        assertFalse(jsonString.contains("\"pin\""))
        assertFalse(jsonString.contains("\"pinHash\""))
        assertFalse(jsonString.contains("\"password\""))
        assertFalse(jsonString.contains("\"salt\""))
    }
}
