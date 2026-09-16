package com.example

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.billing.BillingManager
import com.example.notification.QuickVoiceNotificationHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NotificationWidgetVisibilityTest {

    private lateinit var context: Context
    private lateinit var notificationManager: NotificationManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        shadowOf(context as android.app.Application).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
    }

    @Test
    fun testNotificationHiddenWhenOptionDisabledOrUserFree() {
        val shadowNotificationManager = shadowOf(notificationManager)

        // ۱. کاربر رایگان: نباید هیچ اعلانی نشان داده شود
        BillingManager.setProUserForTesting(context, false)
        BillingManager.setQuickVoiceNotificationEnabled(context, true)

        QuickVoiceNotificationHelper.showQuickVoiceNotification(context)
        assertEquals(0, shadowNotificationManager.allNotifications.size)

        // ۲. کاربر پرو اما گزینه ویجت غیرفعال: باز هم نباید ویجت نشان داده شود
        BillingManager.setProUserForTesting(context, true)
        BillingManager.setQuickVoiceNotificationEnabled(context, false)

        QuickVoiceNotificationHelper.showQuickVoiceNotification(context)
        assertEquals(0, shadowNotificationManager.allNotifications.size)
    }

    @Test
    fun testNotificationShownWhenEnabledAndUserVip() {
        val shadowNotificationManager = shadowOf(notificationManager)

        BillingManager.setProUserForTesting(context, true)
        BillingManager.setQuickVoiceNotificationEnabled(context, true)

        QuickVoiceNotificationHelper.showQuickVoiceNotification(context)

        // باید اعلان ویجت اضافه شود
        assertTrue(shadowNotificationManager.allNotifications.isNotEmpty())
        val notification = shadowNotificationManager.allNotifications.first()
        assertTrue((notification.flags and android.app.Notification.FLAG_ONGOING_EVENT) != 0)

        // لغو اعلان
        QuickVoiceNotificationHelper.cancelQuickVoiceNotification(context)
        assertEquals(0, shadowNotificationManager.allNotifications.size)
    }
}
