package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.onboarding.OnboardingPreferencesManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FirstLaunchOnboardingTest {

    private lateinit var context: Context
    private lateinit var onboardingManager: OnboardingPreferencesManager

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        onboardingManager = OnboardingPreferencesManager(context)
        onboardingManager.resetFirstLaunchForTesting()
    }

    @Test
    fun testFirstLaunchInitialStateIsFalse() = runBlocking {
        // در اولین نصب و اولین اجرا، مقدار hasSeenFirstLaunch باید false باشد
        val hasSeen = onboardingManager.hasSeenFirstLaunch()
        assertFalse("در اولین اجرا، باید فلگ مشاهده Onboarding برابر false باشد", hasSeen)

        val flowVal = onboardingManager.hasSeenFirstLaunchFlow.first()
        assertFalse("Flow نیز باید مقدار false را برگرداند", flowVal)
    }

    @Test
    fun testFirstLaunchBecomesTrueAfterDismissOrStart() = runBlocking {
        // کاربر روی دکمه شروع یا بعداً کلیک می‌کند
        onboardingManager.setHasSeenFirstLaunch(true)

        val hasSeen = onboardingManager.hasSeenFirstLaunch()
        assertTrue("پس از تایید یا رد Onboarding، فلگ باید true شود", hasSeen)

        // نمونه جدیدی از منیجر بسازیم تا پایداری در DataStore تست شود
        val newManagerInstance = OnboardingPreferencesManager(context)
        assertTrue("پس از ایجاد مجدد یا اجرای بعدی، فلگ باید همچنان true باقی بماند", newManagerInstance.hasSeenFirstLaunch())
    }

    @Test
    fun testFirstLaunchDoesNotShowAgainOnSubsequentLaunches() = runBlocking {
        // شبیه‌سازی اولین اجرا و بستن Onboarding
        onboardingManager.setHasSeenFirstLaunch(true)

        // شبیه‌سازی بازگشت از بک‌گراند، تغییر تم، یا راه‌اندازی مجدد
        val managerAfterRestart = OnboardingPreferencesManager(context)
        val hasSeen = managerAfterRestart.hasSeenFirstLaunch()

        // نباید دوباره دیالوگ باز شود
        assertTrue("در دفعات بعدی اجرای برنامه نباید Onboarding مجدداً باز شود", hasSeen)
    }
}
