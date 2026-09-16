package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.sms.BankSmsParser
import com.example.sms.MatchSource
import com.example.sms.SmartCategoryMatcher
import com.example.sms.UserCategoryLearner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * آزمون‌های اعتبارسنجی موتور هوشمند دسته‌بندی و یادگیری ترجیحات مالی کاربر (مرحله ۷.۵)
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SmartCategoryMatcherTest {

    private lateinit var context: Context
    private lateinit var learner: UserCategoryLearner
    private lateinit var parser: BankSmsParser

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        learner = UserCategoryLearner(context)
        learner.clear()
        parser = BankSmsParser()
    }

    // =========================================================================
    // ۱. تست نرمال‌سازی عمیق کاراکترهای فارسی و عربی و حذف نیم‌فاصله‌ها
    // =========================================================================
    @Test
    fun testTextNormalization() {
        val rawInput = "فروشگاه\u200Cهاي زنجيره‌اي   افق كوروش: خريد!"
        val normalized = SmartCategoryMatcher.normalizeText(rawInput)
        // ی عربی به فارسی، ک عربی به فارسی، نیم‌فاصله به فاصله، علائم نگارشی پاکسازی
        assertTrue(normalized.contains("کوروش"))
        assertTrue(normalized.contains("فروشگاه"))
        assertFalse(normalized.contains("كوروش")) // ک و ی عربی نباید باقی بماند
        assertFalse(normalized.contains("\u200c"))
    }

    // =========================================================================
    // ۲. تست تشخیص دقیق کلمات کلیدی دسته‌های مختلف مالی
    // =========================================================================
    @Test
    fun testCategoryMatching_foodAndGroceries() {
        val result1 = SmartCategoryMatcher.matchCategory(
            text = "خرید از فروشگاه افق کوروش مبلغ 120000 تومان",
            merchant = "افق کوروش"
        )
        assertEquals(SmartCategoryMatcher.CAT_FOOD, result1.category)
        assertEquals(SmartCategoryMatcher.KISEH_FOOD, result1.canonicalKisehCategory)
        assertTrue(result1.confidence >= 0.8f)

        val result2 = SmartCategoryMatcher.matchCategory(
            text = "برداشت نانوایی بربری سنگک",
            merchant = "نانوایی بربری"
        )
        assertEquals(SmartCategoryMatcher.CAT_FOOD, result2.category)
        assertEquals(SmartCategoryMatcher.KISEH_FOOD, result2.canonicalKisehCategory)
    }

    @Test
    fun testCategoryMatching_restaurantsAndCafes() {
        val result1 = SmartCategoryMatcher.matchCategory(
            text = "خرید فست فود عطاویچ",
            merchant = "فست فود عطاویچ"
        )
        assertEquals(SmartCategoryMatcher.CAT_RESTAURANT, result1.category)
        assertEquals(SmartCategoryMatcher.KISEH_FOOD, result1.canonicalKisehCategory)

        val result2 = SmartCategoryMatcher.matchCategory(
            text = "خرید از کافه لاته طباخی",
            merchant = "کافه لاته"
        )
        assertEquals(SmartCategoryMatcher.CAT_RESTAURANT, result2.category)
        assertEquals(SmartCategoryMatcher.KISEH_FOOD, result2.canonicalKisehCategory)
    }

    @Test
    fun testCategoryMatching_transportation() {
        val result1 = SmartCategoryMatcher.matchCategory(
            text = "خرید اسنپ سفر درون شهری",
            merchant = "اسنپ"
        )
        assertEquals(SmartCategoryMatcher.CAT_TRANSPORT, result1.category)
        assertEquals(SmartCategoryMatcher.KISEH_TRANSPORT, result1.canonicalKisehCategory)

        val result2 = SmartCategoryMatcher.matchCategory(
            text = "برداشت جایگاه سوخت بنزین آزادی",
            merchant = "جایگاه سوخت 245"
        )
        assertEquals(SmartCategoryMatcher.CAT_TRANSPORT, result2.category)
        assertEquals(SmartCategoryMatcher.KISEH_TRANSPORT, result2.canonicalKisehCategory)
    }

    @Test
    fun testCategoryMatching_healthcare() {
        val result1 = SmartCategoryMatcher.matchCategory(
            text = "خرید از داروخانه شبانه روزی دکتر راد",
            merchant = "داروخانه دکتر راد"
        )
        assertEquals(SmartCategoryMatcher.CAT_HEALTH, result1.category)
        assertEquals(SmartCategoryMatcher.KISEH_HEALTH, result1.canonicalKisehCategory)

        val result2 = SmartCategoryMatcher.matchCategory(
            text = "پذیرنده کلینیک دندانپزشکی پارس",
            merchant = "کلینیک دندانپزشکی"
        )
        assertEquals(SmartCategoryMatcher.CAT_HEALTH, result2.category)
        assertEquals(SmartCategoryMatcher.KISEH_HEALTH, result2.canonicalKisehCategory)
    }

    @Test
    fun testCategoryMatching_billsAndUtilities() {
        val result = SmartCategoryMatcher.matchCategory(
            text = "برداشت قبض همراه اول بسته اینترنت",
            merchant = "همراه اول"
        )
        assertEquals(SmartCategoryMatcher.CAT_BILLS, result.category)
        assertEquals(SmartCategoryMatcher.KISEH_BILLS, result.canonicalKisehCategory)
    }

    @Test
    fun testCategoryMatching_clothing() {
        val result = SmartCategoryMatcher.matchCategory(
            text = "خرید پوشاک هاکوپیان کت و شلوار",
            merchant = "بوتیک هاکوپیان"
        )
        assertEquals(SmartCategoryMatcher.CAT_CLOTHING, result.category)
        assertEquals(SmartCategoryMatcher.KISEH_CLOTHING, result.canonicalKisehCategory)
    }

    @Test
    fun testCategoryMatching_entertainment() {
        val result = SmartCategoryMatcher.matchCategory(
            text = "خرید بلیط پردیس سینمایی کوروش",
            merchant = "سینما کوروش"
        )
        assertEquals(SmartCategoryMatcher.CAT_ENTERTAINMENT, result.category)
        assertEquals(SmartCategoryMatcher.KISEH_ENTERTAINMENT, result.canonicalKisehCategory)
    }

    @Test
    fun testCategoryMatching_income() {
        val result = SmartCategoryMatcher.matchCategory(
            text = "واریز حقوق ماهانه به حساب شما",
            isIncome = true
        )
        assertEquals(SmartCategoryMatcher.CAT_INCOME, result.category)
        assertEquals(SmartCategoryMatcher.KISEH_INCOME, result.canonicalKisehCategory)
        assertTrue(result.confidence >= 0.85f)
    }

    // =========================================================================
    // ۳. تست یادگیری هوشمند ترجیحات کاربر (User Category Learning)
    // =========================================================================
    @Test
    fun testUserCategoryLearning_overridesDefaultKeyword() {
        val unknownMerchant = "فروشگاه ستاره کویر"

        // قبل از یادگیری، چون کلمه کلیدی خاصی ندارد، دسته "سایر" حدس زده می‌شود
        val defaultResult = SmartCategoryMatcher.matchCategory(
            text = "خرید از فروشگاه ستاره کویر مبلغ 200,000 تومان",
            merchant = unknownMerchant
        )
        // ممکن است فروشگاه در خوراک بیفتد، اما کاربر می‌خواهد پوشاک باشد
        learner.learn(unknownMerchant, "پوشاک")

        // بررسی ذخیره‌سازی در موتور یادگیری
        val learnedCat = learner.getLearnedCategory(unknownMerchant)
        assertEquals("پوشاک", learnedCat)

        // تطبیق مجدد با دسته‌بندی یاد گرفته شده
        val smartResult = SmartCategoryMatcher.matchCategory(
            text = "خرید از فروشگاه ستاره کویر مبلغ 350,000 تومان",
            merchant = unknownMerchant,
            userLearnedCategory = learnedCat
        )

        assertEquals("پوشاک", smartResult.category)
        assertEquals(SmartCategoryMatcher.KISEH_CLOTHING, smartResult.canonicalKisehCategory)
        assertEquals(1.0f, smartResult.confidence)
        assertTrue(smartResult.isUserLearned)
        assertEquals(MatchSource.USER_LEARNED, smartResult.source)
    }

    // =========================================================================
    // ۴. تست تبدیل به دسته‌بندی‌های استاندارد برنامه «کیسه»
    // =========================================================================
    @Test
    fun testToCanonicalKisehCategory() {
        assertEquals("خوراکی", SmartCategoryMatcher.toCanonicalKisehCategory("خوراک"))
        assertEquals("خوراکی", SmartCategoryMatcher.toCanonicalKisehCategory("رستوران و کافه"))
        assertEquals("حمل‌ونقل", SmartCategoryMatcher.toCanonicalKisehCategory("حمل و نقل"))
        assertEquals("درمان", SmartCategoryMatcher.toCanonicalKisehCategory("پزشکی و سلامت"))
        assertEquals("قبض", SmartCategoryMatcher.toCanonicalKisehCategory("قبوض و ارتباطات"))
        assertEquals("پوشاک", SmartCategoryMatcher.toCanonicalKisehCategory("پوشاک"))
        assertEquals("تفریح", SmartCategoryMatcher.toCanonicalKisehCategory("تفریح و سرگرمی"))
        assertEquals("مسکن", SmartCategoryMatcher.toCanonicalKisehCategory("مسکن و خانه"))
        assertEquals("درآمد", SmartCategoryMatcher.toCanonicalKisehCategory("درآمد و حقوق"))
        assertEquals("متفرقه", SmartCategoryMatcher.toCanonicalKisehCategory("سایر"))
        // دسته‌بندی دلخواه کاربر بدون دستکاری عبور داده می‌شود
        assertEquals("قسط وام", SmartCategoryMatcher.toCanonicalKisehCategory("قسط وام"))
    }

    // =========================================================================
    // ۵. تست رگرسیون BankSmsParser با موتور جدید
    // =========================================================================
    @Test
    fun testBankSmsParserRegressionWithSmartMatcher() {
        val melliSms = """
            بانک ملی ایران
            برداشت مبلغ 1,500,000 ریال
            از حساب: *1234
            مانده: 12,000,000 ریال
            پذیرنده: فروشگاه افق کوروش
        """.trimIndent()

        val parsed = parser.parse(melliSms)
        assertNotNull(parsed)
        assertEquals("خوراک", parsed!!.suggestedCategory)

        // تست یادگیری در سطح Parser
        learner.learn("فروشگاه افق کوروش", "سوپرمارکت خانوادگی")
        val learnedCat = learner.getLearnedCategory("فروشگاه افق کوروش")
        val parsedWithLearning = parser.parse(melliSms, userLearnedCategory = learnedCat)
        assertNotNull(parsedWithLearning)
        assertEquals("سوپرمارکت خانوادگی", parsedWithLearning!!.suggestedCategory)
    }
}
