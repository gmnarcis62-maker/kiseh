package com.example

import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BazaarDependencyRemovalTest {

    @Test
    fun testBazaarPermissionNotDeclaredInManifest() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS
        )
        val requestedPermissions = packageInfo.requestedPermissions?.toList() ?: emptyList()

        // Verify Bazaar payment permission is completely removed
        assertFalse(
            "com.farsitel.bazaar.permission.PAY_THROUGH_BAZAAR must not be present in manifest",
            requestedPermissions.contains("com.farsitel.bazaar.permission.PAY_THROUGH_BAZAAR")
        )

        // Verify Myket permission is declared
        assertTrue(
            "ir.mservices.market.BILLING must be present in manifest",
            requestedPermissions.contains("ir.mservices.market.BILLING")
        )
    }

    @Test
    fun testBazaarClassesNotLoaded() {
        // Confirm no CafeBazaar SDK or billing client classes are present
        val bazaarClasses = listOf(
            "com.farsitel.bazaar.Bazaar",
            "com.farsitel.bazaar.BillingClient",
            "com.farsitel.bazaar.purchase.Purchase",
            "ir.cafebazaar.pardakht.InAppBillingService"
        )

        for (className in bazaarClasses) {
            var classFound = false
            try {
                Class.forName(className)
                classFound = true
            } catch (_: ClassNotFoundException) {
                classFound = false
            }
            assertFalse("Bazaar class $className should not exist on classpath", classFound)
        }
    }
}
