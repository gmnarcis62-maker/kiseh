package com.example.analyzer

import java.util.regex.Pattern

data class AnalysisResult(
    val amount: Long,
    val category: String,
    val description: String,
    val isIncome: Boolean = false
)

class ExpenseAnalyzer {

    // نقشه کلیدواژه‌ها به دسته‌بندی‌های فارسی
    private val categoryRules = listOf(
        // خوراکی
        CategoryRule(
            category = "خوراکی",
            keywords = listOf("نان", "نون", "شیر", "گوشت", "مرغ", "برنج", "خوراک", "میوه", "سبزی", "قند", "چای", "خرما", "پنیر", "سوپرمارکت", "هایپر", "ناهار", "شام", "صبحانه", "بستنی", "قهوه", "کافه", "پیتزا", "ساندویچ", "رستوران", "غذا", "کباب", "آبمیوه", "تنقلات", "شیرینی", "کیک", "شکلات")
        ),
        // حمل‌ونقل
        CategoryRule(
            category = "حمل‌ونقل",
            keywords = listOf("بنزین", "تاکسی", "اتوبوس", "مترو", "ماشین", "پارکینگ", "اسنپ", "تپسی", "کرایه", "گاز", "سرویس", "مکانیکی", "روغن", "تایر", "لاستیک", "عوارضی", "جریمه", "کارواش")
        ),
        // درمان
        CategoryRule(
            category = "درمان",
            keywords = listOf("دکتر", "دارو", "بیمار", "درمان", "قرص", "آزمایش", "ویزیت", "دندانپزشک", "بیمارستان", "درمانگاه", "مکمل", "تزریقات", "داروخانه", "سرم", "فیزیوتراپی", "عینک")
        ),
        // قبض و خدمات
        CategoryRule(
            category = "قبض",
            keywords = listOf("برق", "آب", "گاز", "تلفن", "اینترنت", "شارژ", "همراه اول", "ایرانسل", "رایتل", "قبض", "بسته اینترنت", "وای‌فای", "مودم")
        ),
        // تفریح و گردش
        CategoryRule(
            category = "تفریح",
            keywords = listOf("سینما", "بازی", "استخر", "سفر", "هتل", "بلیط", "کتاب", "ورزش", "باشگاه", "تئاتر", "شهربازی", "کنسرت", "تور", "تفریح")
        ),
        // پوشاک
        CategoryRule(
            category = "پوشاک",
            keywords = listOf("لباس", "کفش", "شلوار", "پیراهن", "کیف", "مانتو", "کاپشن", "جوراب", "خیاطی", "پوشاک", "کت", "کتونی", "شال", "روسری")
        ),
        // مسکن و خانه
        CategoryRule(
            category = "مسکن",
            keywords = listOf("اجاره", "رهن", "مستاجر", "شارژ ساختمان", "تعمیرات خانه", "لوازم خانگی", "مبل", "فرش", "خانه", "آپارتمان")
        ),
        // درآمد
        CategoryRule(
            category = "درآمد",
            keywords = listOf("حقوق", "دستمزد", "واریزی", "پاداش", "سود", "فروش", "درآمد", "عیدی", "کرایه دریافتی"),
            isIncome = true
        )
    )

    fun analyze(input: String): AnalysisResult {
        val normalized = normalizePersianDigits(input.trim())
        
        // ۱. استخراج مبلغ
        val amount = extractAmount(normalized)
        
        // ۲. تشخیص دسته‌بندی و نوع (درآمد/هزینه)
        val (category, isIncome) = detectCategoryAndType(normalized)
        
        // ۳. خلاصه‌سازی و پاکسازی متن توضیح
        val description = cleanDescription(input, amount)

        return AnalysisResult(
            amount = amount,
            category = category,
            description = description,
            isIncome = isIncome
        )
    }

    private fun normalizePersianDigits(text: String): String {
        var result = text
        val persianDigits = arrayOf("۰", "۱", "۲", "۳", "۴", "۵", "۶", "۷", "۸", "۹")
        val arabicDigits = arrayOf("٠", "١", "٢", "٣", "٤", "٥", "٦", "٧", "٨", "٩")
        
        for (i in 0..9) {
            result = result.replace(persianDigits[i], i.toString())
                .replace(arabicDigits[i], i.toString())
        }
        return result
    }

    private fun extractAmount(text: String): Long {
        // ۱. بررسی الگوهای اعشاری: "2.5 میلیون" یا "20.5 هزار"
        val millionDecimalMatch = Pattern.compile("""(\d+(?:[.,/]\d+)?)\s*(میلیون|M|m)""", Pattern.CASE_INSENSITIVE).matcher(text)
        if (millionDecimalMatch.find()) {
            val numStr = millionDecimalMatch.group(1)!!.replace(",", ".").replace("/", ".")
            val valDouble = numStr.toDoubleOrNull() ?: 0.0
            if (valDouble > 0) return (valDouble * 1_000_000).toLong()
        }

        val thousandDecimalMatch = Pattern.compile("""(\d+(?:[.,/]\d+)?)\s*(هزار|K|k)""", Pattern.CASE_INSENSITIVE).matcher(text)
        if (thousandDecimalMatch.find()) {
            val numStr = thousandDecimalMatch.group(1)!!.replace(",", ".").replace("/", ".")
            val valDouble = numStr.toDoubleOrNull() ?: 0.0
            if (valDouble > 0) return (valDouble * 1_000).toLong()
        }

        // ۲. بررسی اعداد حروف فارسی (مانند: بیست هزار، پنجاه تومن، سی و پنج هزار)
        val wordAmount = parsePersianWordsToNumber(text)
        if (wordAmount > 0) {
            return wordAmount
        }

        // ۳. استخراج تمام اعداد موجود در متن
        val numberMatcher = Pattern.compile("""\d[\d,.]*""").matcher(text)
        val foundNumbers = mutableListOf<Long>()
        while (numberMatcher.find()) {
            val cleanNumStr = numberMatcher.group()!!.replace(",", "").replace(".", "")
            val num = cleanNumStr.toLongOrNull()
            if (num != null && num > 0) {
                foundNumbers.add(num)
            }
        }

        if (foundNumbers.isNotEmpty()) {
            // ترجیحاً بزرگترین عدد یا عدد متناسب انتخاب شود
            val rawValue = foundNumbers.maxOrNull() ?: 0L
            if (rawValue > 0) {
                return when {
                    text.contains("میلیون") -> rawValue * 1_000_000L
                    text.contains("هزار") -> rawValue * 1_000L
                    text.contains("ریال") -> if (rawValue >= 10) rawValue / 10L else rawValue
                    // در گفتار فارسی اگر عدد کمتر از ۱۰۰۰ باشد (مثل ۲۰ نون، ۵0 بنزین، ۱5۰ خرید) منظور ۲۰ هزار تومان است
                    rawValue in 1..999 -> rawValue * 1_000L
                    else -> rawValue
                }
            }
        }

        return 0L
    }

    private fun parsePersianWordsToNumber(text: String): Long {
        val wordMap = mapOf(
            "یک" to 1L, "دو" to 2L, "سه" to 3L, "چهار" to 4L, "پنج" to 5L,
            "شش" to 6L, "شەش" to 6L, "هفت" to 7L, "هشت" to 8L, "نه" to 9L, "ده" to 10L,
            "یازده" to 11L, "دوازده" to 12L, "سیزده" to 13L, "چهارده" to 14L, "پانزده" to 15L,
            "شانزده" to 16L, "هفده" to 17L, "هیجده" to 18L, "هجده" to 18L, "نوزده" to 19L,
            "بیست" to 20L, "سی" to 30L, "چهل" to 40L, "پنجاه" to 50L, "شصت" to 60L,
            "هفتاد" to 70L, "هشتاد" to 80L, "نود" to 90L,
            "صد" to 100L, "دویست" to 200L, "سیصد" to 300L, "چهارصد" to 400L, "پانصد" to 500L,
            "ششصد" to 600L, "هفتصد" to 700L, "هشتصد" to 800L, "نهصد" to 900L
        )

        var total = 0L
        var currentSum = 0L

        val words = text.split(Regex("""\s+"""))
        for (w in words) {
            val cleanW = w.replace("و", "").trim()
            if (cleanW in wordMap) {
                currentSum += wordMap[cleanW]!!
            } else if (cleanW == "هزار") {
                if (currentSum == 0L) currentSum = 1L
                total += currentSum * 1000L
                currentSum = 0L
            } else if (cleanW == "میلیون") {
                if (currentSum == 0L) currentSum = 1L
                total += currentSum * 1_000_000L
                currentSum = 0L
            }
        }
        total += currentSum

        if (total in 1..999 && (text.contains("تومن") || text.contains("تومان"))) {
            total *= 1000L
        }

        return total
    }

    private fun detectCategoryAndType(text: String): Pair<String, Boolean> {
        val lowerText = text.lowercase()
        for (rule in categoryRules) {
            for (kw in rule.keywords) {
                if (lowerText.contains(kw)) {
                    return Pair(rule.category, rule.isIncome)
                }
            }
        }
        return Pair("متفرقه", false)
    }

    private fun cleanDescription(text: String, extractedAmount: Long): String {
        // حذف کلمات پولی، اعداد و فعل‌های متداول پرداخت و کلمات رابط
        var cleaned = text
            .replace(Regex("""\d+"""), "")
            .replace(Regex("""[۰-۹]+"""), "")
            .replace(Regex("""[٠-٩]+"""), "")
            .replace(Regex("تومن|تومان|هزار|میلیون|ریال|ت|Toman|toman", RegexOption.IGNORE_CASE), "")
            .replace(Regex("خرید کردم|پرداخت کردم|خرج کردم|خریدم|دادم|کردم|زدم|گرفتم|واریز شد|خرید|بابت|تا|عدد"), "")
            .trim()

        // حذف فاصله‌های اضافی
        cleaned = cleaned.replace(Regex("""\s+"""), " ").trim()

        if (cleaned.length > 35) {
            cleaned = cleaned.take(35) + "..."
        }

        return when {
            cleaned == "امروز" -> "خرید امروز"
            cleaned.isNotEmpty() -> cleaned
            else -> "ثبت صوتی"
        }
    }

    private data class CategoryRule(
        val category: String,
        val keywords: List<String>,
        val isIncome: Boolean = false
    )
}
