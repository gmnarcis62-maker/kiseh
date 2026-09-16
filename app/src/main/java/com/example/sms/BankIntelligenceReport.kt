package com.example.sms

import android.content.Context
import com.example.billing.BillingManager
import com.example.data.PendingBankSms
import com.example.data.Transaction

/**
 * مدل داده‌ای گزارش تحلیلی هوش پیامکی بانکی
 */
data class BankIntelligenceReportData(
    val totalProcessedSms: Int,
    val totalExpenseFromSms: Long,
    val totalIncomeFromSms: Long,
    val topMerchants: List<Pair<String, Long>>,
    val topCategories: List<Pair<String, Long>>,
    val autoConfirmedCount: Int
)

/**
 * موتور گزارش‌گیری هوش پیامکی حرفه‌ای (مخصوص کاربران VIP)
 * در نسخه رایگان، دسترسی به این موتور مسدود بوده و داده‌ای تولید نمی‌کند.
 */
object BankIntelligenceReport {

    fun generateReport(
        context: Context,
        transactions: List<Transaction>,
        pendingSmsList: List<PendingBankSms> = emptyList()
    ): BankIntelligenceReportData? {
        // بررسی و اعمال قانون VIP: نسخه Free نباید گزارش هوشمند پیامک بانکی را اجرا یا تولید کند
        if (!BillingManager.isProUser(context)) {
            return null
        }

        val bankTransactions = transactions.filter {
            it.description.contains("کارت") ||
            it.description.contains("بانک") ||
            BankType.values().any { b -> it.description.contains(b.displayName) }
        }

        val totalExpense = bankTransactions.filter { !it.isIncome }.sumOf { it.amount }
        val totalIncome = bankTransactions.filter { it.isIncome }.sumOf { it.amount }

        val topCategories = bankTransactions.groupBy { it.category }
            .mapValues { it.value.sumOf { t -> t.amount } }
            .toList()
            .sortedByDescending { it.second }
            .take(5)

        val topMerchants = bankTransactions
            .filter { it.description.contains("-") }
            .groupBy { it.description.split("-").getOrNull(1)?.trim() ?: "" }
            .filterKeys { it.isNotBlank() }
            .mapValues { it.value.sumOf { t -> t.amount } }
            .toList()
            .sortedByDescending { it.second }
            .take(5)

        return BankIntelligenceReportData(
            totalProcessedSms = bankTransactions.size + pendingSmsList.size,
            totalExpenseFromSms = totalExpense,
            totalIncomeFromSms = totalIncome,
            topMerchants = topMerchants,
            topCategories = topCategories,
            autoConfirmedCount = bankTransactions.size
        )
    }

    fun isAccessible(context: Context): Boolean {
        return BillingManager.isProUser(context)
    }
}
