package com.example

import com.example.sms.BankSmsParser
import com.example.sms.BankType
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class BankDetectionTest {

    private lateinit var parser: BankSmsParser

    @Before
    fun setUp() {
        parser = BankSmsParser()
    }

    @Test
    fun testAllBanksDetectionFromTextWithoutSender() {
        // ۱. بانک ملی
        assertEquals(BankType.MELLI, BankType.fromText("بانک ملی: برداشت 500,000 ریال"))
        // ۲. بانک ملت
        assertEquals(BankType.MELLAT, BankType.fromText("بانک ملت: واریز 1,000,000 ریال"))
        // ۳. بانک سامان
        assertEquals(BankType.SAMAN, BankType.fromText("بانک سامان: خرید 25,000 تومان"))
        // ۴. بانک پاسارگاد
        assertEquals(BankType.PASARGAD, BankType.fromText("بانک پاسارگاد: برداشت 120,000 ریال"))
        // ۵. بانک تجارت
        assertEquals(BankType.TEJARAT, BankType.fromText("بانک تجارت: واریز 300,000 ریال"))
        // ۶. بانک صادرات
        assertEquals(BankType.SADERAT, BankType.fromText("بانک صادرات ایران: برداشت 400,000 ریال"))
        // ۷. بانک سپه
        assertEquals(BankType.SEPAH, BankType.fromText("بانک سپه: واریز حقوق 8,000,000 ریال"))
        // ۸. بانک کشاورزی
        assertEquals(BankType.KESHAVARZI, BankType.fromText("بانک کشاورزی: برداشت 200,000 ریال"))
        // ۹. بانک مسکن
        assertEquals(BankType.MASKAN, BankType.fromText("بانک مسکن: پرداخت قسط 1,500,000 ریال"))
        // ۱۰. بانک آینده
        assertEquals(BankType.AYANDEH, BankType.fromText("بانک آینده: خرید 80,000 تومان"))
        // ۱۱. بانک شهر
        assertEquals(BankType.SHAHR, BankType.fromText("بانک شهر: واریز 650,000 ریال"))
        // ۱۲. بانک رفاه کارگران
        assertEquals(BankType.REFAH, BankType.fromText("بانک رفاه کارگران: واریز مستمری 9,500,000 ریال"))
        // ۱۳. بانک پارسیان
        assertEquals(BankType.PARSIAN, BankType.fromText("بانک پارسیان: برداشت 350,000 ریال"))
        // ۱۴. بانک اقتصاد نوین
        assertEquals(BankType.EGHTESAD_NOVIN, BankType.fromText("بانک اقتصاد نوین: خرید 180,000 تومان"))
        // ۱۵. بانک سینا
        assertEquals(BankType.SINA, BankType.fromText("بانک سینا: واریز 220,000 ریال"))
        // ۱۶. بانک دی
        assertEquals(BankType.DEY, BankType.fromText("بانک دی: برداشت 140,000 ریال"))
        // ۱۷. بانک سرمایه
        assertEquals(BankType.SARMAYEH, BankType.fromText("بانک سرمایه: واریز 750,000 ریال"))
        // ۱۸. بانک کارآفرین
        assertEquals(BankType.KARAFARIN, BankType.fromText("بانک کارآفرین: برداشت 900,000 ریال"))
        // ۱۹. بانک گردشگری
        assertEquals(BankType.GARDESHGARI, BankType.fromText("بانک گردشگری: خرید 45,000 تومان"))
        // ۲۰. بانک ایران زمین
        assertEquals(BankType.IRANZAMIN, BankType.fromText("بانک ایران زمین: واریز 550,000 ریال"))
        // ۲۱. بانک خاورمیانه
        assertEquals(BankType.MIDDLE_EAST, BankType.fromText("بانک خاورمیانه: برداشت 1,200,000 ریال"))
        // ۲۲. قرض‌الحسنه مهر ایران
        assertEquals(BankType.MEHR_IRAN, BankType.fromText("بانک قرض‌الحسنه مهر ایران: واریز وام 10,000,000 ریال"))
        // ۲۳. قرض‌الحسنه رسالت
        assertEquals(BankType.RESALAT, BankType.fromText("بانک قرض‌الحسنه رسالت: برداشت 300,000 ریال"))
        // ۲۴. پست بانک
        assertEquals(BankType.POST_BANK, BankType.fromText("پست بانک ایران: خرید 70,000 ریال"))
        // ۲۵. توسعه تعاون
        assertEquals(BankType.TOSE_TAAVON, BankType.fromText("بانک توسعه تعاون: واریز 850,000 ریال"))
        // ۲۶. صنعت و معدن
        assertEquals(BankType.SANAT_VA_MADAN, BankType.fromText("بانک صنعت و معدن: برداشت 4,000,000 ریال"))
        // ۲۷. توسعه صادرات
        assertEquals(BankType.TOSE_SADERAT, BankType.fromText("بانک توسعه صادرات ایران: واریز 15,000,000 ریال"))
        // ۲۸. بلوبانک
        assertEquals(BankType.BLUBANK, BankType.fromText("بلوبانک: خرید 50,000 تومان سوپرمارکت"))
        // ۲۹. ویپاد
        assertEquals(BankType.WEPOD, BankType.fromText("ویپاد: واریز 120,000 تومان پایا"))
        // ۳۰. باجت
        assertEquals(BankType.BAJET, BankType.fromText("باجت: برداشت 35,000 تومان خرید شارژ"))
        // ۳۱. آبانک
        assertEquals(BankType.ABANK, BankType.fromText("آبانک: انتقال وجه 90,000 تومان"))
    }

    @Test
    fun testBankDetectionFromSender() {
        assertEquals(BankType.BLUBANK, BankType.fromText("خرید 40,000 تومان", "BLUBANK"))
        assertEquals(BankType.WEPOD, BankType.fromText("واریز 100,000 تومان", "wepod"))
        assertEquals(BankType.SEPAH, BankType.fromText("برداشت 500,000 ریال", "BankSepah"))
        assertEquals(BankType.KESHAVARZI, BankType.fromText("واریز 2,000,000 ریال", "BKI"))
        assertEquals(BankType.POST_BANK, BankType.fromText("خرید 150,000 ریال", "PostBank"))
        assertEquals(BankType.MEHR_IRAN, BankType.fromText("واریز 3,000,000 ریال", "QMB"))
        assertEquals(BankType.RESALAT, BankType.fromText("برداشت 400,000 ریال", "RQBank"))
    }

    @Test
    fun testSupportedBanksCount() {
        assertEquals(31, BankType.supportedBanks.size)
    }
}
