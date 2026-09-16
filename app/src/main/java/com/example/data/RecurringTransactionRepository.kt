package com.example.data

import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class RecurringTransactionRepository(private val dao: RecurringTransactionDao) {

    val allRecurringTransactions: Flow<List<RecurringTransaction>> = dao.getAll()
    val activeRecurringTransactions: Flow<List<RecurringTransaction>> = dao.getActiveList()

    suspend fun insert(item: RecurringTransaction): Long = dao.insert(item)

    suspend fun update(item: RecurringTransaction) = dao.update(item)

    suspend fun delete(item: RecurringTransaction) = dao.delete(item)

    suspend fun setActiveState(id: Long, isActive: Boolean) = dao.setActiveState(id, isActive)

    suspend fun getDueTransactions(currentTime: Long = System.currentTimeMillis()): List<RecurringTransaction> =
        dao.getDueTransactions(currentTime)

    suspend fun getUpcomingDueTransactions(
        currentTime: Long = System.currentTimeMillis(),
        withinDays: Int = 2
    ): List<RecurringTransaction> {
        val threshold = currentTime + (withinDays.toLong() * 24 * 60 * 60 * 1000)
        return dao.getUpcomingDueTransactions(currentTime, threshold)
    }

    suspend fun updateExecutionDates(id: Long, nextDate: Long, lastExecuted: Long) =
        dao.updateExecutionDates(id, nextDate, lastExecuted)

    suspend fun updateCategoryName(oldName: String, newName: String) =
        dao.updateCategoryName(oldName, newName)

    companion object {
        /**
         * تاریخ سررسید بعدی را بر اساس دوره تکرار محاسبه می‌کند
         */
        fun calculateNextDate(currentNextDate: Long, period: RecurrencePeriod): Long {
            val cal = Calendar.getInstance()
            cal.timeInMillis = currentNextDate
            when (period) {
                RecurrencePeriod.DAILY -> cal.add(Calendar.DAY_OF_YEAR, 1)
                RecurrencePeriod.WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 1)
                RecurrencePeriod.MONTHLY -> cal.add(Calendar.MONTH, 1)
                RecurrencePeriod.YEARLY -> cal.add(Calendar.YEAR, 1)
            }
            return cal.timeInMillis
        }
    }
}
