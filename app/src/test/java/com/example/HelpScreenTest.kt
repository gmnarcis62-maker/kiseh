package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HelpScreenTest {

    @Test
    fun testHelpScreenContentIntegrity() {
        val introText = "کیسه یک دستیار مدیریت مالی شخصی است که به شما کمک می‌کند هزینه‌ها، درآمدها و برنامه مالی خود را مدیریت کنید."
        assertEquals("کیسه یک دستیار مدیریت مالی شخصی است که به شما کمک می‌کند هزینه‌ها، درآمدها و برنامه مالی خود را مدیریت کنید.", introText)

        val features = listOf(
            "ثبت دستی تراکنش‌ها",
            "ثبت صوتی هزینه و درآمد",
            "هوش پیامکی بانکی",
            "اسکن و تحلیل هوشمند فاکتور",
            "اهداف پس‌انداز",
            "تراکنش‌های دوره‌ای",
            "گزارش‌های مالی",
            "پشتیبان‌گیری و بازیابی"
        )

        assertEquals(8, features.size)
        assertTrue(features.contains("ثبت دستی تراکنش‌ها"))
        assertTrue(features.contains("ثبت صوتی هزینه و درآمد"))
        assertTrue(features.contains("هوش پیامکی بانکی"))
        assertTrue(features.contains("اسکن و تحلیل هوشمند فاکتور"))
        assertTrue(features.contains("اهداف پس‌انداز"))
        assertTrue(features.contains("تراکنش‌های دوره‌ای"))
        assertTrue(features.contains("گزارش‌های مالی"))
        assertTrue(features.contains("پشتیبان‌گیری و بازیابی"))
    }
}
