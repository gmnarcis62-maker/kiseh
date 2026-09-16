package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.billing.BillingManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MyketProductPriceTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        BillingManager.setSkuPriceForTesting(null)
    }

    @After
    fun tearDown() {
        BillingManager.setSkuPriceForTesting(null)
    }

    @Test
    fun testInitialPriceIsNullBeforeQuery() {
        // Price should not be hardcoded to 49000 or anything else
        assertNull(BillingManager.skuPriceState.value)
        assertNull(BillingManager.vipProductPrice.value)
    }

    @Test
    fun testFormatPriceDisplayWithMyketTestPrice() {
        // Myket panel configured test price: 1000 Tomans
        val formatted1000 = BillingManager.formatPriceDisplay("1000")
        assertEquals("۱,۰۰۰ تومان", formatted1000)

        val formattedWithToman = BillingManager.formatPriceDisplay("1000 تومان")
        assertTrue(formattedWithToman.contains("۱۰۰۰") || formattedWithToman.contains("۱,۰۰۰"))
    }

    @Test
    fun testDynamicPriceStateUpdate() {
        // Simulate Myket query returning 1000 Tomans
        val testPrice = BillingManager.formatPriceDisplay("1000")
        BillingManager.setSkuPriceForTesting(testPrice)

        assertEquals("۱,۰۰۰ تومان", BillingManager.skuPriceState.value)
        assertEquals("۱,۰۰۰ تومان", BillingManager.vipProductPrice.value)
    }

    @Test
    fun testEmptyPriceHandling() {
        assertEquals("", BillingManager.formatPriceDisplay(""))
        assertEquals("", BillingManager.formatPriceDisplay(null))
    }
}
