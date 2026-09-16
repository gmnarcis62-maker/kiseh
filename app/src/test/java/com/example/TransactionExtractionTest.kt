package com.example

import com.example.sms.BankSmsParser
import com.example.sms.BankType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TransactionExtractionTest {

    private lateinit var parser: BankSmsParser

    @Before
    fun setUp() {
        parser = BankSmsParser()
    }

    // ۱. خرید کارتخوان با اعداد فارسی و تبدیل ریال به تومان
    @Test
    fun testPosPurchase_persianDigitsRialToToman() {
        val sms = """
            بانک پاسارگاد
            خرید از پایانه فروشگاهی
            مبلغ: ۲,۵۰۰,۰۰۰ ریال
            از کارت: 502229******8812
            مانده: ۱۸,۰۰۰,۰۰۰ ریال
            پذیرنده: هایپرمارکت نجم
        """.trimIndent()

        val parsed = parser.parse(sms)
        assertNotNull(parsed)
        assertEquals(BankType.PASARGAD, parsed!!.bank)
        // ۲,۵۰۰,۰۰۰ ریال = ۲۵۰,۰۰۰ تومان
        assertEquals(250000L, parsed.amount)
        assertFalse(parsed.isIncome)
        assertEquals(1800000L, parsed.balance)
        assertEquals("8812", parsed.cardNumber)
        assertEquals("خوراک", parsed.suggestedCategory)
    }

    // ۲. واریز حقوق پایا به تومان (نئوبانک بلوبانک)
    @Test
    fun testSalaryIncomePaya_blubankToman() {
        val sms = """
            بلوبانک
            واریز حقوق: 25,000,000 تومان
            از طریق انتقال پایا
            به حساب: 9012
            مانده: 32,500,000 تومان
        """.trimIndent()

        val parsed = parser.parse(sms)
        assertNotNull(parsed)
        assertEquals(BankType.BLUBANK, parsed!!.bank)
        assertEquals(25000000L, parsed.amount)
        assertTrue(parsed.isIncome)
        assertEquals(32500000L, parsed.balance)
        assertEquals("9012", parsed.cardNumber)
        assertEquals("درآمد و حقوق", parsed.suggestedCategory)
    }

    // ۳. کارت به کارت شتابی (بانک ملی)
    @Test
    fun testCardToCardTransfer_melli() {
        val sms = """
            بانک ملی ایران
            برداشت: انتقال کارت به کارت
            مبلغ: 5,000,000 ریال
            به کارت: 603799******4321
            از کارت: 603799******1122
            مانده: 14,000,000 ریال
        """.trimIndent()

        val parsed = parser.parse(sms)
        assertNotNull(parsed)
        assertEquals(BankType.MELLI, parsed!!.bank)
        assertEquals(500000L, parsed.amount)
        assertFalse(parsed.isIncome)
        assertEquals(1400000L, parsed.balance)
        // کارت مبدا استخراج می‌شود
        assertEquals("1122", parsed.cardNumber)
    }

    // ۴. واریز ساتنا (بانک آینده)
    @Test
    fun testSatnaIncome_ayandeh() {
        val sms = """
            بانک آینده
            واریز ساتنا
            مبلغ: 150,000,000 ریال
            به حساب: ...4455
            مانده: 210,000,000 ریال
            شرح: تسویه قرارداد
        """.trimIndent()

        val parsed = parser.parse(sms)
        assertNotNull(parsed)
        assertEquals(BankType.AYANDEH, parsed!!.bank)
        // 150,000,000 ریال = 15,000,000 تومان
        assertEquals(15000000L, parsed.amount)
        assertTrue(parsed.isIncome)
        assertEquals(21000000L, parsed.balance)
        assertEquals("4455", parsed.cardNumber)
    }

    // ۵. واریز سود سپرده بانکی (بانک شهر)
    @Test
    fun testDepositInterest_shahr() {
        val sms = """
            بانک شهر
            واریز سود سپرده ماهانه
            مبلغ: 1,200,000 ریال
            حساب: 70081234
            مانده: 11,200,000 ریال
        """.trimIndent()

        val parsed = parser.parse(sms)
        assertNotNull(parsed)
        assertEquals(BankType.SHAHR, parsed!!.bank)
        assertEquals(120000L, parsed.amount)
        assertTrue(parsed.isIncome)
        assertEquals(1120000L, parsed.balance)
        assertEquals("1234", parsed.cardNumber)
    }

    // ۶. برگشت وجه خرید (ویپاد نئوبانک)
    @Test
    fun testRefundIncome_wepod() {
        val sms = """
            ویپاد
            واریز: برگشت وجه خرید دیجی کالا
            مبلغ: +450,000 تومان
            مانده: 1,850,000 تومان
        """.trimIndent()

        val parsed = parser.parse(sms)
        assertNotNull(parsed)
        assertEquals(BankType.WEPOD, parsed!!.bank)
        assertEquals(450000L, parsed.amount)
        assertTrue(parsed.isIncome)
        assertEquals(1850000L, parsed.balance)
    }

    // ۷. پرداخت قبض (بانک صادرات)
    @Test
    fun testBillPayment_saderat() {
        val sms = """
            بانک صادرات ایران
            برداشت: پرداخت قبض برق
            مبلغ: 85,000 تومان
            از کارت: ...5566
            مانده: 900,000 تومان
        """.trimIndent()

        val parsed = parser.parse(sms)
        assertNotNull(parsed)
        assertEquals(BankType.SADERAT, parsed!!.bank)
        assertEquals(85000L, parsed.amount)
        assertFalse(parsed.isIncome)
        assertEquals(900000L, parsed.balance)
        assertEquals("5566", parsed.cardNumber)
        assertEquals("قبوض و ارتباطات", parsed.suggestedCategory)
    }

    // ۸. تراکنش با علامت منفی نئوبانکی (باجت)
    @Test
    fun testNegativeExpense_bajet() {
        val sms = """
            باجت: -180,000 تومان
            خرید از کافه لمیز
            مانده: 620,000 تومان
        """.trimIndent()

        val parsed = parser.parse(sms)
        assertNotNull(parsed)
        assertEquals(BankType.BAJET, parsed!!.bank)
        assertEquals(180000L, parsed.amount)
        assertFalse(parsed.isIncome)
        assertEquals(620000L, parsed.balance)
        assertEquals("رستوران و کافه", parsed.suggestedCategory)
    }
}
