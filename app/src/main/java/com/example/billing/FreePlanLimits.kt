package com.example.billing

/**
 * منبع مرکزی تعاریف و سقف محدودیت‌های نسخه رایگان (Free Plan Limits).
 * تمام UIها، پیام‌ها و بخش‌های محاسباتی برنامه از این منبع مرکزی استفاده می‌کنند.
 */
object FreePlanLimits {
    const val VOICE_DAILY_LIMIT = 5
    const val BANK_SMS_TRIAL_LIMIT = 3
    const val INVOICE_SCAN_LIMIT = 3
    const val MAX_SAVINGS_GOALS = 1
    const val MAX_RECURRING_TRANSACTIONS = 1
    const val MAX_CUSTOM_CATEGORIES = 2

    // عناوین و توضیحات فارسی صریح جهت نمایش یکپارچه در UIها
    const val TITLE_FREE_PLAN = "شما در حال استفاده از نسخه رایگان کیسه هستید"
    const val DESC_FREE_PLAN = "تمام قابلیت‌های برنامه برای آشنایی شما فعال هستند، اما دارای محدودیت استفاده می‌باشند.\nبرای استفاده نامحدود از تمام امکانات، نسخه VIP را فعال کنید."
    
    val LIMIT_VOICE_LABEL = "ثبت صوتی: $VOICE_DAILY_LIMIT ثبت در روز"
    val LIMIT_BANK_SMS_LABEL = "هوش پیامکی بانکی: $BANK_SMS_TRIAL_LIMIT تراکنش آزمایشی"
    val LIMIT_INVOICE_LABEL = "اسکن و تحلیل فاکتور: $INVOICE_SCAN_LIMIT استفاده رایگان"
    val LIMIT_SAVINGS_LABEL = "اهداف پس‌انداز: $MAX_SAVINGS_GOALS هدف فعال"
    val LIMIT_RECURRING_LABEL = "تراکنش‌های دوره‌ای: $MAX_RECURRING_TRANSACTIONS مورد فعال"
    const val LIMIT_BACKUP_LABEL = "پشتیبان‌گیری: محدود"
}
