package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "savings_goals")
data class SavingsGoal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,                // عنوان هدف (مثلا خرید لپ‌تاپ)
    val targetAmount: Long,           // مبلغ هدف به تومان
    val savedAmount: Long = 0L,       // مبلغ پس‌انداز شده تا الان
    val targetDate: Long? = null,     // تاریخ هدف (اختیاری)
    val note: String = "",            // توضیحات (اختیاری)
    val iconName: String = "Star",    // نام آیکون انتخابی
    val colorHex: String = "#0D5C46", // رنگ انتخابی (Hex)
    val createdAt: Long = System.currentTimeMillis()
) {
    val progressFraction: Float
        get() = if (targetAmount > 0) (savedAmount.toFloat() / targetAmount).coerceIn(0f, 1f) else 0f

    val progressPercent: Int
        get() = (progressFraction * 100).toInt()

    val isCompleted: Boolean
        get() = savedAmount >= targetAmount && targetAmount > 0

    val remainingAmount: Long
        get() = (targetAmount - savedAmount).coerceAtLeast(0L)
}
