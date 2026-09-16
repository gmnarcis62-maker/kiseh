package com.example

import com.example.sms.BankSmsParser
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class FalsePositiveSmsTest {

    private lateinit var parser: BankSmsParser

    @Before
    fun setUp() {
        parser = BankSmsParser()
    }

    // ۱. رمز پویا و کدهای تایید
    @Test
    fun testOtpAndVerificationCodes_returnsNull() {
        assertNull(parser.parse("بانک ملی: رمز پویا یکبار مصرف شما 981245 معتبر تا 120 ثانیه"))
        assertNull(parser.parse("رمز یکبارمصرف خرید اینترنتی بانک ملت: 541289"))
        assertNull(parser.parse("کد تایید فعال‌سازی همراه بانک تجارت: 45678"))
        assertNull(parser.parse("رمز موقت ورود شما: 991200"))
        assertNull(parser.parse("کد ورود به برنامه آبانک: 3412"))
    }

    // ۲. ورود به اینترنت بانک و همراه بانک
    @Test
    fun testInternetBankLogin_returnsNull() {
        assertNull(parser.parse("بانک سپه: ورود به اینترنت بانک در تاریخ 1403/06/10 ساعت 18:22 انجام شد."))
        assertNull(parser.parse("بانک صادرات: ورود به همراه بانک بام با موفقیت انجام شد."))
        assertNull(parser.parse("بانک پاسارگاد: ورود به سامانه بانکداری مجازی موفقیت‌آمیز بود."))
        assertNull(parser.parse("ورود موفق به اپلیکیشن بلوبانک"))
    }

    // ۳. تغییر رمز و امور کارت
    @Test
    fun testPasswordChangeAndCardServices_returnsNull() {
        assertNull(parser.parse("بانک شهر: تغییر رمز اول کارت شما با موفقیت انجام شد."))
        assertNull(parser.parse("بانک آینده: کارت شما به درخواست دارنده کارت مسدود شد."))
        assertNull(parser.parse("بانک مسکن: صدور کارت المثنی برای حساب شما انجام گردید."))
    }

    // ۴. جشنواره‌ها، قرعه‌کشی و تبلیغات
    @Test
    fun testPromotionsAndFestivals_returnsNull() {
        assertNull(parser.parse("جشنواره بزرگ حساب‌های قرض‌الحسنه بانک کشاورزی با جوایز میلیاردی"))
        assertNull(parser.parse("قرعه کشی حساب‌های پس‌انداز بانک رفاه کارگران آغاز شد."))
        assertNull(parser.parse("نسخه جدید همراه بانک تجارت منتشر شد. جهت بروزرسانی کلیک کنید."))
        assertNull(parser.parse("مشتری گرامی، شعبه جدید بانک اقتصاد نوین در زعفرانیه افتتاح شد."))
    }

    // ۵. وام و تسهیلات بدون وقوع واریز
    @Test
    fun testLoanAnnouncements_returnsNull() {
        assertNull(parser.parse("مشتری گرامی، با درخواست تسهیلات شما در بانک رسالت موافقت شد."))
        assertNull(parser.parse("بانک مهر ایران: پرونده تسهیلاتی شما در انتظار تکمیل مدارک است."))
    }
}
