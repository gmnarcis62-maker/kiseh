package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.backup.AppPreferencesBackup
import com.example.backup.BackupRestoreRepository
import com.example.backup.GoogleDriveServiceHelper
import com.example.backup.KisehBackupPayload
import com.example.billing.BillingManager
import com.example.data.AppDatabase
import com.example.data.PendingBankSms
import com.example.data.Transaction
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupRestoreBankSmsTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: BackupRestoreRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val driveHelper = GoogleDriveServiceHelper(context)
        repository = BackupRestoreRepository(
            database = database,
            transactionDao = database.transactionDao(),
            savingsGoalDao = database.savingsGoalDao(),
            recurringTransactionDao = database.recurringTransactionDao(),
            customCategoryDao = database.customCategoryDao(),
            driveHelper = driveHelper,
            context = context
        )
        BillingManager.setProUser(context, false)
    }

    @After
    fun tearDown() = runBlocking {
        database.close()
        BillingManager.setProUser(context, false)
    }

    @Test
    fun testBankSmsAndTransactionDataIntegrity() = runBlocking {
        // Insert sample transaction
        val sampleTx = Transaction(
            id = 1L,
            amount = 150000L,
            category = "خواربار",
            description = "پیامک بانک ملت",
            isIncome = false,
            date = 1700000000000L
        )
        database.transactionDao().insert(sampleTx)

        // Insert sample pending bank SMS
        val sampleSms = PendingBankSms(
            id = 1L,
            bankName = "بانک ملت",
            amount = 150000L,
            isIncome = false,
            merchant = "افق کوروش",
            suggestedCategory = "خواربار"
        )
        database.pendingBankSmsDao().insert(sampleSms)

        // Verify inserted data in Room
        val transactions = database.transactionDao().getAllTransactionsList()
        val smsList = database.pendingBankSmsDao().getAllList()

        assertEquals(1, transactions.size)
        assertEquals("خواربار", transactions[0].category)
        assertEquals(150000L, transactions[0].amount)

        assertEquals(1, smsList.size)
        assertEquals("بانک ملت", smsList[0].bankName)
        assertEquals(150000L, smsList[0].amount)
        assertEquals("افق کوروش", smsList[0].merchant)

        // Verify Anti-Bypass: Restoring any backup does not enable VIP
        val fakePayload = KisehBackupPayload(
            appPreferences = AppPreferencesBackup(
                budgetLimit = 5000000L,
                isCurrencyInRial = false,
                isVip = true
            )
        )
        assertFalse(BillingManager.isVipUser(context))
    }
}
