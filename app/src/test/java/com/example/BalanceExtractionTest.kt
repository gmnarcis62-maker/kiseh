package com.example

import com.example.sms.BankSmsParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class BalanceExtractionTest {

    private lateinit var parser: BankSmsParser

    @Before
    fun setUp() {
        parser = BankSmsParser()
    }

    @Test
    fun testRialBalanceConvertedToToman() {
        val sms = """
            بانک سپه
            برداشت: 500,000 ریال
            مانده: 50,000,000 ریال
            از کارت: 589210******1234
        """.trimIndent()

        val parsed = parser.parse(sms)
        assertNotNull(parsed)
        // 50,000,000 ریال = 5,000,000 تومان
        assertEquals(5000000L, parsed!!.balance)
    }

    @Test
    fun testTomanBalanceRemainsInToman() {
        val sms = """
            بانک سامان
            واریز: 1,500,000 تومان
            مانده: 14,000,000 تومان
            به حساب: ...9876
        """.trimIndent()

        val parsed = parser.parse(sms)
        assertNotNull(parsed)
        assertEquals(14000000L, parsed!!.balance)
    }

    @Test
    fun testPersianNumeralsInBalance() {
        val sms = """
            بانک کشاورزی
            برداشت: ۱۰۰,۰۰۰ ریال
            مانده: ۲۵,۰۰۰,۰۰۰ ریال
        """.trimIndent()

        val parsed = parser.parse(sms)
        assertNotNull(parsed)
        // ۲۵,۰۰۰,۰۰۰ ریال = ۲,۵۰۰,۰۰۰ تومان
        assertEquals(2500000L, parsed!!.balance)
    }

    @Test
    fun testPureBalanceInquiry_returnsNull() {
        // اعلام موجودی بدون تراکنش باید نادیده گرفته شود تا تراکنش نادرست ثبت نشود
        val sms1 = "بانک مسکن: مانده حساب شماره 123456789 مبلغ 8,500,000 ریال می‌باشد."
        assertNull(parser.parse(sms1))

        val sms2 = "موجودی کارت شما: 1,200,000 تومان"
        assertNull(parser.parse(sms2))

        val sms3 = "بانک تجارت: مانده نهایی سپرده شما ۱۰,۰۰۰,۰۰۰ ریال است."
        assertNull(parser.parse(sms3))
    }
}
