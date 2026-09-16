package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.billing.BillingManager
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LicenseSystemRemovedTest {

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
    fun testLicenseStringsDoNotActivateVip() {
        // Free user initially
        assertFalse(BillingManager.isVipUser(context))

        // Attempts to activate via license keys, activation codes, or serial numbers must fail
        val fakeLicenseKeys = listOf(
            "VIP-2026-LIFETIME",
            "KISEH-PRO-KEY-9999",
            "RSA-ACTIVATION-TOKEN-XYZ",
            "CLOUDFLARE_ACTIVATE_VIP",
            "SERIAL-ABC-DEF-GHI"
        )

        fakeLicenseKeys.forEach { key ->
            // Verification: verifyPurchase fails when given fake license signature
            val isVerified = BillingManager.verifyPurchase(key, "invalid_sig")
            assertFalse("Fake license $key should not be verified as purchase", isVerified)
        }

        // VIP status remains false
        assertFalse(BillingManager.isVipUser(context))
    }

    @Test
    fun testVipOnlyAllowedViaOfficialStoreBilling() {
        // Verification: Without valid Myket verified purchase token, user remains Free
        assertFalse(BillingManager.isVipUser(context))
    }
}
