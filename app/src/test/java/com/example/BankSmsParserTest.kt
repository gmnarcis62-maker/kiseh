package com.example

import com.example.sms.BankSmsParser
import com.example.sms.BankType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BankSmsParserTest {

    private lateinit var parser: BankSmsParser

    @Before
    fun setUp() {
        parser = BankSmsParser()
    }

    // =========================================================================
    // ۱. تست پیامک غیربانکی (کد یکبار مصرف، تبلیغات، چت معمولی)
    // =========================================================================
    @Test
    fun testNonBankSms_returnsNull() {
        // الف: کد تایید و رمز پویا
        val otpSms1 = "بانک سامان: رمز پویا خرید اینترنتی شما 842195 معتبر تا 120 ثانیه"
        assertNull(parser.parse(otpSms1))

        val otpSms2 = "کد تایید ورود به برنامه کیسه: 1234"
        assertNull(parser.parse(otpSms2))

        // ب: پیامک تبلیغاتی اپراتورها
        val promoSms = "مشترک گرامی، با شارژ شگفت انگیز 20% اعتبار هدیه دریافت کنید. همراه اول"
        assertNull(parser.parse(promoSms))

        // ج: پیامک متنی شخصی بدون اصطلاحات تراکنش بانکی
        val personalSms = "سلام، فردا ساعت ۵ جلسه برگزار میشه."
        assertNull(parser.parse(personalSms))

        // د: پیامک امنیتی ورود به اینترنت بانک و همراه بانک
        val loginAlert1 = "بانک ملی: ورود به همراه بانک بام در تاریخ 1403/05/12 ساعت 14:32 با موفقیت انجام شد."
        assertNull(parser.parse(loginAlert1))

        val loginAlert2 = "بانک ملت: ورود به سامانه اینترنت بانک. در صورت عدم اقدام با پشتیبانی تماس بگیرید."
        assertNull(parser.parse(loginAlert2))

        // تغییر رمز و مسدودی کارت
        val passwordChangeAlert = "بانک سپه: رمز اول کارت شما با موفقیت تغییر یافت. اگر شما اقدام نکرده‌اید تماس بگیرید."
        assertNull(parser.parse(passwordChangeAlert))

        // ه: پیامک صرفاً اعلام موجودی و مانده حساب بدون هرگونه تراکنش
        val balanceOnlySms = "موجودی حساب شما در بانک سامان: 15,400,000 ریال"
        assertNull(parser.parse(balanceOnlySms))

        // و: پیامک غیرمالی دیگر
        val nonFinancialSms = "مشتری گرامی، شعبه جدید بانک پاسارگاد در بلوار اندرزگو افتتاح گردید."
        assertNull(parser.parse(nonFinancialSms))
    }

    // =========================================================================
    // تست پایا، ساتنا، کارت به کارت و کارت‌های ماسک‌شده
    // =========================================================================
    @Test
    fun testPayaAndSatnaTransactions() {
        // واریز پایا
        val payaSms = """
            بانک پاسارگاد
            واریز پایا به حساب: 2198000
            مبلغ: 35,000,000 ریال
            شرح: واریز حقوق مهر ماه
            مانده: 42,500,000 ریال
        """.trimIndent()
        val payaParsed = parser.parse(payaSms)
        assertNotNull(payaParsed)
        assertEquals(3500000L, payaParsed!!.amount)
        assertTrue(payaParsed.isIncome)
        assertEquals("درآمد و حقوق", payaParsed.suggestedCategory)

        // ساتنا
        val satnaSms = """
            بانک سامان
            برداشت ساتنا
            مبلغ: 120,000,000 ریال
            از کارت: 6219-8610-****-1199
            مانده: 5,000,000 ریال
        """.trimIndent()
        val satnaParsed = parser.parse(satnaSms)
        assertNotNull(satnaParsed)
        assertEquals(12000000L, satnaParsed!!.amount)
        assertFalse(satnaParsed.isIncome)
        assertEquals("1199", satnaParsed.cardNumber)

        // کارت به کارت
        val c2cSms = """
            بانک ملت
            کارت به کارت به 603799******8821
            برداشت: 500,000 تومان
            از: *4431
            مانده: 2,300,000 تومان
        """.trimIndent()
        val c2cParsed = parser.parse(c2cSms)
        assertNotNull(c2cParsed)
        assertEquals(500000L, c2cParsed!!.amount)
        assertFalse(c2cParsed.isIncome)
        assertEquals("4431", c2cParsed.cardNumber)
    }

    // =========================================================================
    // ۲. تست بانک ملی ایران (برداشت و ریال و مانده)
    // =========================================================================
    @Test
    fun testBankMelli_expenseInRial() {
        val sms = """
            بانک ملی ایران
            برداشت مبلغ 1,500,000 ریال
            از حساب: *1234
            مانده: 12,000,000 ریال
            پذیرنده: فروشگاه افق کوروش
        """.trimIndent()

        val result = parser.parse(sms)
        assertNotNull(result)
        assertEquals(BankType.MELLI, result!!.bank)
        // 1,500,000 ریال = 150,000 تومان
        assertEquals(150000L, result.amount)
        assertFalse("باید هزینه/برداشت باشد", result.isIncome)
        // مانده: 12,000,000 ریال = 1,200,000 تومان
        assertEquals(1200000L, result.balance)
        assertEquals("1234", result.cardNumber)
        assertEquals("خوراک", result.suggestedCategory)
    }

    // =========================================================================
    // ۳. تست بانک ملت (اعداد فارسی و انگلیسی، هزینه با پذیرنده اسنپ)
    // =========================================================================
    @Test
    fun testBankMellat_persianDigitsAndSnapp() {
        val sms = """
            بانک ملت
            برداشت: ۲,۳۵۰,۰۰۰ ریال
            از: 585983***4589
            مانده: ۱۰,۰۰۰,۰۰۰ ریال
            پذیرنده: اسنپ
        """.trimIndent()

        val result = parser.parse(sms)
        assertNotNull(result)
        assertEquals(BankType.MELLAT, result!!.bank)
        // ۲,۳۵۰,۰۰۰ ریال = ۲۳۵,۰۰۰ تومان
        assertEquals(235000L, result.amount)
        assertFalse(result.isIncome)
        assertEquals(1000000L, result.balance)
        assertEquals("4589", result.cardNumber)
        assertEquals("حمل و نقل", result.suggestedCategory)
    }

    // =========================================================================
    // ۴. تست بانک سامان (واحد تومان و واریز)
    // =========================================================================
    @Test
    fun testBankSaman_incomeInToman() {
        val sms = """
            بانک سامان
            واریز مبلغ 4,500,000 تومان
            به حساب ...7821
            موجودی: 18,200,000 تومان
            شرح: حقوق ماهانه
        """.trimIndent()

        val result = parser.parse(sms)
        assertNotNull(result)
        assertEquals(BankType.SAMAN, result!!.bank)
        // واحد تومان است، بدون تبدیل
        assertEquals(4500000L, result.amount)
        assertTrue("باید واریز/درآمد باشد", result.isIncome)
        assertEquals(18200000L, result.balance)
        assertEquals("7821", result.cardNumber)
        assertEquals("درآمد و حقوق", result.suggestedCategory)
    }

    // =========================================================================
    // ۵. تست بانک پاسارگاد (خرید فست فود و تبدیل ریال)
    // =========================================================================
    @Test
    fun testBankPasargad_fastfoodExpense() {
        val sms = """
            بانک پاسارگاد
            برداشت از حساب: 9942
            مبلغ: 850,000 ریال
            مانده: 5,100,000 ریال
            پذیرنده: فست فود عطاویچ
        """.trimIndent()

        val result = parser.parse(sms)
        assertNotNull(result)
        assertEquals(BankType.PASARGAD, result!!.bank)
        // 850,000 ریال = 85,000 تومان
        assertEquals(85000L, result.amount)
        assertFalse(result.isIncome)
        assertEquals("رستوران و کافه", result.suggestedCategory)
    }

    // =========================================================================
    // ۶. تست بانک تجارت (پرداخت قبض)
    // =========================================================================
    @Test
    fun testBankTejarat_billPayment() {
        val sms = """
            بانک تجارت
            برداشت 120,000 ریال
            از حساب ...6632
            پذیرنده: همراه اول
            مانده: 3,400,000 ریال
        """.trimIndent()

        val result = parser.parse(sms)
        assertNotNull(result)
        assertEquals(BankType.TEJARAT, result!!.bank)
        // 120,000 ریال = 12,000 تومان
        assertEquals(12000L, result.amount)
        assertFalse(result.isIncome)
        assertEquals("قبوض و ارتباطات", result.suggestedCategory)
    }

    // =========================================================================
    // ۷. تست بانک صادرات ایران (واریز به حساب و اعداد تمام فارسی)
    // =========================================================================
    @Test
    fun testBankSaderat_incomePersian() {
        val sms = """
            بانک صادرات ایران
            واریز به سپهر کارت ...۳۳۲۱
            مبلغ ۵,۰۰۰,۰۰۰ ریال
            مانده ۲۵,۰۰۰,۰۰۰ ریال
        """.trimIndent()

        val result = parser.parse(sms)
        assertNotNull(result)
        assertEquals(BankType.SADERAT, result!!.bank)
        // ۵,۰۰۰,۰۰۰ ریال = ۵۰۰,۰۰۰ تومان
        assertEquals(500000L, result.amount)
        assertTrue(result.isIncome)
        assertEquals(2500000L, result.balance)
        assertEquals("3321", result.cardNumber)
    }

    // =========================================================================
    // ۸. تست بانک سپه (برداشت خرید سوپرمارکت)
    // =========================================================================
    @Test
    fun testBankSepah_groceryExpense() {
        val sms = """
            بانک سپه
            برداشت: خرید
            مبلغ: 3,200,000 ریال
            از کارت: 589210******7741
            مانده: 8,400,000 ریال
            پذیرنده: سوپرمارکت بهار
        """.trimIndent()

        val result = parser.parse(sms)
        assertNotNull(result)
        assertEquals(BankType.SEPAH, result!!.bank)
        assertEquals(320000L, result.amount)
        assertFalse(result.isIncome)
        assertEquals(840000L, result.balance)
        assertEquals("7741", result.cardNumber)
        assertEquals("خوراک", result.suggestedCategory)
    }

    // =========================================================================
    // ۹. تست بلوبانک (نئوبانک با واحد تومان)
    // =========================================================================
    @Test
    fun testBluBank_transferToman() {
        val sms = """
            بلوبانک
            خرید از کافه لمیز
            مبلغ: 95,000 تومان
            مانده: 1,420,000 تومان
        """.trimIndent()

        val result = parser.parse(sms)
        assertNotNull(result)
        assertEquals(BankType.BLUBANK, result!!.bank)
        assertEquals(95000L, result.amount)
        assertFalse(result.isIncome)
        assertEquals(1420000L, result.balance)
        assertEquals("رستوران و کافه", result.suggestedCategory)
    }

    // =========================================================================
    // ۱۰. تست قرض‌الحسنه مهر ایران (واریز وام)
    // =========================================================================
    @Test
    fun testMehrIran_loanDeposit() {
        val sms = """
            بانک قرض‌الحسنه مهر ایران
            واریز تسهیلات
            مبلغ: 50,000,000 ریال
            به حساب: 231456
            مانده: 55,000,000 ریال
        """.trimIndent()

        val result = parser.parse(sms)
        assertNotNull(result)
        assertEquals(BankType.MEHR_IRAN, result!!.bank)
        assertEquals(5000000L, result.amount)
        assertTrue(result.isIncome)
        assertEquals(5500000L, result.balance)
        assertEquals("1456", result.cardNumber)
    }

    // =========================================================================
    // ۱۱. تست ویپاد (نئوبانک پاسارگاد)
    // =========================================================================
    @Test
    fun testWepod_incomeToman() {
        val sms = """
            ویپاد
            واریز پایا: حقوق ماهانه
            مبلغ: +12,000,000 تومان
            مانده: 15,300,000 تومان
        """.trimIndent()

        val result = parser.parse(sms)
        assertNotNull(result)
        assertEquals(BankType.WEPOD, result!!.bank)
        assertEquals(12000000L, result.amount)
        assertTrue(result.isIncome)
        assertEquals(15300000L, result.balance)
        assertEquals("درآمد و حقوق", result.suggestedCategory)
    }
}
