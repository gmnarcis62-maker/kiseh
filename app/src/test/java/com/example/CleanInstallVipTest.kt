package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.billing.BillingManager
import com.example.billing.VipPreferencesManager
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
class CleanInstallVipTest {

    private lateinit var context: Context

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        // Simulate clean install: clear all prefs and DataStore
        val prefs = context.getSharedPreferences("kiseh_billing_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        VipPreferencesManager(context).clearVip()
    }

    @After
    fun tearDown() = runBlocking {
        val prefs = context.getSharedPreferences("kiseh_billing_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        VipPreferencesManager(context).clearVip()
    }

    @Test
    fun testCleanInstallInitialStateIsFree() {
        BillingManager.init(context)

        // On clean install, user must be Free tier
        assertFalse("Clean install must default to Free tier", BillingManager.isVipUser(context))
        assertFalse(BillingManager.isProState.value)
        assertEquals(5, BillingManager.getRemainingVoiceTrial(context))
    }

    @Test
    fun testCleanInstallCannotBeBypassedByLegacyPrefs() = runBlocking {
        val prefs = context.getSharedPreferences("kiseh_billing_prefs", Context.MODE_PRIVATE)
        // Inject fake legacy bazaar flag
        prefs.edit()
            .putBoolean("is_pro_user", true)
            .putString("purchase_store", "bazaar")
            .commit()

        BillingManager.init(context)

        // Must be reset to Free
        assertFalse("Legacy Bazaar flag must be purged on initialization", BillingManager.isVipUser(context))
        assertFalse(BillingManager.isProState.value)
    }

    @Test
    fun testCleanInstallActivatedOnlyByMyket() = runBlocking {
        BillingManager.init(context)
        assertFalse(BillingManager.isVipUser(context))

        // Legitimate Myket purchase flow
        BillingManager.setVipUser(
            context = context,
            isVip = true,
            purchaseToken = "myket_verified_token_1001",
            purchaseDate = System.currentTimeMillis(),
            productId = BillingManager.SKU_VIP_LIFETIME
        )

        assertTrue("Verified Myket purchase must activate VIP", BillingManager.isVipUser(context))
        assertTrue(BillingManager.isProState.value)
    }
}
