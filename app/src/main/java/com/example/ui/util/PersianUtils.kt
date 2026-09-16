package com.example.ui.util

import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

object PersianUtils {

    fun toPersianDigits(input: String): String {
        val englishDigits = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
        val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        var result = input
        for (i in englishDigits.indices) {
            result = result.replace(englishDigits[i], persianDigits[i])
        }
        return result
    }

    fun formatNumber(number: Long): String {
        val formatter = NumberFormat.getNumberInstance(Locale.US)
        return toPersianDigits(formatter.format(number))
    }

    fun formatCurrencyToman(amount: Long): String {
        return "${formatNumber(amount)} تومان"
    }

    fun formatCurrencyRial(amount: Long): String {
        return "${formatNumber(amount * 10)} ریال"
    }

    fun getShamsiDateParts(timestamp: Long = System.currentTimeMillis()): IntArray {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        val gYear = calendar.get(Calendar.YEAR)
        val gMonth = calendar.get(Calendar.MONTH) + 1
        val gDay = calendar.get(Calendar.DAY_OF_MONTH)

        val gDaysInMonth = intArrayOf(0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        val jDaysInMonth = intArrayOf(0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)

        val isGLeap = (gYear % 4 == 0 && gYear % 100 != 0) || (gYear % 400 == 0)
        if (isGLeap) gDaysInMonth[2] = 29

        val gy = gYear - 1600
        val gm = gMonth - 1
        val gd = gDay - 1

        var gDayNo = 365 * gy + (gy + 3) / 4 - (gy + 99) / 100 + (gy + 399) / 400
        for (i in 0 until gm) {
            gDayNo += gDaysInMonth[i + 1]
        }
        gDayNo += gd

        var jDayNo = gDayNo - 79

        val jNp = jDayNo / 12053
        jDayNo %= 12053

        var jy = 979 + 33 * jNp + 4 * (jDayNo / 1461)
        jDayNo %= 1461

        if (jDayNo >= 366) {
            jy += (jDayNo - 1) / 365
            jDayNo = (jDayNo - 1) % 365
        }

        var jm = 0
        var jd = 0
        for (i in 0 until 11) {
            if (jDayNo < jDaysInMonth[i + 1]) {
                jm = i + 1
                jd = jDayNo + 1
                break
            }
            jDayNo -= jDaysInMonth[i + 1]
        }
        if (jm == 0) {
            jm = 12
            jd = jDayNo + 1
        }

        return intArrayOf(jy, jm, jd)
    }

    fun getShamsiDateString(timestamp: Long = System.currentTimeMillis()): String {
        val (y, m, d) = getShamsiDateParts(timestamp)
        return "${toPersianDigits(y.toString())}/${toPersianDigits(m.toString().padStart(2, '0'))}/${toPersianDigits(d.toString().padStart(2, '0'))}"
    }

    fun getShamsiDateTimeForFilename(timestamp: Long = System.currentTimeMillis()): String {
        val (y, m, d) = getShamsiDateParts(timestamp)
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        return String.format(Locale.US, "%04d-%02d-%02d_%02d-%02d", y, m, d, hour, minute)
    }

    fun formatPersianDate(timestamp: Long): String {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply { timeInMillis = timestamp }

        val diffMillis = now.timeInMillis - target.timeInMillis
        val diffDays = (diffMillis / (1000 * 60 * 60 * 24)).toInt()

        val timeString = toPersianDigits(
            String.format(
                Locale.US,
                "%02d:%02d",
                target.get(Calendar.HOUR_OF_DAY),
                target.get(Calendar.MINUTE)
            )
        )

        val dayString = when (diffDays) {
            0 -> "امروز"
            1 -> "دیروز"
            else -> {
                val year = target.get(Calendar.YEAR)
                val month = target.get(Calendar.MONTH) + 1
                val day = target.get(Calendar.DAY_OF_MONTH)
                "${toPersianDigits("$day")}/${toPersianDigits("$month")}/${toPersianDigits("$year")}"
            }
        }

        return "$dayString - ساعت $timeString"
    }
}
