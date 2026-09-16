package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringTransactionDao {

    @Query("SELECT * FROM recurring_transactions ORDER BY isActive DESC, nextExecutionDate ASC")
    fun getAll(): Flow<List<RecurringTransaction>>

    @Query("SELECT * FROM recurring_transactions WHERE isActive = 1 ORDER BY nextExecutionDate ASC")
    fun getActiveList(): Flow<List<RecurringTransaction>>

    @Query("SELECT * FROM recurring_transactions WHERE isActive = 1 AND nextExecutionDate <= :currentTime")
    suspend fun getDueTransactions(currentTime: Long): List<RecurringTransaction>

    @Query("SELECT * FROM recurring_transactions WHERE isActive = 1 AND nextExecutionDate > :currentTime AND nextExecutionDate <= :thresholdTime")
    suspend fun getUpcomingDueTransactions(currentTime: Long, thresholdTime: Long): List<RecurringTransaction>

    @Query("SELECT * FROM recurring_transactions WHERE id = :id")
    suspend fun getById(id: Long): RecurringTransaction?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recurringTransaction: RecurringTransaction): Long

    @Update
    suspend fun update(recurringTransaction: RecurringTransaction)

    @Delete
    suspend fun delete(recurringTransaction: RecurringTransaction)

    @Query("UPDATE recurring_transactions SET isActive = :isActive WHERE id = :id")
    suspend fun setActiveState(id: Long, isActive: Boolean)

    @Query("UPDATE recurring_transactions SET nextExecutionDate = :nextDate, lastExecutedDate = :lastExecuted WHERE id = :id")
    suspend fun updateExecutionDates(id: Long, nextDate: Long, lastExecuted: Long)

    @Query("UPDATE recurring_transactions SET category = :newName WHERE category = :oldName")
    suspend fun updateCategoryName(oldName: String, newName: String)

    @Query("SELECT * FROM recurring_transactions ORDER BY id ASC")
    suspend fun getAllList(): List<RecurringTransaction>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(recurringTransactions: List<RecurringTransaction>)
}
