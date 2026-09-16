package com.example.sms

/**
 * انواع بانک‌ها و نئوبانک‌های پشتیبانی‌شده در موتور پردازش هوشمند پیامک‌های بانکی کیسه
 *
 * شامل تمام بانک‌های دولتی، خصوصی، قرض‌الحسنه و نئوبانک‌های معتبر شبکه شتاب کشور.
 */
enum class BankType(
    val displayName: String,
    val code: String,
    val isNeobank: Boolean = false,
    val defaultIsRial: Boolean = true
) {
    // ۶ بانک اولیه شبکه بانکی
    MELLI("بانک ملی ایران", "melli", false, true),
    MELLAT("بانک ملت", "mellat", false, true),
    SAMAN("بانک سامان", "saman", false, false),
    PASARGAD("بانک پاسارگاد", "pasargad", false, true),
    TEJARAT("بانک تجارت", "tejarat", false, true),
    SADERAT("بانک صادرات ایران", "saderat", false, true),

    // ۲۱ بانک دولتی، خصوصی و قرض‌الحسنه
    SEPAH("بانک سپه", "sepah", false, true),
    KESHAVARZI("بانک کشاورزی", "keshavarzi", false, true),
    MASKAN("بانک مسکن", "maskan", false, true),
    AYANDEH("بانک آینده", "ayandeh", false, true),
    SHAHR("بانک شهر", "shahr", false, true),
    REFAH("بانک رفاه کارگران", "refah", false, true),
    PARSIAN("بانک پارسیان", "parsian", false, true),
    EGHTESAD_NOVIN("بانک اقتصاد نوین", "eghtesad_novin", false, true),
    SINA("بانک سینا", "sina", false, true),
    DEY("بانک دی", "dey", false, true),
    SARMAYEH("بانک سرمایه", "sarmayeh", false, true),
    KARAFARIN("بانک کارآفرین", "karafarin", false, true),
    GARDESHGARI("بانک گردشگری", "gardeshgari", false, true),
    IRANZAMIN("بانک ایران زمین", "iranzamin", false, true),
    MIDDLE_EAST("بانک خاورمیانه", "middle_east", false, true),
    MEHR_IRAN("بانک قرض‌الحسنه مهر ایران", "mehr_iran", false, true),
    RESALAT("بانک قرض‌الحسنه رسالت", "resalat", false, true),
    POST_BANK("پست بانک ایران", "post_bank", false, true),
    TOSE_TAAVON("بانک توسعه تعاون", "tose_taavon", false, true),
    SANAT_VA_MADAN("بانک صنعت و معدن", "sanat_va_madan", false, true),
    TOSE_SADERAT("بانک توسعه صادرات ایران", "tose_saderat", false, true),

    // ۴ نئوبانک مطرح کشور
    BLUBANK("بلوبانک", "blubank", true, false),
    WEPOD("ویپاد", "wepod", true, false),
    BAJET("باجت", "bajet", true, false),
    ABANK("آبانک", "abank", true, false),

    // وضعیت ناشناس یا متفرقه شتاب
    UNKNOWN("بانک ناشناس / شتاب", "unknown", false, true);

    companion object {
        /**
         * فهرست تمامی بانک‌های مشخص و معتبر فعال در سیستم (بدون UNKNOWN)
         */
        val supportedBanks: List<BankType>
            get() = values().filter { it != UNKNOWN }

        /**
         * تشخیص هوشمند بانک بر اساس ترکیب متن پیامک و عنوان یا سرشماره فرستنده
         */
        fun fromText(text: String, sender: String = ""): BankType {
            val lowerText = text.lowercase()
            val lowerSender = sender.lowercase()
            val combined = "$lowerSender $lowerText"

            // ۱. اولویت اول: نئوبانک‌ها (به دلیل میزبانی روی سوئیچ بانک‌های مادر مثل سامان، پاسارگاد و آینده)
            if (combined.contains("بلوبانک") || combined.contains("بلو بانک") || lowerSender.contains("blubank") || lowerSender == "blu") return BLUBANK
            if (combined.contains("ویپاد") || lowerSender.contains("wepod")) return WEPOD
            if (combined.contains("باجت") || lowerSender.contains("bajet")) return BAJET
            if (combined.contains("آبانک") || combined.contains("ابانك") || lowerSender.contains("abank")) return ABANK

            // ۲. بانک‌های با اسامی خاص، ترکیبی یا نیازمند دقت بالا
            if (combined.contains("توسعه صادرات") || lowerSender.contains("edbi")) return TOSE_SADERAT
            if (combined.contains("صادرات") || combined.contains("سپهر") || lowerSender.contains("saderat") || lowerSender.contains("bsi")) return SADERAT
            if (combined.contains("قرض‌الحسنه مهر") || combined.contains("قرض الحسنه مهر") || combined.contains("مهر ایران") || lowerSender.contains("qmb") || lowerSender.contains("mehriran")) return MEHR_IRAN
            if (combined.contains("قرض‌الحسنه رسالت") || combined.contains("قرض الحسنه رسالت") || combined.contains("رسالت") || lowerSender.contains("rqbank") || lowerSender.contains("resalat")) return RESALAT
            if (combined.contains("ایران زمین") || lowerSender.contains("iranzamin") || lowerSender.contains("izbank")) return IRANZAMIN
            if (combined.contains("توسعه تعاون") || lowerSender.contains("ttbank") || lowerSender.contains("toseetaavon")) return TOSE_TAAVON
            if (combined.contains("صنعت و معدن") || lowerSender.contains("bim") || lowerSender.contains("sanatomadan")) return SANAT_VA_MADAN
            if (combined.contains("اقتصاد نوین") || lowerSender.contains("enbank") || lowerSender.contains("eghtesad")) return EGHTESAD_NOVIN
            if (combined.contains("رفاه کارگران") || combined.contains("بانک رفاه") || lowerSender.contains("refah") || lowerSender.contains("bankrefah")) return REFAH
            if (combined.contains("خاورمیانه") || lowerSender.contains("mebank") || lowerSender.contains("middleeast")) return MIDDLE_EAST
            if (combined.contains("پست بانک") || lowerSender.contains("postbank")) return POST_BANK
            if (combined.contains("گردشگری") || lowerSender.contains("gardeshgari") || lowerSender.contains("tourismbank")) return GARDESHGARI
            if (combined.contains("کارآفرین") || combined.contains("کارافرین") || lowerSender.contains("karafarin")) return KARAFARIN
            if (combined.contains("سرمایه") || lowerSender.contains("sarmayeh") || lowerSender.contains("sbank")) return SARMAYEH
            if (combined.contains("کشاورزی") || lowerSender.contains("keshavarzi") || lowerSender.contains("bki") || lowerSender.contains("agribank")) return KESHAVARZI
            if (combined.contains("مسکن") || lowerSender.contains("maskan") || lowerSender.contains("bankmaskan")) return MASKAN
            if (combined.contains("آینده") || combined.contains("اينده") || lowerSender.contains("ayandeh") || lowerSender == "ba") return AYANDEH
            if (combined.contains("بانک شهر") || combined.contains("شهرنت") || lowerSender.contains("shahr") || lowerSender.contains("citybank") || lowerSender.contains("shahrbank")) return SHAHR
            if (combined.contains("پارسیان") || lowerSender.contains("parsian")) return PARSIAN
            if (combined.contains("سینا") || lowerSender.contains("sina") || lowerSender.contains("sinabank")) return SINA
            if (combined.contains("بانک دی") || lowerSender == "dey" || lowerSender == "day" || lowerSender.contains("daybank")) return DEY
            if ((combined.contains("سپه") && !combined.contains("سپهر")) || combined.contains("انصار") || combined.contains("قوامین") || combined.contains("حکمت") || combined.contains("کوثر") || combined.contains("مهر اقتصاد") || lowerSender.contains("sepah") || lowerSender.contains("banksepah")) return SEPAH

            // ۳. سایر بانک‌های پایه و مطرح اولیه
            if (combined.contains("پاسارگاد") || lowerSender.contains("pasargad") || combined.contains("bpi")) return PASARGAD
            if (combined.contains("سامان") || lowerSender.contains("saman") || combined.contains("sb24")) return SAMAN
            if (combined.contains("تجارت") || lowerSender.contains("tejarat")) return TEJARAT
            if (combined.contains("ملت") || lowerSender.contains("mellat")) return MELLAT
            if (combined.contains("ملی") || lowerSender.contains("melli") || combined.contains("bmi")) return MELLI

            return UNKNOWN
        }
    }
}
