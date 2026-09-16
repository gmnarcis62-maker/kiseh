package com.example.sms

import java.text.NumberFormat
import java.util.Locale
import java.util.regex.Pattern

/**
 * موتور اصلی تحلیل و پردازش هوشمند پیامک‌های تمامی بانک‌ها و نئوبانک‌های ایران
 */
class BankSmsParser {

    private fun safeLogD(tag: String, msg: String) {
        try {
            android.util.Log.d(tag, msg)
        } catch (_: Throwable) {
            println("[$tag] $msg")
        }
    }

    fun parse(
        context: android.content.Context,
        smsText: String,
        sender: String = "",
        userLearnedCategory: String? = null
    ): ParsedBankSms? {
        return parse(smsText, sender, userLearnedCategory, context)
    }

    /**
     * تحلیل متن پیامک و تبدیل به شیء ساخت‌یافته [ParsedBankSms].
     */
    fun parse(
        smsText: String,
        sender: String = "",
        userLearnedCategory: String? = null,
        context: android.content.Context? = null
    ): ParsedBankSms? {
        if (smsText.isBlank()) return null

        val normalizedText = normalizeDigits(smsText).trim()

        // ۱. غربالگری اولیه: پیامک‌های رمز پویا، کد فعالسازی، ورود، امنیتی یا تبلیغاتی فوراً رد می‌شوند
        if (isNonTransactionSms(normalizedText)) {
            safeLogD("BANK_SMS_PARSER", "Filtered out non-transaction SMS: $normalizedText")
            return null
        }

        // ۲. پیام‌های صرفاً اعلام موجودی بدون وقوع هرگونه واریز یا برداشت رد می‌شوند
        if (isPureBalanceInquiry(normalizedText)) {
            safeLogD("BANK_SMS_PARSER", "Filtered out pure balance inquiry SMS: $normalizedText")
            return null
        }

        // ۳. بررسی وجود نشانه‌های یک تراکنش بانکی
        if (!hasTransactionSignature(normalizedText)) {
            safeLogD("BANK_SMS_PARSER", "Filtered out SMS missing transaction signature: $normalizedText")
            return null
        }

        // ۴. تشخیص هوشمند نوع بانک با ترکیب متن و عنوان فرستنده
        val bank = BankType.fromText(normalizedText, sender)
        safeLogD("BANK_SMS_DEBUG", "DETECTED_BANK=${bank.displayName}")

        // ۵. تشخیص دقیق نوع تراکنش (واریز یا برداشت)
        val isIncome = detectIsIncome(normalizedText)

        // ۶. استخراج مبلغ تراکنش و تبدیل آن به «تومان»
        val (amountInToman, isRialDetected) = extractAmountInToman(normalizedText, bank)
        if (amountInToman <= 0L) {
            safeLogD("BANK_SMS_DEBUG", "Failed to extract amount from SMS: $normalizedText")
            return null
        }
        safeLogD("BANK_SMS_DEBUG", "PARSED_AMOUNT=$amountInToman")

        // ۷. استخراج مانده حساب (در صورت وجود در متن پیامک)
        val balance = extractBalanceInToman(normalizedText, isRialDetected)

        // ۸. استخراج شماره کارت یا حساب (۴ رقم انتهایی)
        val cardNumber = extractCardNumber(normalizedText)

        // ۹. استخراج شرح یا پذیرنده
        val description = extractDescription(normalizedText, bank, isIncome)
        val extractedMerchant = description
            .substringAfter("خرید: ")
            .substringAfter("واریز: ")
            .substringAfter("قبض: ")
            .substringBefore(" (")

        // ۱۰. حدس دسته‌بندی هوشمند با موتور SmartCategoryMatcher
        val matchResult = SmartCategoryMatcher.matchCategory(
            text = normalizedText,
            merchant = extractedMerchant,
            isIncome = isIncome,
            userLearnedCategory = userLearnedCategory
        )
        val suggestedCategory = matchResult.category
        val confidence = matchResult.confidence

        safeLogD("BANK_SMS_PARSER", """
            SMS TEXT: $smsText
            Detected Bank: ${bank.displayName}
            Bank Type: ${bank.name}
            Extracted Amount: $amountInToman Toman
            Transaction Type: ${if (isIncome) "Income/Deposit" else "Expense/Withdrawal"}
            Merchant: $extractedMerchant
            Parser Result: SUCCESS (Category: $suggestedCategory, Confidence: $confidence)
        """.trimIndent())

        return ParsedBankSms(
            bank = bank,
            amount = amountInToman,
            isIncome = isIncome,
            balance = balance,
            cardNumber = cardNumber,
            description = description,
            suggestedCategory = suggestedCategory,
            rawSms = smsText,
            timestamp = System.currentTimeMillis(),
            confidence = confidence
        )
    }

    /**
     * نرمال‌سازی ارقام فارسی (۰-۹) و عربی (٠-٩) به ارقام استاندارد انگلیسی (0-9)
     */
    fun normalizeDigits(input: String): String {
        val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
        var result = input
        for (i in 0..9) {
            result = result.replace(persianDigits[i], ('0' + i))
            result = result.replace(arabicDigits[i], ('0' + i))
        }
        return result
    }

    private fun isNonTransactionSms(text: String): Boolean {
        val lower = text.lowercase()
        return BankPatternRegistry.NON_TRANSACTION_KEYWORDS.any { lower.contains(it) }
    }

    /**
     * پیام‌هایی که صرفاً اعلام موجودی هستند و هیچ تراکنش مالی (برداشت، واریز، خرید یا انتقال) ندارند
     *
     * نکته اصلاح‌شده: اگر پیامک شامل مبلغی با علامت صریح + یا - باشد،
     * آن را تراکنش در نظر می‌گیریم نه صرفاً اعلام موجودی.
     */
    private fun isPureBalanceInquiry(text: String): Boolean {
        val lower = text.lowercase()
        val hasBalance = lower.contains("مانده") || lower.contains("موجودی") || lower.contains("موجودي")
        val hasTransaction = BankPatternRegistry.INCOME_KEYWORDS.any { lower.contains(it) } ||
                BankPatternRegistry.EXPENSE_KEYWORDS.any { lower.contains(it) } ||
                lower.contains("خرید") || lower.contains("برداشت") || lower.contains("واریز") ||
                lower.contains("انتقال") || lower.contains("قبض") || lower.contains("سود") || lower.contains("حقوق") ||
                // اگر پیامک شامل یک مبلغ با علامت صریح + یا - باشد، تراکنش است
                Regex("""[+\-]\s*[0-9][0-9,]*""").containsMatchIn(text)
        return hasBalance && !hasTransaction
    }

    private fun hasTransactionSignature(text: String): Boolean {
        val lower = text.lowercase()
        return BankPatternRegistry.BANK_TRANSACTION_KEYWORDS.any { lower.contains(it) }
    }

    /**
     * تشخیص نوع تراکنش (درآمد/واریز در برابر هزینه/برداشت)
     */
    private fun detectIsIncome(text: String): Boolean {
        val lower = text.lowercase()

        // بررسی صریح علامت مثبت یا منفی در مبالغ
        val hasExplicitPlus = text.contains("+ ") || text.contains("+")
        val hasExplicitMinus = text.contains("- ") || text.contains("-")

        // جستجوی موقعیت نخستین کلیدواژه درآمد و برداشت
        val incomeMatchIndex = BankPatternRegistry.INCOME_KEYWORDS
            .map { lower.indexOf(it) }
            .filter { it != -1 }
            .minOrNull() ?: Int.MAX_VALUE

        val expenseMatchIndex = BankPatternRegistry.EXPENSE_KEYWORDS
            .map { lower.indexOf(it) }
            .filter { it != -1 }
            .minOrNull() ?: Int.MAX_VALUE

        return when {
            incomeMatchIndex != Int.MAX_VALUE && expenseMatchIndex != Int.MAX_VALUE -> incomeMatchIndex < expenseMatchIndex
            incomeMatchIndex != Int.MAX_VALUE -> true
            expenseMatchIndex != Int.MAX_VALUE -> false
            hasExplicitPlus && !hasExplicitMinus -> true
            else -> false
        }
    }

    /**
     * استخراج مبلغ و تبدیل آن به «تومان» برای تمامی بانک‌ها و نئوبانک‌های کشور.
     *
     * نکته اصلاح‌شده: الگوی مبلغ با علامت (+/-) روی متن اصلی اعمال می‌شود، نه متن نرمال‌شده،
     * تا اعداد بعدی (مثل تاریخ 06/24) به انتهای مبلغ نچسبند.
     */
    private fun extractAmountInToman(text: String, bank: BankType): Pair<Long, Boolean> {
        // ۱. الگوی نئوبانکی با علامت مثبت یا منفی (اولویت بالا)
        //    مثال‌های منطبق: "-2,060,000"، "-2060000"، "+ 1,500,000"
        //    از متن اصلی استفاده می‌کنیم تا مرز مبلغ با کاراکترهای غیرعددی مشخص شود.
        val signPattern = Pattern.compile("""([+\-])\s*([0-9][0-9,]*[0-9]|[0-9])""")
        val signMatcher = signPattern.matcher(text)
        if (signMatcher.find()) {
            val sign = signMatcher.group(1)
            val rawNumberStr = signMatcher.group(2)?.replace(",", "")?.trim() ?: ""
            val rawValue: Long = rawNumberStr.toLongOrNull() ?: 0L

            if (rawValue > 0L) {
                val isRial = if (bank.defaultIsRial) rawValue >= 1000L else rawValue >= 1000000L
                val amountInToman = if (isRial) rawValue / 10L else rawValue
                val isIncome = sign == "+"
                return Pair(amountInToman, isRial)
            }
        }

        val patterns = listOf(
            // ۲. الگوی استاندارد بانکی با کلمات کلیدی مشخص
            Pattern.compile("""(?:مبلغ|واریز|برداشت|خرید|انتقال|کسر|بدهکار|بستانکار|حقوق|سود|قبض|پرداخت)[\s:]*[+\-]?\s*([0-9,]+)\s*(ریال|تومان|تومن)?"""),
            // ۳. الگوی عددی به همراه قید واحد پول (ریال یا تومان)
            Pattern.compile("""[+\-]\s*([0-9,]+)\s*(ریال|تومان|تومن)"""),
            // ۴. الگوی با کلمات کلیدی تراکنش بدون ذکر واحد
            Pattern.compile("""(?:مبلغ|واریز|برداشت|خرید)[\s:]*([0-9,]+)""")
        )

        for (pattern in patterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val rawGroup = matcher.group(1) ?: continue
                val rawNumberStr = rawGroup.replace(",", "").replace("+", "").replace("-", "").trim()
                val rawValue = rawNumberStr.toLongOrNull() ?: continue
                if (rawValue <= 0L) continue

                val unit = if (matcher.groupCount() >= 2) matcher.group(2) ?: "" else ""
                val isRial = when {
                    unit.contains("ریال") -> true
                    unit.contains("تومان") || unit.contains("تومن") -> false
                    bank.defaultIsRial -> rawValue >= 1000L
                    else -> rawValue >= 1000000L
                }

                val amountInToman = if (isRial) rawValue / 10L else rawValue
                return Pair(amountInToman, isRial)
            }
        }

        return Pair(0L, false)
    }

    /**
     * استخراج مانده حساب به تومان
     */
    private fun extractBalanceInToman(text: String, defaultIsRial: Boolean): Long? {
        val balancePattern = Pattern.compile("""(?:مانده|موجودی|موجودي|مانده نهایی|مانده حساب)[\s:]*([0-9,]+)\s*(ریال|تومان|تومن)?""")
        val matcher = balancePattern.matcher(text)
        if (matcher.find()) {
            val rawNumberStr = matcher.group(1)?.replace(",", "")?.trim() ?: return null
            val rawValue = rawNumberStr.toLongOrNull() ?: return null
            val unit = matcher.group(2) ?: ""
            val isRial = when {
                unit.contains("ریال") -> true
                unit.contains("تومان") || unit.contains("تومن") -> false
                else -> defaultIsRial
            }
            return if (isRial) rawValue / 10L else rawValue
        }
        return null
    }

    /**
     * استخراج ۴ رقم انتهای کارت یا حساب
     */
    private fun extractCardNumber(text: String): String? {
        val textWithoutDashes = text.replace("-", "")

        val fromPattern = Pattern.compile("""(?:از\s*(?:کارت|حساب|سپرده)?[\s:]*)(?:.*?[xX\*\.]{1,}([0-9]{4})\b|([0-9]{4,16})\b)""")
        val fromMatcher = fromPattern.matcher(textWithoutDashes)
        if (fromMatcher.find()) {
            val maskedLast4 = fromMatcher.group(1)
            if (maskedLast4 != null) return maskedLast4
            val fullDigits = fromMatcher.group(2)
            if (fullDigits != null) return if (fullDigits.length >= 4) fullDigits.takeLast(4) else fullDigits
        }

        val maskedPattern = Pattern.compile("""[\*xX\.]{1,}([0-9]{4})\b""")
        val maskedMatcher = maskedPattern.matcher(textWithoutDashes)
        if (maskedMatcher.find()) {
            return maskedMatcher.group(1)
        }

        val cardPattern = Pattern.compile("""(?:\*{2,}|کارت|حساب|سپرده|از|به)[\s:]*([0-9]{4,16})\b""")
        val matcher = cardPattern.matcher(textWithoutDashes)
        if (matcher.find()) {
            val digits = matcher.group(1) ?: return null
            return if (digits.length >= 4) digits.takeLast(4) else digits
        }

        return null
    }

    /**
     * استخراج شرح، نام پذیرنده یا نوع تراکنش
     */
    private fun extractDescription(text: String, bank: BankType, isIncome: Boolean): String {
        val descPattern = Pattern.compile("""(?:پذیرنده|فروشگاه|پایانه|شرح|خرید از|واریز از|بابت)[\s:]*([^\n\r,]+)""")
        val matcher = descPattern.matcher(text)
        val extracted = if (matcher.find()) {
            matcher.group(1)?.trim() ?: ""
        } else ""

        val isBill = text.contains("قبض")
        val isTransfer = text.contains("کارت به کارت") || text.contains("کارت‌به‌کارت") || text.contains("پایا") || text.contains("ساتنا")

        return when {
            extracted.isNotBlank() -> {
                "${if (isIncome) "واریز:" else "خرید:"} $extracted (${bank.displayName})"
            }
            isBill -> {
                "پرداخت قبض (${bank.displayName})"
            }
            isTransfer -> {
                "${if (isIncome) "انتقال وجه به حساب" else "انتقال وجه از حساب"} (${bank.displayName})"
            }
            else -> {
                "${if (isIncome) "واریز به حساب" else "برداشت از حساب"} (${bank.displayName})"
            }
        }
    }

    /**
     * حدس هوشمند دسته‌بندی بر اساس کلمات کلیدی متن با استفاده از SmartCategoryMatcher
     */
    fun guessCategory(text: String, isIncome: Boolean, merchant: String = "", userLearnedCategory: String? = null): String {
        return SmartCategoryMatcher.matchCategory(
            text = text,
            merchant = merchant,
            isIncome = isIncome,
            userLearnedCategory = userLearnedCategory
        ).category
    }
}