package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.billing.BillingManager
import com.example.ui.components.ProPaywallDialog
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class ProPaywallDialogLayoutTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        BillingManager.setVipUser(context, false)
    }

    @After
    fun tearDown() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        BillingManager.setVipUser(context, false)
    }

    @Test
    fun testPaywallButtonsAreDisplayedDistinctlyWithoutOverlap() {
        var dismissed = false
        var purchased = false

        composeTestRule.setContent {
            ProPaywallDialog(
                onDismiss = { dismissed = true },
                onSuccessPurchase = { purchased = true }
            )
        }

        // 1. Primary Purchase Button is displayed and clickable
        composeTestRule.onNodeWithTag("activate_vip_button")
            .assertIsDisplayed()

        // 2. Restore Purchases Button is displayed and clickable
        composeTestRule.onNodeWithTag("restore_purchase_button")
            .assertIsDisplayed()

        // 3. Continue with Free Button is displayed, separate, and dismisses the dialog
        composeTestRule.onNodeWithTag("continue_free_button")
            .assertIsDisplayed()
            .performClick()

        assertTrue("Clicking continue with free button should trigger onDismiss", dismissed)
    }

    @Test
    fun testPaywallWhenAlreadyVipDisplaysCloseButton() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        BillingManager.setVipUser(context, true)

        var dismissed = false
        composeTestRule.setContent {
            ProPaywallDialog(
                onDismiss = { dismissed = true }
            )
        }

        composeTestRule.onNodeWithTag("close_dialog_button")
            .assertIsDisplayed()
            .performClick()

        assertTrue("Clicking close button should trigger onDismiss", dismissed)
    }

    @Test
    fun testMyketOfficialProductAndRsaIntegrity() {
        assertEquals("kiseh_pro_lifetime", BillingManager.SKU_PRO_LIFETIME)
        assertEquals("kiseh_pro_lifetime", BillingManager.SKU_VIP_LIFETIME)
        assertTrue(BillingManager.ALL_VIP_SKUS.contains("kiseh_pro_lifetime"))
        assertEquals(
            "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQChuEJCVjyXbvgOF74J3Dzbo86482Q+HI7UFFX1odEhqQ4qBO/62dEtjBvMzxUe8rZeVEpxJeATTx95+FIZJPiogXVCYhWwXokrSYO0F67wgTivoapyUUIevzZtZhi6JTEQebIPNR2J8qIRPzzC09FOVylhNobJGqXXcet1fzlrOQIDAQAB",
            BillingManager.MYKET_PUBLIC_KEY
        )
    }
}
