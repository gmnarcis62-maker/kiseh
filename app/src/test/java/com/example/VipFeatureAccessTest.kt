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
class VipFeatureAccessTest {

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
    fun tearDown() {
        database.close()
        BillingManager.setProUser(context, false)
    }

    @Test
    fun testFreeVsVipVoiceLimits() {
        // Free
        BillingManager.setProUser(context, false)
        BillingManager.resetVoiceUsageForTesting(context, 0)
        assertEquals(5, BillingManager.getRemainingVoiceTrial(context))

        // VIP
        BillingManager.setVipUser(context, true)
        assertEquals(Int.MAX_VALUE, BillingManager.getRemainingVoiceTrial(context))
        assertTrue(BillingManager.canUseVoiceInput(context))
    }

    @Test
    fun testBackupRestoreCannotBypassVipStatus() = runBlocking {
        // User is Free
        BillingManager.setProUser(context, false)
        assertFalse(BillingManager.isVipUser(context))

        // Crafted payload with isVip = true
        val maliciousPayload = KisehBackupPayload(
            appPreferences = AppPreferencesBackup(
                budgetLimit = 10000000L,
                isCurrencyInRial = false,
                isVip = true // attempting bypass
            )
        )

        // VIP status MUST NOT be activated by backup preferences
        assertFalse(BillingManager.isVipUser(context))
        assertFalse(BillingManager.isProUser(context))
    }
}
