package com.example.analyzer

import android.content.Context
import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.regex.Pattern
import kotlin.coroutines.resume

class ReceiptOCR(private val context: Context) {

    private val recognizer: TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun analyzeReceipt(bitmap: Bitmap): ReceiptResult {
        val image = InputImage.fromBitmap(bitmap, 0)

        return try {
            val fullText = processImageAsync(image)

            // Normalize Persian digits to English digits
            val normalizedText = normalizeDigits(fullText)

            // Extract amounts
            val amounts = extractAmounts(normalizedText)
            val total = amounts.maxOrNull() ?: 0L

            // Guess category from original & normalized text
            val category = guessCategory(fullText + "\n" + normalizedText)

            ReceiptResult(
                totalAmount = total,
                items = amounts,
                category = category,
                rawText = fullText
            )
        } catch (e: Exception) {
            ReceiptResult(
                totalAmount = 0L,
                items = emptyList(),
                category = "متفرقه",
                rawText = ""
            )
        }
    }

    private suspend fun processImageAsync(image: InputImage): String =
        suspendCancellableCoroutine { continuation ->
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    if (continuation.isActive) {
                        continuation.resume(visionText.text)
                    }
                }
                .addOnFailureListener { e ->
                    if (continuation.isActive) {
                        continuation.resume("")
                    }
                }
        }

    private fun normalizeDigits(text: String): String {
        val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
        var result = text
        for (i in 0..9) {
            result = result.replace(persianDigits[i], ('0' + i))
                .replace(arabicDigits[i], ('0' + i))
        }
        return result
    }

    private fun extractAmounts(text: String): List<Long> {
        val amounts = mutableListOf<Long>()
        
        // ۱. بررسی سطر به سطر برای پیدا کردن سطرهای کلیدی مانند «مبلغ کل»، «جمع»، «قابل پرداخت»
        val lines = text.split("\n")
        var priorityAmount: Long? = null

        val priorityKeywords = listOf("مبلغ کل", "جمع کل", "قابل پرداخت", "پوز", "مبلغ", "جمع", "total", "amount", "pay")

        for (line in lines) {
            val lowerLine = line.lowercase()
            if (priorityKeywords.any { lowerLine.contains(it) }) {
                val lineNumbers = extractRawNumbersFromText(line)
                if (lineNumbers.isNotEmpty()) {
                    val maxLineNum = lineNumbers.maxOrNull() ?: 0L
                    if (maxLineNum > 100) {
                        priorityAmount = adjustRialToTomanIfNeeded(maxLineNum, text)
                        break
                    }
                }
            }
        }

        // ۲. استخراج عمومی تمام اعداد موجود در متن فاکتور
        val allNumbers = extractRawNumbersFromText(text)
        for (rawAmount in allNumbers) {
            if (rawAmount >= 100) {
                val adjusted = adjustRialToTomanIfNeeded(rawAmount, text)
                if (adjusted > 0 && !amounts.contains(adjusted)) {
                    amounts.add(adjusted)
                }
            }
        }

        if (priorityAmount != null && priorityAmount > 0) {
            amounts.remove(priorityAmount)
            amounts.add(0, priorityAmount)
        }

        return amounts.sortedDescending()
    }

    private fun extractRawNumbersFromText(text: String): List<Long> {
        val list = mutableListOf<Long>()
        // الگوی اعداد با جداکننده کاما، نقطه، اسلش یا بدون جداکننده
        val pattern = Pattern.compile("""\b\d{1,3}(?:[,,٫./\s]\d{3})+\b|\b\d{3,9}\b""")
        val matcher = pattern.matcher(text)

        while (matcher.find()) {
            val rawStr = matcher.group()!!
            val cleanStr = rawStr.replace(",", "").replace(".", "").replace("/", "").replace("٫", "").replace(" ", "").trim()
            val amount = cleanStr.toLongOrNull()
            if (amount != null && amount >= 100) {
                list.add(amount)
            }
        }
        return list
    }

    private fun adjustRialToTomanIfNeeded(amount: Long, fullText: String): Long {
        val lowerText = fullText.lowercase()
        val isExplicitRial = lowerText.contains("ریال") || lowerText.contains("rial")
        
        // اگر کلمه «ریال» صریحاً در فاکتور آمده یا مبلغ بیشتر از ۵۰۰ هزار است و به صفر ختم می‌شود، تبدیل ریال به تومان
        return if (isExplicitRial) {
            if (amount >= 10) amount / 10L else amount
        } else if (amount >= 1_000_000L && amount % 10L == 0L) {
            amount / 10L
        } else {
            amount
        }
    }

    private fun guessCategory(text: String): String {
        return when {
            text.contains(Regex("نان|شیر|گوشت|برنج|روغن|قند|چای|میوه|سبزی|لبنیات|سوپرمارکت|هایپر|فروشگاه")) -> "خوراکی"
            text.contains(Regex("بنزین|گازوئیل|روغن موتور|اسنپ|تپسی|تاکسی|کرایه|پارکینگ")) -> "حمل‌و‌نقل"
            text.contains(Regex("دکتر|دارو|بیمارستان|درمانگاه|مطب|داروخانه|کلینیک")) -> "درمان"
            text.contains(Regex("برق|آب|گاز|تلفن|اینترنت|قبض|همراه اول|ایرانسل")) -> "قبض"
            text.contains(Regex("پیراهن|شلوار|کفش|لباس|کیف|پوشاک|بوتیک")) -> "پوشاک"
            text.contains(Regex("رستوران|فست فود|پیتزا|کافه|سینما|تفریح")) -> "تفریح"
            text.contains(Regex("اجاره|مسکن|املاک")) -> "اجاره"
            else -> "متفرقه"
        }
    }

    fun close() {
        try {
            recognizer.close()
        } catch (_: Exception) {}
    }
}

data class ReceiptResult(
    val totalAmount: Long,
    val items: List<Long>,
    val category: String,
    val rawText: String
)
