package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.billing.BillingManager
import com.example.sms.LearningEngine
import com.example.sms.UserCategoryLearner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LearningEngineVipOnlyTest {

    private lateinit var context: Context
    private lateinit var learner: UserCategoryLearner

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        UserCategoryLearner.resetInstanceForTesting()
        learner = UserCategoryLearner.getInstance(context)
        learner.clear()
    }

    @Test
    fun testLearningEngineBlockedForFreeUser() {
        BillingManager.setProUserForTesting(context, false)
        assertFalse(BillingManager.isProUser(context))

        // ۱. تلاش برای یادگیری در نسخه رایگان
        val result = LearningEngine.learn(context, "بوتیک بهار", "پوشاک")
        assertFalse(result)

        // ۲. عدم ذخیره در ترجیحات
        val learned = learner.getLearnedCategory("بوتیک بهار")
        assertNull(learned)

        // ۳. تست learnVipOnly در UserCategoryLearner
        val vipOnlyResult = learner.learnVipOnly("کافه نادری", "خوراکی")
        assertFalse(vipOnlyResult)
        assertNull(learner.getLearnedCategory("کافه نادری"))
    }

    @Test
    fun testLearningEngineActiveForVipUser() {
        BillingManager.setProUserForTesting(context, true)
        assertTrue(BillingManager.isProUser(context))

        // ۱. یادگیری موفق در نسخه VIP
        val result = LearningEngine.learn(context, "بوتیک بهار", "پوشاک")
        assertTrue(result)

        // ۲. بازیابی موفق دسته‌بندی یادگرفته‌شده
        val learned = learner.getLearnedCategory("بوتیک بهار")
        assertEquals("پوشاک", learned)

        // ۳. تست learnVipOnly در نسخه VIP
        val vipOnlyResult = learner.learnVipOnly("کافه نادری", "خوراکی")
        assertTrue(vipOnlyResult)
        assertEquals("خوراکی", learner.getLearnedCategory("کافه نادری"))
    }
}
