package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Long,           // مبلغ به تومان
    val category: String,       // خوراکی, حمل‌ونقل, درمان, ...
    val description: String,    // توضیح خلاصه
    val date: Long = System.currentTimeMillis(),
    val isIncome: Boolean = false // هزینه (false) یا درآمد (true)
)
