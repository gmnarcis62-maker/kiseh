package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.billing.BillingManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `verify app name string resource`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("کیسه", appName)
  }

  @Test
  fun `verify Myket billing configuration and SKU`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    
    // Check SKU
    assertEquals("kiseh_pro_lifetime", BillingManager.SKU_VIP_LIFETIME)
    assertEquals("ir.mservices.market", BillingManager.MYKET_PACKAGE)
    assertEquals("ir.mservices.market.InAppBillingService.BIND", BillingManager.MYKET_BILLING_ACTION)
    
    // Test set and get pro/vip user
    BillingManager.setVipUser(context, false)
    assertFalse(BillingManager.isVipUser(context))
    assertFalse(BillingManager.isProUser(context))
    assertEquals(5, BillingManager.getRemainingVoiceTrial(context))
    assertTrue(BillingManager.canUseVoiceInput(context))
    
    BillingManager.setVipUser(context, true)
    assertTrue(BillingManager.isVipUser(context))
    assertTrue(BillingManager.isProUser(context))
    assertEquals(Int.MAX_VALUE, BillingManager.getRemainingVoiceTrial(context))
    assertTrue(BillingManager.canUseVoiceInput(context))
  }

  @Test
  fun `verify MainActivity launches without crash`() {
    val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java)
    controller.create().start().postCreate(null).resume().visible()
    assertNotNull(controller.get())
  }
}
