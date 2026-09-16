package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.billing.BillingManager
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
class VipActivationTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        BillingManager.setProUser(context, false)
    }

    @After
    fun tearDown() {
        BillingManager.setProUser(context, false)
    }

    @Test
    fun testVipActivationCycle() {
        // Initially Free
        assertFalse(BillingManager.isVipUser(context))
        assertFalse(BillingManager.isProState.value)

        // Activate VIP
        BillingManager.setVipUser(context, true)
        assertTrue(BillingManager.isVipUser(context))
        assertTrue(BillingManager.isProState.value)

        // Deactivate or reset
        BillingManager.setVipUser(context, false)
        assertFalse(BillingManager.isVipUser(context))
        assertFalse(BillingManager.isProState.value)
    }
}
