package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class RecurrencePeriod(val title: String, val daysApprox: Int) {
    DAILY("روزانه", 1),
    WEEKLY("هفتگی", 7),
    MONTHLY("ماهانه", 30),
    YEARLY("سالانه", 365);

    companion object {
        fun fromString(value: String): RecurrencePeriod {
            return entries.find { it.name.equals(value, ignoreCase = true) || it.title == value } ?: MONTHLY
        }
    }
}

@Entity(tableName = "recurring_transactions")
data class RecurringTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Long,                          // مبلغ به تومان
    val isIncome: Boolean = false,             // هزینه (false) یا درآمد (true)
    val category: String,                      // خوراکی، مسکن، حقوق، ...
    val period: String = RecurrencePeriod.MONTHLY.name, // DAILY, WEEKLY, MONTHLY, YEARLY
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long? = null,                 // تاریخ پایان اختیاری
    val nextExecutionDate: Long,               // تاریخ سررسید بعدی به میلی‌ثانیه
    val lastExecutedDate: Long? = null,        // آخرین تاریخی که به طور خودکار ثبت شد
    val isActive: Boolean = true,              // فعال بودن یا توقف موقت
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    val recurrencePeriod: RecurrencePeriod
        get() = RecurrencePeriod.fromString(period)

    val isDueNow: Boolean
        get() = isActive && (System.currentTimeMillis() >= nextExecutionDate) && (endDate == null || nextExecutionDate <= endDate)

    val isDueSoon: Boolean
        get() {
            if (!isActive) return false
            val now = System.currentTimeMillis()
            val twoDaysMs = 2L * 24 * 60 * 60 * 1000
            return (nextExecutionDate > now) && ((nextExecutionDate - now) <= twoDaysMs)
        }
}
