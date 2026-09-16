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
class VipPersistenceTest {

    private lateinit var context: Context
    private lateinit var vipManager: VipPreferencesManager

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        vipManager = VipPreferencesManager(context)
        vipManager.clearVip()
        BillingManager.setProUser(context, false)
    }

    @After
    fun tearDown() = runBlocking {
        vipManager.clearVip()
        BillingManager.setProUser(context, false)
    }

    @Test
    fun testVipPersistenceViaDataStore() = runBlocking {
        // Initial state in DataStore is not VIP
        val initialInfo = vipManager.getVipInfo()
        assertFalse(initialInfo.isVip)

        // Save VIP with purchase metadata
        val token = "sample_myket_purchase_token_123"
        val date = 1715000000000L
        val productId = BillingManager.SKU_VIP_LIFETIME

        vipManager.saveVipStatus(
            isVip = true,
            purchaseToken = token,
            purchaseDate = date,
            productId = productId
        )

        // Retrieve and verify from DataStore
        val persistedInfo = vipManager.getVipInfo()
        assertTrue(persistedInfo.isVip)
        assertEquals(token, persistedInfo.purchaseToken)
        assertEquals(date, persistedInfo.purchaseDate)
        assertEquals(productId, persistedInfo.productId)

        // Clear and verify
        vipManager.clearVip()
        val clearedInfo = vipManager.getVipInfo()
        assertFalse(clearedInfo.isVip)
    }
}
