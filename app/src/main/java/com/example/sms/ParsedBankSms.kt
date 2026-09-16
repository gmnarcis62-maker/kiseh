package com.example.sms

/**
 * مدل داده استاندارد پیامک بانکی تحلیل‌شده
 *
 * @param bank نوع بانک شناسایی شده
 * @param amount مبلغ تراکنش به تومان (استاندارد نرمالایز شده برای ثبت در دیتابیس کیسه)
 * @param isIncome آیا واریز/درآمد است (true) یا برداشت/خرید/انتقال (false)
 * @param balance مانده حساب پس از تراکنش به تومان (در صورت درج در پیامک)
 * @param cardNumber ۴ رقم انتهای کارت یا شماره حساب
 * @param description شرح تراکنش یا نام فروشگاه/پذیرنده
 * @param suggestedCategory دسته‌بندی پیشنهادی هوشمند بر اساس نام پذیرنده یا شرح
 * @param rawSms متن خام پیامک جهت شفافیت و بررسی
 * @param timestamp زمان وقوع یا ثبت پیامک
 */
data class ParsedBankSms(
    val bank: BankType,
    val amount: Long,
    val isIncome: Boolean,
    val balance: Long? = null,
    val cardNumber: String? = null,
    val description: String = "",
    val suggestedCategory: String = "سایر",
    val rawSms: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val confidence: Float = 0.85f
)
