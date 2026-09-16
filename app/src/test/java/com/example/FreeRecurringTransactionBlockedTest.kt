package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.billing.BillingManager
import com.example.billing.FeatureUsageManager
import com.example.billing.VipPreferencesManager
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FreeRecurringTransactionBlockedTest {

    private lateinit var app: Context

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        BillingManager.setVipUser(app, false)
        runBlocking {
            VipPreferencesManager(app).clearVip()
        }
    }

    @After
    fun tearDown() {
        runBlocking {
            BillingManager.setVipUser(app, false)
            VipPreferencesManager(app).clearVip()
        }
    }

    @Test
    fun testFreeUserQuotaForRecurringTransactions() {
        assertFalse(BillingManager.isVipUser(app))

        // 1st item (count = 0) is allowed for Free user
        assertTrue("Free user must be allowed 1st recurring transaction", FeatureUsageManager.canAddRecurringTransaction(app, 0))

        // 2nd item (count = 1) must be blocked for Free user
        assertFalse("Free user must be blocked from 2nd recurring transaction", FeatureUsageManager.canAddRecurringTransaction(app, 1))
    }

    @Test
    fun testVipUserCanAddRecurringTransaction() {
        BillingManager.setVipUser(app, true)
        assertTrue(BillingManager.isVipUser(app))

        // VIP user can add unlimited recurring transactions
        assertTrue("VIP user must be allowed 1st recurring transaction", FeatureUsageManager.canAddRecurringTransaction(app, 0))
        assertTrue("VIP user must be allowed 2nd recurring transaction", FeatureUsageManager.canAddRecurringTransaction(app, 1))
        assertTrue("VIP user must be allowed 10th recurring transaction", FeatureUsageManager.canAddRecurringTransaction(app, 10))
    }
}
