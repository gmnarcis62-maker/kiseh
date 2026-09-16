package com.example.sms

import android.content.Context
import android.content.SharedPreferences

/**
 * منبع تشخیص دسته‌بندی
 */
enum class MatchSource {
    USER_LEARNED,      // یادگیری از انتخاب‌های قبلی کاربر
    EXACT_MERCHANT,    // تطبیق نام دقیق پذیرنده
    MERCHANT_KEYWORD,  // کلیدواژه در نام پذیرنده
    BODY_KEYWORD,      // کلیدواژه در کل متن پیامک
    FALLBACK_DEFAULT   // دسته‌بندی پیش‌فرض بر اساس نوع تراکنش
}

/**
 * خروجی تطبیق هوشمند دسته‌بندی
 */
data class CategoryMatchResult(
    val category: String,               // نام دسته‌بندی استاندارد یا تشخیصی (مانند خوراک، حمل و نقل)
    val canonicalKisehCategory: String, // دسته‌بندی منطبق بر سیستم مالی کیسه (مانند خوراکی، حمل‌ونقل)
    val confidence: Float,              // ضریب اطمینان از ۰٫۰ تا ۱٫۰
    val matchedKeyword: String? = null, // کلیدواژه‌ای که تطبیق داده شد
    val isUserLearned: Boolean = false, // آیا از رفتار کاربر یاد گرفته شده است
    val source: MatchSource = MatchSource.BODY_KEYWORD
)

/**
 * موتور هوشمند تشخیص و دسته‌بندی خودکار تراکنش‌های بانکی (Bank SMS Intelligence)
 *
 * قابلیت‌ها:
 * ۱. نرمال‌سازی عمیق متون فارسی و عربی (حذف نیم‌فاصله‌ها، تبدیل حروف، حذف نویز)
 * ۲. تشخیص دسته‌بندی بر اساس کلمات کلیدی جامع شبکه پذیرندگان و بانک‌های کشور
 * ۳. اولویت‌بندی تطبیق: پذیرنده > متن پیامک > پیش‌فرض
 * ۴. یادگیری مداوم از تصمیمات و اصلاحات کاربر (User Preference Learning)
 * ۵. سازگاری کامل با سیستم تحلیل مالی و دسته‌بندی‌های استاندارد برنامه «کیسه»
 */
object SmartCategoryMatcher {

    // دسته‌بندی‌های استاندارد رجیستری پیامک بانکی
    const val CAT_FOOD = "خوراک"
    const val CAT_RESTAURANT = "رستوران و کافه"
    const val CAT_TRANSPORT = "حمل و نقل"
    const val CAT_HEALTH = "پزشکی و سلامت"
    const val CAT_CLOTHING = "پوشاک"
    const val CAT_BILLS = "قبوض و ارتباطات"
    const val CAT_ENTERTAINMENT = "تفریح و سرگرمی"
    const val CAT_HOUSING = "مسکن و خانه"
    const val CAT_EDUCATION = "آموزش و کتاب"
    const val CAT_ONLINE_SERVICES = "خدمات آنلاین"
    const val CAT_INCOME = "درآمد و حقوق"
    const val CAT_OTHER = "سایر"

    // دسته‌بندی‌های استاندارد هسته مالی «کیسه»
    const val KISEH_FOOD = "خوراکی"
    const val KISEH_TRANSPORT = "حمل‌ونقل"
    const val KISEH_HEALTH = "درمان"
    const val KISEH_CLOTHING = "پوشاک"
    const val KISEH_BILLS = "قبض"
    const val KISEH_ENTERTAINMENT = "تفریح"
    const val KISEH_HOUSING = "مسکن"
    const val KISEH_INCOME = "درآمد"
    const val KISEH_OTHER = "متفرقه"

    /**
     * واژه‌نامه جامع کلمات کلیدی دسته‌بندی‌ها
     */
    private val CATEGORY_KEYWORD_RULES: Map<String, List<String>> = mapOf(
        CAT_FOOD to listOf(
            "هایپر", "سوپر", "افق کوروش", "فروشگاه", "رفاه", "جانبو", "اتکا", "شهروند",
            "هفت", "دیلی مارکت", "یاران دریان", "مارکت", "بقالی", "نانوایی", "نانوا", "نان",
            "سنگک", "بربری", "لواش", "تافتون", "میوه", "میوه فروشی", "تره بار", "قصابی",
            "گوشت", "مرغ", "ماهی", "پروتئین", "لبنیات", "لبنیاتی", "شیرینی", "قنادی", "کیک",
            "آجیل", "خشکبار", "خواروبار", "هایپرمارکت", "سوپرمارکت", "برنج", "شکلات", "تنقلات"
        ),
        CAT_RESTAURANT to listOf(
            "کافه", "رستوران", "فست فود", "فست‌فود", "پیتزا", "ساندویچ", "برگر", "سوخاری",
            "قهوه", "طباخی", "کله پزی", "کباب", "چلوکباب", "چلوکبابی", "دیزی", "بریانی",
            "جگرکی", "اکبرجوجه", "تهیه غذا", "کترینگ", "سفره خانه", "باربیکیو", "بستنی",
            "آبمیوه", "کافه بستنی", "اسنپ فود", "اسنپ‌فود", "عطاویچ", "شیلا", "هات داگ",
            "ناهار", "شام", "صبحانه", "شاندیز", "چایخانه"
        ),
        CAT_TRANSPORT to listOf(
            "بنزین", "پمپ بنزین", "جایگاه سوخت", "جایگاه", "گاز", "cng", "سی ان جی", "گازوئیل",
            "اسنپ", "تپسی", "تپ‌سی", "ماکسیم", "تاکسی", "تاکسیرانی", "مترو", "شارژ بلیت",
            "کارت بلیت", "اتوبوس", "بی آر تی", "brt", "طرح ترافیک", "عوارض", "عوارضی",
            "عوارض آزادراه", "ترمینال", "پایانه مسافربری", "پایانه", "فرودگاه", "بلیط هواپیما",
            "پرواز", "راه‌آهن", "راه آهن", "قطار", "رجا", "علی بابا", "فلای تودی", "مستربلیت",
            "اسنپ تریپ", "پنچرگیری", "آپاراتی", "تعویض روغنی", "مکانیکی", "اتوسرویس",
            "کارواش", "امداد خودرو", "لوازم یدکی", "سایپا", "ایران خودرو", "تایر", "لاستیک"
        ),
        CAT_HEALTH to listOf(
            "داروخانه", "دراگ استور", "بیمارستان", "درمانگاه", "کلینیک", "دندانپزشکی",
            "دندانپزشک", "آزمایشگاه", "فیزیوتراپی", "چشم پزشکی", "عینک", "رادیولوژی",
            "سونوگرافی", "تصویربرداری", "پزشک", "دکتر", "مطب", "متخصص", "اورژانس",
            "روانپزشک", "روانشناس", "داروکده", "ویزیت", "تزریقات", "ام آر آی", "دارو", "قرص"
        ),
        CAT_CLOTHING to listOf(
            "پوشاک", "کفش", "لباس", "کت و شلوار", "کیف", "بوتیک", "مزون", "کتونی",
            "مانتو", "روسری", "شال", "شلوار", "پیراهن", "لباس زیر", "خیاطی", "پارچه",
            "چرم", "بانی مد", "دیجی استایل", "هاکوپیان", "ال سی وایکیکی", "کفاشی", "جوراب", "کاپشن"
        ),
        CAT_BILLS to listOf(
            "همراه اول", "ایرانسل", "رایتل", "مخابرات", "قبض آب", "قبض برق", "قبض گاز",
            "قبض تلفن", "اینترنت", "شاتل", "آسیاتک", "صبانت", "پارس آنلاین", "های وب",
            "پیشگامان", "تلفن ثابت", "بسته اینترنت", "شارژ سیمکارت", "شارژ", "آب و فاضلاب",
            "شرکت برق", "شرکت گاز", "شهرداری", "نوسازی", "پسماند", "قبض"
        ),
        CAT_ENTERTAINMENT to listOf(
            "سینما", "پردیس سینمایی", "تئاتر", "کنسرت", "استخر", "سونا", "باشگاه",
            "بدنسازی", "فیتنس", "بولینگ", "بیلیارد", "شهربازی", "پارک", "باغ وحش",
            "موزه", "اتاق فرار", "گیم نت", "پلی استیشن", "هتل", "اقامتگاه", "ویلا",
            "جاجیگا", "شب", "اتاقک", "تور", "تور گردشگری", "سفر", "بازی"
        ),
        CAT_HOUSING to listOf(
            "اجاره", "اجاره بها", "ودیعه", "رهن", "مشاور املاک", "بنگاه املاک", "املاک",
            "شارژ ساختمان", "شارژ واحد", "تاسیسات", "لوله کشی", "برقکاری", "کلیدسازی",
            "نقاشی ساختمان", "دکوراسیون", "مبل", "مبلمان", "فرش", "قالیشویی", "لوازم خانگی",
            "اسنوا", "دوو", "خانه", "آپارتمان"
        ),
        CAT_EDUCATION to listOf(
            "دانشگاه", "شهریه", "مدرسه", "دبیرستان", "آموزشگاه", "کنکور", "قلم چی", "گاج",
            "کتاب", "کتابفروشی", "انتشارات", "نشر", "نمایشگاه کتاب", "دوره آموزشی",
            "فرادرس", "مکتب خونه", "کوئرا", "کانون زبان"
        ),
        CAT_ONLINE_SERVICES to listOf(
            "دیجی کالا", "دیجیکالا", "اسنپ مارکت", "ترب", "ایمالز", "تکنولایف", "باسلام",
            "کافه بازار", "مایکت", "فیلیمو", "نماوا", "فیلم نت", "زرین پال", "آپ", "تاپ", "بله"
        ),
        CAT_INCOME to listOf(
            "حقوق", "دستمزد", "واریز حقوق", "پاداش", "عیدی", "سنوات", "تسویه حساب",
            "سود سهام", "سود سپرده", "واریز پایا", "واریز ساتنا", "دریافتی", "کارکرد", "پورسانت"
        )
    )

    /**
     * تطبیق هوشمند متن و پذیرنده به دسته‌بندی مناسب
     *
     * @param text متن کامل یا بخش توضیحات پیامک
     * @param merchant نام پذیرنده یا فروشگاه (در صورت وجود)
     * @param isIncome آیا تراکنش واریز است
     * @param userLearnedCategory دسته‌بندی یاد گرفته شده از کاربر (در صورت وجود)
     */
    fun matchCategory(
        text: String,
        merchant: String = "",
        isIncome: Boolean = false,
        userLearnedCategory: String? = null
    ): CategoryMatchResult {
        // ۱. اگر کاربر قبلاً برای این پذیرنده یا الگو دسته‌بندی اختصاص داده باشد (بالاترین اولویت)
        if (!userLearnedCategory.isNullOrBlank()) {
            return CategoryMatchResult(
                category = userLearnedCategory,
                canonicalKisehCategory = toCanonicalKisehCategory(userLearnedCategory),
                confidence = 1.0f,
                matchedKeyword = merchant.ifBlank { text.take(20) },
                isUserLearned = true,
                source = MatchSource.USER_LEARNED
            )
        }

        // ۲. اگر تراکنش واریز / درآمد باشد
        if (isIncome) {
            val normalizedText = normalizeText(text)
            val matchedIncomeKeyword = CATEGORY_KEYWORD_RULES[CAT_INCOME]?.firstOrNull {
                normalizedText.contains(normalizeText(it))
            }
            return CategoryMatchResult(
                category = CAT_INCOME,
                canonicalKisehCategory = KISEH_INCOME,
                confidence = if (matchedIncomeKeyword != null) 0.95f else 0.85f,
                matchedKeyword = matchedIncomeKeyword ?: "واریز",
                source = MatchSource.BODY_KEYWORD
            )
        }

        val normalizedMerchant = normalizeText(merchant)
        val normalizedText = normalizeText(text)

        // ۳. بررسی تطبیق دقیق یا کلیدواژه‌ای در نام پذیرنده (اولویت بالا)
        if (normalizedMerchant.isNotBlank()) {
            for ((category, keywords) in CATEGORY_KEYWORD_RULES) {
                for (kw in keywords) {
                    val normalizedKw = normalizeText(kw)
                    if (normalizedMerchant.contains(normalizedKw)) {
                        return CategoryMatchResult(
                            category = category,
                            canonicalKisehCategory = toCanonicalKisehCategory(category),
                            confidence = 0.92f,
                            matchedKeyword = kw,
                            source = MatchSource.MERCHANT_KEYWORD
                        )
                    }
                }
            }
        }

        // ۴. بررسی تطبیق در کل متن پیامک
        for ((category, keywords) in CATEGORY_KEYWORD_RULES) {
            for (kw in keywords) {
                val normalizedKw = normalizeText(kw)
                if (normalizedText.contains(normalizedKw)) {
                    return CategoryMatchResult(
                        category = category,
                        canonicalKisehCategory = toCanonicalKisehCategory(category),
                        confidence = 0.82f,
                        matchedKeyword = kw,
                        source = MatchSource.BODY_KEYWORD
                    )
                }
            }
        }

        // ۵. حالت پیش‌فرض در صورت عدم تطبیق
        return CategoryMatchResult(
            category = CAT_OTHER,
            canonicalKisehCategory = KISEH_OTHER,
            confidence = 0.50f,
            matchedKeyword = null,
            source = MatchSource.FALLBACK_DEFAULT
        )
    }

    /**
     * حالت پیشرفته تشخیص دسته‌بندی با هوش مصنوعی که منحصراً برای مشترکین VIP فعال است.
     * برای کاربران نسخه رایگان، هوش پیشرفته اجرا نشده و حالت پیش‌فرض برگردانده می‌شود.
     */
    fun matchCategoryAdvanced(
        context: Context,
        text: String,
        merchant: String = "",
        isIncome: Boolean = false,
        userLearnedCategory: String? = null
    ): CategoryMatchResult {
        if (!com.example.billing.BillingManager.isProUser(context)) {
            return CategoryMatchResult(
                category = if (isIncome) CAT_INCOME else CAT_OTHER,
                canonicalKisehCategory = if (isIncome) KISEH_INCOME else KISEH_OTHER,
                confidence = 0.50f,
                matchedKeyword = null,
                source = MatchSource.FALLBACK_DEFAULT
            )
        }
        return matchCategory(text, merchant, isIncome, userLearnedCategory)
    }

    /**
     * تبدیل دسته‌بندی عمومی به دسته‌بندی استاندارد برنامه «کیسه»
     */
    fun toCanonicalKisehCategory(category: String): String {
        return when (category.trim()) {
            CAT_FOOD, "خوراکی", "غذا", "خواروبار", "سوپرمارکت" -> KISEH_FOOD
            CAT_RESTAURANT, "رستوران", "کافه", "فست فود" -> KISEH_FOOD
            CAT_TRANSPORT, "حمل‌ونقل", "حمل ونقل", "تاکسی", "بنزین" -> KISEH_TRANSPORT
            CAT_HEALTH, "درمان", "داروخانه", "پزشکی", "سلامت" -> KISEH_HEALTH
            CAT_CLOTHING, "لباس", "پوشاک و مد" -> KISEH_CLOTHING
            CAT_BILLS, "قبض", "قبوض", "ارتباطات", "اینترنت", "شارژ" -> KISEH_BILLS
            CAT_ENTERTAINMENT, "تفریح", "گردش", "سرگرمی" -> KISEH_ENTERTAINMENT
            CAT_HOUSING, "مسکن", "خانه", "اجاره", "املاک" -> KISEH_HOUSING
            CAT_INCOME, "درآمد", "حقوق", "دستمزد" -> KISEH_INCOME
            CAT_EDUCATION, CAT_ONLINE_SERVICES, CAT_OTHER, "متفرقه", "سایر" -> KISEH_OTHER
            else -> category // در صورتی که دسته‌بندی سفارشی ساخته شده توسط کاربر باشد
        }
    }

    /**
     * نرمال‌سازی عمیق کاراکترهای فارسی و عربی، حذف نیم‌فاصله و یکنواخت‌سازی فاصله‌ها
     */
    fun normalizeText(input: String): String {
        if (input.isBlank()) return ""
        return input
            .replace('\u200c', ' ') // نیم‌فاصله
            .replace('\u00a0', ' ') // non-breaking space
            .replace('ي', 'ی')     // ی عربی
            .replace('ى', 'ی')     // ی ماقبل آخر
            .replace('ك', 'ک')     // ک عربی
            .replace('ة', 'ه')     // ت گرد
            .replace('آ', 'ا')
            .replace('أ', 'ا')
            .replace('إ', 'ا')
            .replace('ؤ', 'و')
            .replace('ئ', 'ی')
            .lowercase()
            .replace(Regex("[\\p{Punct}&&[^@]]"), " ") // حذف کاراکترهای نشانه گذاری
            .replace(Regex("\\s+"), " ")               // تجمیع فاصله‌های اضافی
            .trim()
    }
}

/**
 * سیستم یادگیری و ثبت ترجیحات دسته‌بندی کاربر (User Category Preference Learner)
 *
 * به طور کاملاً محلی و آفلاین ترجیحات اصلاح شده کاربر برای هر پذیرنده را ذخیره کرده
 * و در پیامک‌های بعدی به صورت خودکار با ضریب اطمینان ۱۰۰٪ اعمال می‌کند.
 */
class UserCategoryLearner(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    companion object {
        private const val PREFS_NAME = "kiseh_merchant_category_learning"

        @Volatile
        private var INSTANCE: UserCategoryLearner? = null

        fun getInstance(context: Context): UserCategoryLearner {
            val appCtx = context.applicationContext
            return INSTANCE?.takeIf { it.context == appCtx } ?: synchronized(this) {
                INSTANCE?.takeIf { it.context == appCtx } ?: UserCategoryLearner(appCtx).also { INSTANCE = it }
            }
        }

        fun resetInstanceForTesting() {
            INSTANCE = null
        }
    }

    /**
     * یادگیری و ذخیره دسته‌بندی انتخابی برای یک فروشگاه یا پذیرنده
     */
    fun learn(merchant: String, category: String) {
        val cleanKey = SmartCategoryMatcher.normalizeText(merchant)
        if (cleanKey.isNotBlank() && category.isNotBlank()) {
            prefs.edit().putString(cleanKey, category.trim()).commit()
        }
    }

    /**
     * ثبت یادگیری تنها در صورتی که کاربر اشتراک VIP داشته باشد.
     * برای کاربران نسخه رایگان false برمی‌گرداند و چیزی ذخیره نمی‌کند.
     */
    fun learnVipOnly(merchant: String, category: String): Boolean {
        if (!com.example.billing.BillingManager.isProUser(context)) {
            return false
        }
        learn(merchant, category)
        return true
    }

    /**
     * دریافت دسته‌بندی یاد گرفته شده برای پذیرنده یا متن
     */
    fun getLearnedCategory(merchantOrText: String): String? {
        val cleanKey = SmartCategoryMatcher.normalizeText(merchantOrText)
        if (cleanKey.isBlank()) return null

        // ۱. جستجوی دقیق کلید
        val exactMatch = prefs.getString(cleanKey, null)
        if (exactMatch != null) return exactMatch

        // ۲. اگر تطبیق دقیق نبود، بررسی اینکه آیا کلمه آموخته‌شده در متن موجود است
        val allLearned = prefs.all
        for ((learnedMerchant, cat) in allLearned) {
            if (cat is String && learnedMerchant.length >= 3 && cleanKey.contains(learnedMerchant)) {
                return cat
            }
        }

        return null
    }

    /**
     * لیست تمام نگاشت‌های آموخته‌شده
     */
    fun getAllLearned(): Map<String, String> {
        val result = mutableMapOf<String, String>()
        prefs.all.forEach { (k, v) ->
            if (v is String) {
                result[k] = v
            }
        }
        return result
    }

    /**
     * بارگذاری دسته‌ای نگاشت‌های پذیرندگان از نسخه پشتیبان
     */
    fun learnAll(mappings: Map<String, String>) {
        if (mappings.isEmpty()) return
        val editor = prefs.edit()
        mappings.forEach { (merchant, category) ->
            val cleanKey = SmartCategoryMatcher.normalizeText(merchant)
            if (cleanKey.isNotBlank() && category.isNotBlank()) {
                editor.putString(cleanKey, category.trim())
            }
        }
        editor.commit()
    }

    /**
     * حذف نگاشت ذخیره‌شده یک پذیرنده
     */
    fun forget(merchant: String) {
        val cleanKey = SmartCategoryMatcher.normalizeText(merchant)
        prefs.edit().remove(cleanKey).commit()
    }

    /**
     * پاکسازی کل داده‌های یادگیری
     */
    fun clear() {
        prefs.edit().clear().commit()
    }
}

/**
 * موتور یادگیری هوشمند پذیرندگان و فروشگاه‌ها
 * صرفاً برای مشترکین VIP فعال است و برای کاربران عادی هیچ یادگیری انجام نمی‌دهد.
 */
object LearningEngine {

    fun learn(context: Context, merchant: String, category: String): Boolean {
        if (!com.example.billing.BillingManager.isProUser(context)) {
            return false
        }
        UserCategoryLearner.getInstance(context).learn(merchant, category)
        return true
    }

    fun isVipOnly(): Boolean = true
}

