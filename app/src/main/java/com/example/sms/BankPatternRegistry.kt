package com.example.sms

import java.util.regex.Pattern

/**
 * رجیستری الگوهای Regex، شناسه‌های اختصاصی و قواعد پردازش پیامک‌های تمامی بانک‌های کشور
 */
object BankPatternRegistry {

    /**
     * پروفایل ویژگی‌ها و قواعد اختصاصی هر بانک و نئوبانک
     */
    data class BankProfile(
        val bank: BankType,
        val senderIdentifiers: List<String>,
        val keywords: List<String>,
        val defaultIsRial: Boolean = true
    )

    /**
     * الگوهای پیامک‌های غیرتراکنشی که باید به عنوان False Positive فیلتر شوند.
     * مانند رمز پویا، ورود به اینترنت بانک، تغییر رمز، اطلاعیه‌ها، تبلیغات و غیره.
     */
    val NON_TRANSACTION_KEYWORDS = listOf(
        // رمزهای پویا و کدهای تایید موقت
        "رمز پویا",
        "رمز یکبار مصرف",
        "رمز یکبارمصرف",
        "رمز یک‌بار مصرف",
        "رمز موقت",
        "کد تایید",
        "کد تأیید",
        "کد فعالسازی",
        "کد فعال‌سازی",
        "کد ورود",

        // ورود به سامانه‌ها و خدمات الکترونیک
        "ورود به اینترنت بانک",
        "ورود به اینترنت‌بانک",
        "ورود به همراه بانک",
        "ورود به همراه‌بانک",
        "ورود به موبایل بانک",
        "ورود به سامانه",
        "ورود به برنامه",
        "ورود به اپلیکیشن",
        "ورود به وب‌سایت",
        "ورود به وبسایت",
        "ورود به بام",
        "ورود به همراه کارت",
        "ورود موفق",
        "تغییر رمز",
        "تغییر اطلاعات",
        "فراموشی رمز",

        // امور اداری، تسهیلات و کارت
        "افتتاح حساب",
        "صدور کارت",
        "تمدید کارت",
        "مسدودی کارت",
        "مسدود شد",
        "رفع مسدودی",
        "هشدار امنیتی",
        "درخواست تسهیلات",
        "درخواست وام",
        "تسهیلات شما",
        "پرونده تسهیلاتی",

        // اطلاع‌رسانی‌های عمومی، نظرسنجی و تبلیغات
        "نظرسنجی",
        "جشنواره",
        "قرعه کشی",
        "قرعه‌کشی",
        "نسخه جدید همراه بانک",
        "نسخه جدید همراه‌بانک",
        "شعبه جدید",
        "مشتری گرامی با سلام",
        "تبریک",
        "تسلیت"
    )

    /**
     * کلمات کلیدی که نشان‌دهنده یک تراکنش مالی بانکی معتبر هستند
     */
    val BANK_TRANSACTION_KEYWORDS = listOf(
        "برداشت",
        "واریز",
        "خرید",
        "انتقال",
        "کارت به کارت",
        "کارت‌به‌کارت",
        "پایا",
        "ساتنا",
        "مانده",
        "موجودی",
        "موجودي",
        "مبلغ",
        "بدهکار",
        "بستانکار",
        "پایانه",
        "کارتخوان",
        "پذیرنده",
        "سپرده",
        "کارت",
        "حساب",
        "حقوق",
        "سود",
        "سود سپرده",
        "برگشت وجه",
        "قبض",
        "پرداخت قبض",
        "پرداخت قبوض",
        "پرداخت",
        "کارمزد",
        "قسط",
        "اقساط",
        "شارژ"
    )

    /**
     * کلمات کلیدی درآمد / واریز
     */
    val INCOME_KEYWORDS = listOf(
        "واریز",
        "واریزی",
        "انتقال از",
        "بستانکار",
        "ورود وجه",
        "حقوق",
        "سود سپرده",
        "سود سهام",
        "سود",
        "برگشت وجه",
        "شارژ کیف",
        "واریز شد",
        "دریافت وجه",
        "واریز حقوق",
        "credit",
        "deposit",
        "received"
    )

    /**
     * کلمات کلیدی هزینه / برداشت
     */
    val EXPENSE_KEYWORDS = listOf(
        "برداشت",
        "خرید",
        "خرید اینترنتی",
        "خرید شارژ",
        "خرید از",
        "انتقال به",
        "بدهکار",
        "خروج وجه",
        "کسر",
        "کسر شد",
        "پرداخت قبض",
        "پرداخت",
        "کارمزد",
        "اقساط",
        "قسط",
        "برداشت شد",
        "انتقال وجه به",
        "debit",
        "withdraw",
        "payment"
    )

    /**
     * جدول پروفایل‌های اختصاصی تمام ۳۱ بانک و نئوبانک شبکه شتاب ایران
     */
    val BANK_PROFILES: Map<BankType, BankProfile> = mapOf(
        BankType.MELLI to BankProfile(BankType.MELLI, listOf("melli", "bmi", "bankmelli"), listOf("ملی", "bmi", "بام")),
        BankType.MELLAT to BankProfile(BankType.MELLAT, listOf("mellat", "bankmellat"), listOf("ملت")),
        BankType.SAMAN to BankProfile(BankType.SAMAN, listOf("saman", "sb24"), listOf("سامان"), defaultIsRial = false),
        BankType.PASARGAD to BankProfile(BankType.PASARGAD, listOf("pasargad", "bpi"), listOf("پاسارگاد")),
        BankType.TEJARAT to BankProfile(BankType.TEJARAT, listOf("tejarat", "banktejarat"), listOf("تجارت")),
        BankType.SADERAT to BankProfile(BankType.SADERAT, listOf("saderat", "bsi"), listOf("صادرات", "سپهر")),
        BankType.SEPAH to BankProfile(BankType.SEPAH, listOf("sepah", "banksepah"), listOf("سپه", "انصار", "قوامین", "حکمت", "کوثر")),
        BankType.KESHAVARZI to BankProfile(BankType.KESHAVARZI, listOf("keshavarzi", "bki", "agribank"), listOf("کشاورزی")),
        BankType.MASKAN to BankProfile(BankType.MASKAN, listOf("maskan", "bankmaskan"), listOf("مسکن")),
        BankType.AYANDEH to BankProfile(BankType.AYANDEH, listOf("ayandeh", "ba"), listOf("آینده")),
        BankType.SHAHR to BankProfile(BankType.SHAHR, listOf("shahr", "citybank", "shahrbank"), listOf("شهر", "شهرنت")),
        BankType.REFAH to BankProfile(BankType.REFAH, listOf("refah", "bankrefah"), listOf("رفاه")),
        BankType.PARSIAN to BankProfile(BankType.PARSIAN, listOf("parsian", "parsianbank"), listOf("پارسیان")),
        BankType.EGHTESAD_NOVIN to BankProfile(BankType.EGHTESAD_NOVIN, listOf("enbank", "eghtesad"), listOf("اقتصاد نوین")),
        BankType.SINA to BankProfile(BankType.SINA, listOf("sina", "sinabank"), listOf("سینا")),
        BankType.DEY to BankProfile(BankType.DEY, listOf("dey", "day", "daybank"), listOf("بانک دی")),
        BankType.SARMAYEH to BankProfile(BankType.SARMAYEH, listOf("sarmayeh", "sbank"), listOf("سرمایه")),
        BankType.KARAFARIN to BankProfile(BankType.KARAFARIN, listOf("karafarin"), listOf("کارآفرین")),
        BankType.GARDESHGARI to BankProfile(BankType.GARDESHGARI, listOf("gardeshgari", "tourismbank"), listOf("گردشگری")),
        BankType.IRANZAMIN to BankProfile(BankType.IRANZAMIN, listOf("iranzamin", "izbank"), listOf("ایران زمین")),
        BankType.MIDDLE_EAST to BankProfile(BankType.MIDDLE_EAST, listOf("mebank", "middleeast"), listOf("خاورمیانه")),
        BankType.MEHR_IRAN to BankProfile(BankType.MEHR_IRAN, listOf("qmb", "mehriran"), listOf("مهر ایران")),
        BankType.RESALAT to BankProfile(BankType.RESALAT, listOf("rqbank", "resalat"), listOf("رسالت")),
        BankType.POST_BANK to BankProfile(BankType.POST_BANK, listOf("postbank"), listOf("پست بانک")),
        BankType.TOSE_TAAVON to BankProfile(BankType.TOSE_TAAVON, listOf("ttbank"), listOf("توسعه تعاون")),
        BankType.SANAT_VA_MADAN to BankProfile(BankType.SANAT_VA_MADAN, listOf("bim"), listOf("صنعت و معدن")),
        BankType.TOSE_SADERAT to BankProfile(BankType.TOSE_SADERAT, listOf("edbi"), listOf("توسعه صادرات")),
        BankType.BLUBANK to BankProfile(BankType.BLUBANK, listOf("blubank", "blu"), listOf("بلوبانک", "بلو بانک"), defaultIsRial = false),
        BankType.WEPOD to BankProfile(BankType.WEPOD, listOf("wepod"), listOf("ویپاد"), defaultIsRial = false),
        BankType.BAJET to BankProfile(BankType.BAJET, listOf("bajet"), listOf("باجت"), defaultIsRial = false),
        BankType.ABANK to BankProfile(BankType.ABANK, listOf("abank"), listOf("آبانک"), defaultIsRial = false)
    )

    /**
     * نقشه تطبیق کلمات کلیدی به دسته‌بندی‌های فارسی
     */
    val CATEGORY_RULES = mapOf(
        "خوراک" to listOf("هایپر", "سوپر", "افق کوروش", "فروشگاه", "رفاه", "نانوایی", "میوه", "قصابی", "پروتئین", "لبنیات", "مارکت"),
        "حمل و نقل" to listOf("بنزین", "جایگاه", "سوخت", "اسنپ", "تپسی", "تاکسی", "مترو", "اتوبوس", "طرح ترافیک", "عوارض"),
        "رستوران و کافه" to listOf("کافه", "رستوران", "فست فود", "پیتزا", "ساندویچ", "قهوه", "طباخی", "کباب"),
        "قبوض و ارتباطات" to listOf("همراه اول", "ایرانسل", "رایتل", "مخابرات", "قبض آب", "قبض برق", "قبض گاز", "قبض تلفن", "اینترنت"),
        "پزشکی و سلامت" to listOf("داروخانه", "بیمارستان", "درمانگاه", "کلینیک", "پزشک", "دندانپزشکی", "آزمایشگاه", "فیزیوتراپی"),
        "پوشاک" to listOf("پوشاک", "کفش", "لباس", "کت و شلوار", "کیف", "بوتیک", "مزون", "کتونی")
    )
}
